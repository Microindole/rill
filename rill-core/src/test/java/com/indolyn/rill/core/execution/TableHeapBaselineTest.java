package com.indolyn.rill.core.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.indolyn.rill.core.catalog.TableInfo;
import com.indolyn.rill.core.execution.operator.TableHeap;
import com.indolyn.rill.core.model.Column;
import com.indolyn.rill.core.model.DataType;
import com.indolyn.rill.core.model.RID;
import com.indolyn.rill.core.model.Schema;
import com.indolyn.rill.core.model.Tuple;
import com.indolyn.rill.core.model.Value;
import com.indolyn.rill.core.storage.buffer.BufferPoolManager;
import com.indolyn.rill.core.storage.disk.DiskManager;
import com.indolyn.rill.core.storage.page.PageId;
import com.indolyn.rill.core.transaction.LockService;
import com.indolyn.rill.core.transaction.Transaction;
import com.indolyn.rill.core.transaction.log.LogManager;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TableHeapBaselineTest {

    @TempDir
    Path tempDir;

    @Test
    void tableHeapShouldInsertReadUpdateDeleteAndIterateAcrossPages() throws Exception {
        try (TestTableHeapContext context = TestTableHeapContext.open(tempDir, wideUserSchema())) {
            TableHeap tableHeap = context.tableHeap();
            Transaction insertTxn = new Transaction();

            Tuple first = wideTuple(1, "alice");
            Tuple second = wideTuple(2, "bob");
            Tuple third = wideTuple(3, "carol");

            assertTrue(tableHeap.insertTuple(first, insertTxn));
            assertTrue(tableHeap.insertTuple(second, insertTxn));
            assertTrue(tableHeap.insertTuple(third, insertTxn));
            assertNotNull(tableHeap.getFirstPageId());
            assertTrue(tableHeap.getFirstPageId().getPageNum() >= 0);

            Transaction readTxn = new Transaction();
            Tuple inserted = tableHeap.getTuple(first.getRid(), readTxn);
            assertNotNull(inserted);
            assertTrue(((String) inserted.getValues().get(1).getValue()).startsWith("alice-"));

            Transaction updateTxn = new Transaction();
            Tuple updatedTuple = wideTuple(1, "alice-updated");
            RID updatedRid = tableHeap.updateTuple(updatedTuple, first.getRid(), updateTxn);
            assertNotNull(updatedRid);

            Transaction verifyTxn = new Transaction();
            Tuple updatedReadBack = tableHeap.getTuple(updatedRid, verifyTxn);
            assertNotNull(updatedReadBack);
            assertTrue(((String) updatedReadBack.getValues().get(1).getValue()).startsWith("alice-updated-"));

            Transaction deleteTxn = new Transaction();
            assertTrue(tableHeap.deleteTuple(second.getRid(), deleteTxn));

            Transaction iteratorTxn = new Transaction();
            tableHeap.initIterator(iteratorTxn);
            List<String> names = new ArrayList<>();
            while (tableHeap.hasNext()) {
                names.add((String) tableHeap.next().getValues().get(1).getValue());
            }

            assertEquals(2, names.size());
            assertTrue(names.stream().anyMatch(name -> name.startsWith("carol-")));
            assertTrue(names.stream().anyMatch(name -> name.startsWith("alice-updated-")));
            assertNull(tableHeap.getTuple(second.getRid(), new Transaction()));
        }
    }

    @Test
    void tableHeapShouldRejectStringsThatExceedDeclaredLength() throws Exception {
        Schema constrainedSchema =
            new Schema(
                List.of(
                    new Column("id", DataType.INT),
                    new Column("name", DataType.VARCHAR, "VARCHAR", List.of(5))));

        try (TestTableHeapContext context = TestTableHeapContext.open(tempDir, constrainedSchema)) {
            TableHeap tableHeap = context.tableHeap();
            Transaction txn = new Transaction();

            IllegalArgumentException exception =
                assertThrows(
                    IllegalArgumentException.class,
                    () -> tableHeap.insertTuple(tuple(1, "toolong-name"), txn));

            assertTrue(exception.getMessage().contains("length limit"));
        }
    }

    private static Tuple tuple(int id, String name) {
        return new Tuple(List.of(new Value(id), new Value(name)));
    }

    private static Tuple wideTuple(int id, String name) {
        return new Tuple(List.of(new Value(id), new Value(name + "-" + "x".repeat(1500))));
    }

    private static Schema wideUserSchema() {
        return new Schema(List.of(new Column("id", DataType.INT), new Column("name", DataType.VARCHAR)));
    }
    private record TestTableHeapContext(
        DiskManager diskManager,
        LogManager logManager,
        BufferPoolManager bufferPoolManager,
        TableHeap tableHeap)
        implements AutoCloseable {

        private static TestTableHeapContext open(Path tempDir, Schema schema) throws Exception {
            DiskManager diskManager = new DiskManager(tempDir.resolve("rill.data").toString());
            diskManager.open();
            LogManager logManager = new LogManager(tempDir.resolve("rill.data.log").toString());
            BufferPoolManager bufferPoolManager = new BufferPoolManager(8, diskManager, "LRU");
            TableInfo tableInfo = new TableInfo("users", schema, new PageId(-1));
            TableHeap tableHeap =
                new TableHeap(bufferPoolManager, tableInfo, logManager, new NoOpLockService());
            return new TestTableHeapContext(diskManager, logManager, bufferPoolManager, tableHeap);
        }

        @Override
        public void close() throws Exception {
            bufferPoolManager.flushAllPages();
            logManager.close();
            diskManager.close();
        }
    }

    private static final class NoOpLockService implements LockService {
        @Override
        public void lockShared(Transaction txn, PageId pageId) {}

        @Override
        public void lockExclusive(Transaction txn, PageId pageId) {}

        @Override
        public void unlock(Transaction txn, PageId pageId) {}
    }
}

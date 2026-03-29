package com.indolyn.rill.core.execution;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.indolyn.rill.core.catalog.TableInfo;
import com.indolyn.rill.core.execution.operator.TableHeap;
import com.indolyn.rill.core.model.Column;
import com.indolyn.rill.core.model.DataType;
import com.indolyn.rill.core.model.Schema;
import com.indolyn.rill.core.model.Tuple;
import com.indolyn.rill.core.model.Value;
import com.indolyn.rill.core.transaction.Transaction;

import java.util.List;

import org.junit.jupiter.api.Test;

class RecoveryMultiPageTest {

    @Test
    void recoveryShouldUndoUncommittedWideTupleAcrossPages() throws Exception {
        String dbName = "core_recovery_multi_page_" + System.nanoTime();
        QueryRuntime runtime = new QueryRuntime(dbName);
        try {
            TableHeap tableHeap = createWideUsersTable(runtime);

            Transaction committedTxn = runtime.getTransactionManager().begin();
            tableHeap.insertTuple(wideTuple(1, "persisted"), committedTxn);
            tableHeap.insertTuple(wideTuple(2, "persisted-second"), committedTxn);
            runtime.getTransactionManager().commit(committedTxn);

            Transaction uncommittedTxn = runtime.getTransactionManager().begin();
            tableHeap.insertTuple(wideTuple(3, "transient"), uncommittedTxn);

            crashRuntime(runtime);
            runtime = null;

            QueryProcessor recoveredProcessor = new QueryProcessor(dbName);
            try {
                String result = recoveredProcessor.executeAndGetResult("SELECT * FROM users;");
                assertTrue(result.contains("persisted-"));
                assertTrue(result.contains("persisted-second-"));
                assertFalse(result.contains("transient-"));
            } finally {
                recoveredProcessor.close();
            }
        } finally {
            closeRuntime(runtime);
        }
    }

    private TableHeap createWideUsersTable(QueryRuntime runtime) throws Exception {
        TableInfo tableInfo =
            runtime.getCatalog()
                .createTable(
                    "users",
                    new Schema(List.of(new Column("id", DataType.INT), new Column("name", DataType.VARCHAR))));
        return new TableHeap(
            runtime.getBufferPoolManager(), tableInfo, runtime.getLogManager(), runtime.getLockManager());
    }

    private Tuple wideTuple(int id, String prefix) {
        return new Tuple(List.of(new Value(id), new Value(prefix + "-" + "x".repeat(1500))));
    }

    private void crashRuntime(QueryRuntime runtime) throws Exception {
        runtime.getBufferPoolManager().flushAllPages();
        runtime.getLogManager().flush();
        runtime.getDiskManager().close();
        runtime.getLogManager().close();
    }

    private void closeRuntime(QueryRuntime runtime) throws Exception {
        if (runtime == null) {
            return;
        }
        runtime.getBufferPoolManager().flushAllPages();
        runtime.getLogManager().flush();
        runtime.getDiskManager().close();
        runtime.getLogManager().close();
    }
}

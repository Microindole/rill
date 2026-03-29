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

class RecoveryMutationUndoTest {

    @Test
    void recoveryShouldUndoUncommittedUpdateAndRestorePreviousValue() throws Exception {
        String dbName = "core_recovery_uncommitted_update_" + System.nanoTime();
        QueryRuntime runtime = new QueryRuntime(dbName);
        try {
            TableHeap tableHeap = createUsersTable(runtime);

            Transaction seedTxn = runtime.getTransactionManager().begin();
            Tuple originalTuple = new Tuple(List.of(new Value(1), new Value("alice")));
            tableHeap.insertTuple(originalTuple, seedTxn);
            runtime.getTransactionManager().commit(seedTxn);

            Transaction updateTxn = runtime.getTransactionManager().begin();
            tableHeap.updateTuple(
                new Tuple(List.of(new Value(1), new Value("bob"))),
                originalTuple.getRid(),
                updateTxn);

            crashRuntime(runtime);
            runtime = null;

            QueryProcessor recoveredProcessor = new QueryProcessor(dbName);
            try {
                String result = recoveredProcessor.executeAndGetResult("SELECT * FROM users;");
                assertTrue(result.contains("alice"));
                assertFalse(result.contains("bob"));
            } finally {
                recoveredProcessor.close();
            }
        } finally {
            closeRuntime(runtime);
        }
    }

    @Test
    void recoveryShouldUndoUncommittedDeleteAndKeepCommittedRowVisible() throws Exception {
        String dbName = "core_recovery_uncommitted_delete_" + System.nanoTime();
        QueryRuntime runtime = new QueryRuntime(dbName);
        try {
            TableHeap tableHeap = createUsersTable(runtime);

            Transaction seedTxn = runtime.getTransactionManager().begin();
            Tuple originalTuple = new Tuple(List.of(new Value(1), new Value("alice")));
            tableHeap.insertTuple(originalTuple, seedTxn);
            runtime.getTransactionManager().commit(seedTxn);

            Transaction deleteTxn = runtime.getTransactionManager().begin();
            tableHeap.deleteTuple(originalTuple.getRid(), deleteTxn);

            crashRuntime(runtime);
            runtime = null;

            QueryProcessor recoveredProcessor = new QueryProcessor(dbName);
            try {
                String result = recoveredProcessor.executeAndGetResult("SELECT * FROM users;");
                assertTrue(result.contains("alice"));
                assertTrue(result.contains("1 rows returned"));
            } finally {
                recoveredProcessor.close();
            }
        } finally {
            closeRuntime(runtime);
        }
    }

    private TableHeap createUsersTable(QueryRuntime runtime) throws Exception {
        TableInfo tableInfo =
            runtime.getCatalog()
                .createTable(
                    "users",
                    new Schema(List.of(new Column("id", DataType.INT), new Column("name", DataType.VARCHAR))));
        return new TableHeap(
            runtime.getBufferPoolManager(), tableInfo, runtime.getLogManager(), runtime.getLockManager());
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

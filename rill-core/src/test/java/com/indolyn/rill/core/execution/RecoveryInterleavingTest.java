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

class RecoveryInterleavingTest {

    @Test
    void recoveryShouldUndoUncommittedInsertWhileKeepingCommittedRow() throws Exception {
        String dbName = "core_recovery_uncommitted_insert_" + System.nanoTime();
        QueryRuntime runtime = new QueryRuntime(dbName);
        try {
            TableHeap tableHeap = createUsersTable(runtime);

            Transaction committedTxn = runtime.getTransactionManager().begin();
            tableHeap.insertTuple(
                new Tuple(List.of(new Value(1), new Value("alice"))), committedTxn);
            runtime.getTransactionManager().commit(committedTxn);

            Transaction uncommittedTxn = runtime.getTransactionManager().begin();
            tableHeap.insertTuple(
                new Tuple(List.of(new Value(2), new Value("bob"))), uncommittedTxn);

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
    void recoveryShouldPreserveCommittedRowsAcrossInterleavedTransactions() throws Exception {
        String dbName = "core_recovery_interleaved_transactions_" + System.nanoTime();
        QueryRuntime runtime = new QueryRuntime(dbName);
        try {
            TableHeap tableHeap = createUsersTable(runtime);
            TableHeap ordersTableHeap = createOrdersTable(runtime);

            Transaction firstCommittedTxn = runtime.getTransactionManager().begin();
            tableHeap.insertTuple(
                new Tuple(List.of(new Value(1), new Value("alice"))), firstCommittedTxn);
            runtime.getTransactionManager().commit(firstCommittedTxn);

            Transaction uncommittedTxn = runtime.getTransactionManager().begin();
            tableHeap.insertTuple(
                new Tuple(List.of(new Value(2), new Value("bob"))), uncommittedTxn);

            Transaction secondCommittedTxn = runtime.getTransactionManager().begin();
            ordersTableHeap.insertTuple(
                new Tuple(List.of(new Value(3), new Value("carol"))), secondCommittedTxn);
            runtime.getTransactionManager().commit(secondCommittedTxn);

            crashRuntime(runtime);
            runtime = null;

            QueryProcessor recoveredProcessor = new QueryProcessor(dbName);
            try {
                String usersResult = recoveredProcessor.executeAndGetResult("SELECT * FROM users;");
                String ordersResult = recoveredProcessor.executeAndGetResult("SELECT * FROM orders;");
                assertTrue(usersResult.contains("alice"));
                assertFalse(usersResult.contains("bob"));
                assertTrue(ordersResult.contains("carol"));
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

    private TableHeap createOrdersTable(QueryRuntime runtime) throws Exception {
        TableInfo tableInfo =
            runtime.getCatalog()
                .createTable(
                    "orders",
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

package com.indolyn.rill.core.execution;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.indolyn.rill.core.catalog.Catalog;
import com.indolyn.rill.core.catalog.TableInfo;
import com.indolyn.rill.core.execution.operator.command.CreateDatabaseExecutor;
import com.indolyn.rill.core.execution.operator.query.SeqScanExecutor;
import com.indolyn.rill.core.model.Column;
import com.indolyn.rill.core.model.DataType;
import com.indolyn.rill.core.model.Schema;
import com.indolyn.rill.core.sql.planner.plan.PlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.CreateDatabasePlanNode;
import com.indolyn.rill.core.sql.planner.plan.query.SeqScanPlanNode;
import com.indolyn.rill.core.storage.buffer.BufferPoolManager;
import com.indolyn.rill.core.storage.database.DatabaseManager;
import com.indolyn.rill.core.storage.disk.DiskManager;
import com.indolyn.rill.core.transaction.LockManager;
import com.indolyn.rill.core.transaction.Transaction;
import com.indolyn.rill.core.transaction.TransactionManager;
import com.indolyn.rill.core.transaction.log.LogManager;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExecutionEngineTest {

    private static final String TEST_DB_FILE = "execution_engine_test.db";
    private static final String TEST_LOG_FILE = "execution_engine_test.db.log";

    private DiskManager diskManager;
    private LogManager logManager;
    private TransactionManager transactionManager;
    private ExecutionEngine executionEngine;
    private TableInfo usersTable;

    @BeforeEach
    void setUp() throws IOException {
        new File(TEST_DB_FILE).delete();
        new File(TEST_LOG_FILE).delete();

        diskManager = new DiskManager(TEST_DB_FILE);
        diskManager.open();
        BufferPoolManager bufferPoolManager = new BufferPoolManager(10, diskManager, "LRU");
        logManager = new LogManager(TEST_LOG_FILE);
        LockManager lockManager = new LockManager();
        transactionManager = new TransactionManager(lockManager, logManager);
        Catalog catalog = new Catalog(bufferPoolManager);
        usersTable =
            catalog.createTable(
                "users",
                new Schema(List.of(new Column("id", DataType.INT), new Column("name", DataType.VARCHAR))));

        executionEngine =
            new ExecutionEngine(
                bufferPoolManager, catalog, logManager, lockManager, new DatabaseManager());
    }

    @AfterEach
    void tearDown() throws IOException {
        if (logManager != null) {
            logManager.close();
        }
        if (diskManager != null) {
            diskManager.close();
        }
        new File(TEST_DB_FILE).delete();
        new File(TEST_LOG_FILE).delete();
        deleteDirectory(new File("data/execution_engine_created_db"));
    }

    @Test
    void shouldCreateCommandExecutorForCreateDatabasePlan() throws Exception {
        CreateDatabasePlanNode plan = new CreateDatabasePlanNode("execution_engine_created_db");

        assertInstanceOf(CreateDatabaseExecutor.class, executionEngine.execute(plan, null));
    }

    @Test
    void shouldCreateQueryExecutorForSeqScanPlan() throws Exception {
        SeqScanPlanNode plan = new SeqScanPlanNode(usersTable, null);
        Transaction txn = transactionManager.begin();

        try {
            assertInstanceOf(SeqScanExecutor.class, executionEngine.execute(plan, txn));
        } finally {
            transactionManager.commit(txn);
        }
    }

    @Test
    void unsupportedPlanShouldThrow() {
        PlanNode unsupportedPlan = new PlanNode(new Schema(List.of(new Column("id", DataType.INT)))) {
        };

        assertThrows(
            UnsupportedOperationException.class, () -> executionEngine.execute(unsupportedPlan, null));
    }

    private void deleteDirectory(File directory) {
        if (!directory.exists()) {
            return;
        }
        File[] allContents = directory.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        directory.delete();
    }
}

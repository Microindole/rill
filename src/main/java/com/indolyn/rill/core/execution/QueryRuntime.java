package com.indolyn.rill.core.execution;

import com.indolyn.rill.core.catalog.Catalog;
import com.indolyn.rill.core.storage.buffer.BufferPoolManager;
import com.indolyn.rill.core.storage.database.DatabaseManager;
import com.indolyn.rill.core.storage.disk.DiskManager;
import com.indolyn.rill.core.transaction.LockManager;
import com.indolyn.rill.core.transaction.RecoveryManager;
import com.indolyn.rill.core.transaction.TransactionManager;
import com.indolyn.rill.core.transaction.log.LogManager;
import com.indolyn.rill.core.sql.planner.Planner;

import java.io.IOException;

final class QueryRuntime {
    private static final int DEFAULT_BUFFER_POOL_SIZE = 100;
    private static final String DEFAULT_REPLACEMENT_POLICY = "MLFQ";

    private final DatabaseManager dbManager;
    private final DiskManager diskManager;
    private final BufferPoolManager bufferPoolManager;
    private final Catalog catalog;
    private final Planner planner;
    private final LogManager logManager;
    private final LockManager lockManager;
    private final TransactionManager transactionManager;
    private final ExecutionEngine executionEngine;

    QueryRuntime(String dbName) throws IOException {
        this.dbManager = new DatabaseManager();
        this.diskManager = new DiskManager(DatabaseManager.getDbFilePath(dbName));
        diskManager.open();
        this.bufferPoolManager =
            new BufferPoolManager(DEFAULT_BUFFER_POOL_SIZE, diskManager, DEFAULT_REPLACEMENT_POLICY);
        this.catalog = new Catalog(bufferPoolManager);
        this.planner = new Planner(catalog);
        this.logManager = new LogManager(DatabaseManager.getDbFilePath(dbName) + ".log");
        this.lockManager = new LockManager();
        this.transactionManager = new TransactionManager(lockManager, logManager);
        this.executionEngine =
            new ExecutionEngine(bufferPoolManager, catalog, logManager, lockManager, dbManager);
        runRecovery();
    }

    private void runRecovery() throws IOException {
        RecoveryManager recoveryManager =
            new RecoveryManager(logManager, bufferPoolManager, catalog, lockManager);
        recoveryManager.recover();
    }

    DatabaseManager getDbManager() {
        return dbManager;
    }

    DiskManager getDiskManager() {
        return diskManager;
    }

    BufferPoolManager getBufferPoolManager() {
        return bufferPoolManager;
    }

    Catalog getCatalog() {
        return catalog;
    }

    Planner getPlanner() {
        return planner;
    }

    LogManager getLogManager() {
        return logManager;
    }

    LockManager getLockManager() {
        return lockManager;
    }

    TransactionManager getTransactionManager() {
        return transactionManager;
    }

    ExecutionEngine getExecutionEngine() {
        return executionEngine;
    }
}

package com.indolyn.rill.core.execution;

import com.indolyn.rill.core.catalog.Catalog;
import com.indolyn.rill.core.storage.buffer.BufferPoolManager;
import com.indolyn.rill.core.storage.database.DatabaseManager;
import com.indolyn.rill.core.storage.disk.DiskManager;
import com.indolyn.rill.core.transaction.LockService;
import com.indolyn.rill.core.transaction.RecoveryManager;
import com.indolyn.rill.core.transaction.TransactionManager;
import com.indolyn.rill.core.transaction.log.LogService;
import com.indolyn.rill.core.sql.planner.Planner;

import java.io.IOException;

final class QueryRuntime {
    private final DatabaseManager dbManager;
    private final DiskManager diskManager;
    private final BufferPoolManager bufferPoolManager;
    private final Catalog catalog;
    private final Planner planner;
    private final LogService logManager;
    private final LockService lockManager;
    private final TransactionManager transactionManager;
    private final ExecutionEngine executionEngine;

    QueryRuntime(String dbName) throws IOException {
        this(dbName, new DefaultRuntimeInfrastructureFactory());
    }

    QueryRuntime(String dbName, RuntimeInfrastructureFactory infrastructureFactory) throws IOException {
        RuntimeInfrastructure infrastructure = infrastructureFactory.create(dbName);
        this.dbManager = infrastructure.getDatabaseManager();
        this.diskManager = infrastructure.getDiskManager();
        this.bufferPoolManager = infrastructure.getBufferPoolManager();
        this.catalog = infrastructure.getCatalog();
        this.planner = new Planner(catalog);
        this.logManager = infrastructure.getLogManager();
        this.lockManager = infrastructure.getLockManager();
        this.transactionManager = infrastructure.getTransactionManager();
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

    LogService getLogManager() {
        return logManager;
    }

    LockService getLockManager() {
        return lockManager;
    }

    TransactionManager getTransactionManager() {
        return transactionManager;
    }

    ExecutionEngine getExecutionEngine() {
        return executionEngine;
    }
}

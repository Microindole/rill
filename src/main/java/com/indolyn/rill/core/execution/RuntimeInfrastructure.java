package com.indolyn.rill.core.execution;

import com.indolyn.rill.core.catalog.Catalog;
import com.indolyn.rill.core.storage.buffer.BufferPoolManager;
import com.indolyn.rill.core.storage.database.DatabaseManager;
import com.indolyn.rill.core.storage.disk.DiskManager;
import com.indolyn.rill.core.transaction.LockService;
import com.indolyn.rill.core.transaction.TransactionManager;
import com.indolyn.rill.core.transaction.log.LogService;

final class RuntimeInfrastructure {
    private final DatabaseManager databaseManager;
    private final DiskManager diskManager;
    private final BufferPoolManager bufferPoolManager;
    private final Catalog catalog;
    private final LogService logManager;
    private final LockService lockManager;
    private final TransactionManager transactionManager;

    RuntimeInfrastructure(
        DatabaseManager databaseManager,
        DiskManager diskManager,
        BufferPoolManager bufferPoolManager,
        Catalog catalog,
        LogService logManager,
        LockService lockManager,
        TransactionManager transactionManager) {
        this.databaseManager = databaseManager;
        this.diskManager = diskManager;
        this.bufferPoolManager = bufferPoolManager;
        this.catalog = catalog;
        this.logManager = logManager;
        this.lockManager = lockManager;
        this.transactionManager = transactionManager;
    }

    DatabaseManager getDatabaseManager() {
        return databaseManager;
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

    LogService getLogManager() {
        return logManager;
    }

    LockService getLockManager() {
        return lockManager;
    }

    TransactionManager getTransactionManager() {
        return transactionManager;
    }
}

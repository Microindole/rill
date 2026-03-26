package com.indolyn.rill.core.execution;

import com.indolyn.rill.core.catalog.Catalog;
import com.indolyn.rill.core.storage.buffer.BufferPoolManager;
import com.indolyn.rill.core.storage.database.DatabasePathResolver;
import com.indolyn.rill.core.storage.database.DatabaseManager;
import com.indolyn.rill.core.storage.database.LocalDatabasePathResolver;
import com.indolyn.rill.core.storage.disk.DiskManager;
import com.indolyn.rill.core.transaction.LockManager;
import com.indolyn.rill.core.transaction.TransactionManager;
import com.indolyn.rill.core.transaction.log.LogManager;

import java.io.IOException;

final class DefaultRuntimeInfrastructureFactory implements RuntimeInfrastructureFactory {
    private static final int DEFAULT_BUFFER_POOL_SIZE = 100;
    private static final String DEFAULT_REPLACEMENT_POLICY = "MLFQ";
    private final DatabasePathResolver pathResolver;

    DefaultRuntimeInfrastructureFactory() {
        this(new LocalDatabasePathResolver());
    }

    DefaultRuntimeInfrastructureFactory(DatabasePathResolver pathResolver) {
        this.pathResolver = pathResolver;
    }

    @Override
    public RuntimeInfrastructure create(String dbName) throws IOException {
        DatabaseManager databaseManager = new DatabaseManager(pathResolver);
        DiskManager diskManager = new DiskManager(pathResolver.resolveDatabaseFilePath(dbName));
        diskManager.open();

        BufferPoolManager bufferPoolManager =
            new BufferPoolManager(
                DEFAULT_BUFFER_POOL_SIZE, diskManager, DEFAULT_REPLACEMENT_POLICY);
        Catalog catalog = new Catalog(bufferPoolManager, pathResolver);
        LogManager logManager = new LogManager(pathResolver.resolveLogFilePath(dbName));
        LockManager lockManager = new LockManager();
        TransactionManager transactionManager = new TransactionManager(lockManager, logManager);

        return new RuntimeInfrastructure(
            databaseManager,
            diskManager,
            bufferPoolManager,
            catalog,
            logManager,
            lockManager,
            transactionManager);
    }
}

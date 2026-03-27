package com.indolyn.rill.core.storage.database;

public interface DatabasePathResolver {
    String resolveDatabaseRootPath();

    String resolveDatabaseDirectory(String dbName);

    String resolveDatabaseFilePath(String dbName);

    String resolveLogFilePath(String dbName);
}

package com.indolyn.rill.core.storage.database;

import java.io.File;

public class LocalDatabasePathResolver implements DatabasePathResolver {
    private static final String DB_ROOT_DIR = "data";
    private static final String DB_FILE_NAME = "rill.data";

    @Override
    public String resolveDatabaseRootPath() {
        return DB_ROOT_DIR;
    }

    @Override
    public String resolveDatabaseDirectory(String dbName) {
        return DB_ROOT_DIR + File.separator + dbName;
    }

    @Override
    public String resolveDatabaseFilePath(String dbName) {
        return resolveDatabaseDirectory(dbName) + File.separator + DB_FILE_NAME;
    }

    @Override
    public String resolveLogFilePath(String dbName) {
        return resolveDatabaseFilePath(dbName) + ".log";
    }
}

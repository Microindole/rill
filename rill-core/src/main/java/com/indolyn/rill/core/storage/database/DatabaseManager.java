package com.indolyn.rill.core.storage.database;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DatabaseManager {
    private static final DatabasePathResolver DEFAULT_PATH_RESOLVER =
        new LocalDatabasePathResolver();
    private final DatabasePathResolver pathResolver;

    public DatabaseManager() {
        this(DEFAULT_PATH_RESOLVER);
    }

    public DatabaseManager(DatabasePathResolver pathResolver) {
        this.pathResolver = pathResolver;
        File rootDir = new File(pathResolver.resolveDatabaseRootPath());
        if (!rootDir.exists()) {
            rootDir.mkdirs();
        }
    }

    public void createDatabase(String dbName) {
        File dbDir = new File(pathResolver.resolveDatabaseDirectory(dbName));
        if (dbDir.exists()) {
            throw new RuntimeException("Database '" + dbName + "' already exists.");
        }
        dbDir.mkdirs();
    }

    public List<String> listDatabases() {
        File rootDir = new File(pathResolver.resolveDatabaseRootPath());
        File[] directories = rootDir.listFiles(File::isDirectory);
        if (directories == null) {
            return List.of();
        }
        return Arrays.stream(directories).map(File::getName).collect(Collectors.toList());
    }

    public static String getDbFilePath(String dbName) {
        return DEFAULT_PATH_RESOLVER.resolveDatabaseFilePath(dbName);
    }

    public String getDatabaseFilePath(String dbName) {
        return pathResolver.resolveDatabaseFilePath(dbName);
    }

    public String getLogFilePath(String dbName) {
        return pathResolver.resolveLogFilePath(dbName);
    }

    public void dropDatabase(String dbName) {
        File dbDir = new File(pathResolver.resolveDatabaseDirectory(dbName));
        if (!dbDir.exists() || !dbDir.isDirectory()) {
            throw new RuntimeException("Database '" + dbName + "' does not exist.");
        }
        deleteDirectory(dbDir);
    }

    private void deleteDirectory(File directory) {
        File[] allContents = directory.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        directory.delete();
    }
}

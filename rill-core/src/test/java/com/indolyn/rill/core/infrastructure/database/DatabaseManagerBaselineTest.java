package com.indolyn.rill.core.infrastructure.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.indolyn.rill.core.storage.database.DatabaseManager;
import com.indolyn.rill.core.storage.database.DatabasePathResolver;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DatabaseManagerBaselineTest {

    @TempDir
    Path tempDir;

    @Test
    void databaseManagerShouldCreateListAndDropDatabasesWithinResolvedRoot() throws Exception {
        DatabaseManager databaseManager = new DatabaseManager(new TestPathResolver(tempDir));

        databaseManager.createDatabase("demo");
        databaseManager.createDatabase("demo2");

        List<String> databases = databaseManager.listDatabases();
        assertTrue(databases.contains("demo"));
        assertTrue(databases.contains("demo2"));
        assertTrue(Files.isDirectory(tempDir.resolve("demo")));

        databaseManager.dropDatabase("demo");

        assertFalse(Files.exists(tempDir.resolve("demo")));
        assertEquals(List.of("demo2"), databaseManager.listDatabases());
    }

    @Test
    void databaseManagerShouldRejectDuplicateDatabaseNames() {
        DatabaseManager databaseManager = new DatabaseManager(new TestPathResolver(tempDir));

        databaseManager.createDatabase("demo");

        RuntimeException exception =
            assertThrows(RuntimeException.class, () -> databaseManager.createDatabase("demo"));
        assertTrue(exception.getMessage().contains("already exists"));
    }

    private record TestPathResolver(Path root) implements DatabasePathResolver {
        @Override
        public String resolveDatabaseRootPath() {
            return root.toString();
        }

        @Override
        public String resolveDatabaseDirectory(String dbName) {
            return root.resolve(dbName).toString();
        }

        @Override
        public String resolveDatabaseFilePath(String dbName) {
            return root.resolve(dbName).resolve("rill.data").toString();
        }

        @Override
        public String resolveLogFilePath(String dbName) {
            return root.resolve(dbName).resolve("rill.data.log").toString();
        }
    }
}

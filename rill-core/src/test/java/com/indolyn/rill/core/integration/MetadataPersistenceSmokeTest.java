package com.indolyn.rill.core.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.indolyn.rill.core.execution.QueryProcessor;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;

class MetadataPersistenceSmokeTest {

    private static final String TEST_DB_NAME = "core_metadata_persistence_smoke_test_db";

    @Test
    void schemaAndIndexMetadataShouldSurviveRestart() throws Exception {
        deleteDirectory(new File("data/" + TEST_DB_NAME));

        QueryProcessor queryProcessor = new QueryProcessor(TEST_DB_NAME);
        try {
            queryProcessor.executeAndGetResult(
                "CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(20) NOT NULL DEFAULT 'guest');");
            queryProcessor.executeAndGetResult("ALTER TABLE users ADD COLUMN email VARCHAR(50);");
            queryProcessor.executeAndGetResult("CREATE INDEX idx_users_email ON users (email);");
        } finally {
            queryProcessor.close();
        }

        QueryProcessor recoveredProcessor = new QueryProcessor(TEST_DB_NAME);
        try {
            String showColumnsResult = recoveredProcessor.executeAndGetResult("SHOW COLUMNS FROM users;");
            String showCreateResult = recoveredProcessor.executeAndGetResult("SHOW CREATE TABLE users;");

            assertTrue(showColumnsResult.contains("email"));
            assertTrue(showCreateResult.contains("`email` VARCHAR(50)"));
            assertTrue(
                showCreateResult.contains("DEFAULT 'guest'")
                    || showCreateResult.contains("DEFAULT guest"));
            assertTrue(showCreateResult.contains("NOT NULL"));
        } finally {
            recoveredProcessor.close();
            deleteDirectory(new File("data/" + TEST_DB_NAME));
        }
    }

    private void deleteDirectory(File directory) throws IOException {
        if (!directory.exists()) {
            return;
        }
        File[] allContents = directory.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        if (!directory.delete()) {
            throw new IOException("Failed to delete " + directory.getAbsolutePath());
        }
    }
}

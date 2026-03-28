package com.indolyn.rill.core.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.indolyn.rill.core.execution.QueryProcessor;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;

class RecoverySmokeTest {

    private static final String TEST_DB_NAME = "core_recovery_smoke_test_db";

    @Test
    void queryProcessorShouldRecoverVisibleDataAfterRestart() throws Exception {
        deleteDirectory(new File("data/" + TEST_DB_NAME));

        QueryProcessor firstProcessor = new QueryProcessor(TEST_DB_NAME);
        try {
            firstProcessor.executeAndGetResult("CREATE TABLE users (id INT, name VARCHAR);");
            firstProcessor.executeAndGetResult("INSERT INTO users (id, name) VALUES (1, 'alice');");
        } finally {
            firstProcessor.close();
        }

        QueryProcessor secondProcessor = new QueryProcessor(TEST_DB_NAME);
        try {
            String result = secondProcessor.executeAndGetResult("SELECT * FROM users;");
            assertTrue(result.contains("alice"));
            assertTrue(result.contains("id"));
            assertTrue(result.contains("name"));
        } finally {
            secondProcessor.close();
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

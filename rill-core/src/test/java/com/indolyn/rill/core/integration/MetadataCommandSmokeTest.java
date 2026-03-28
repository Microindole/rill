package com.indolyn.rill.core.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.indolyn.rill.core.execution.QueryProcessor;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;

class MetadataCommandSmokeTest {

    private static final String TEST_DB_NAME = "core_metadata_command_smoke_test";

    @Test
    void queryProcessorShouldRenderShowColumnsAndShowCreateTableResults() throws Exception {
        deleteDirectory(new File("data/" + TEST_DB_NAME));

        QueryProcessor queryProcessor = new QueryProcessor(TEST_DB_NAME);
        try {
            queryProcessor.executeAndGetResult(
                "CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(10) NOT NULL DEFAULT 'guest');");

            String showColumnsResult = queryProcessor.executeAndGetResult("SHOW COLUMNS FROM users;");
            String showCreateResult = queryProcessor.executeAndGetResult("SHOW CREATE TABLE users;");

            assertTrue(showColumnsResult.contains("id"));
            assertTrue(showColumnsResult.toLowerCase().contains("varchar"));
            assertTrue(showCreateResult.contains("PRIMARY KEY"));
            assertTrue(showCreateResult.contains("guest"));
            assertTrue(showCreateResult.contains("users"));
        } finally {
            queryProcessor.close();
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

package com.indolyn.rill.core.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.indolyn.rill.core.execution.QueryProcessor;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;

class DatabaseCommandFailureSmokeTest {

    private static final String TEST_DB_NAME = "core_database_command_failure_smoke_test";

    @Test
    void queryProcessorShouldRejectUseOfMissingDatabase() throws Exception {
        deleteDirectory(new File("data/" + TEST_DB_NAME));

        QueryProcessor queryProcessor = new QueryProcessor(TEST_DB_NAME);
        try {
            String result = queryProcessor.executeAndGetResult("USE db_that_does_not_exist;");
            assertTrue(result.contains("does not exist"));
        } finally {
            queryProcessor.close();
            deleteDirectory(new File("data/" + TEST_DB_NAME));
        }
    }

    @Test
    void queryProcessorShouldRejectShowCreateTableForMissingTable() throws Exception {
        deleteDirectory(new File("data/" + TEST_DB_NAME));

        QueryProcessor queryProcessor = new QueryProcessor(TEST_DB_NAME);
        try {
            String result = queryProcessor.executeAndGetResult("SHOW CREATE TABLE missing_table;");
            assertTrue(result.startsWith("ERROR:"));
            assertTrue(result.toLowerCase().contains("not found"));
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

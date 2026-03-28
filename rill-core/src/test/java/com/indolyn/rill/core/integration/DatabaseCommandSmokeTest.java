package com.indolyn.rill.core.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.indolyn.rill.core.execution.QueryProcessor;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;

class DatabaseCommandSmokeTest {

    private static final String TEST_DB_NAME = "core_database_command_smoke_test";
    private static final String CREATED_DB_NAME = "db_command_demo";

    @Test
    void queryProcessorShouldHandleCreateShowUseAndDropDatabaseCommands() throws Exception {
        deleteDirectory(new File("data/" + TEST_DB_NAME));
        deleteDirectory(new File("data/" + CREATED_DB_NAME));

        QueryProcessor queryProcessor = new QueryProcessor(TEST_DB_NAME);
        try {
            String createResult = queryProcessor.executeAndGetResult("CREATE DATABASE " + CREATED_DB_NAME + ";");
            String showResult = queryProcessor.executeAndGetResult("SHOW DATABASES;");
            String useResult = queryProcessor.executeAndGetResult("USE " + CREATED_DB_NAME + ";");
            String dropResult = queryProcessor.executeAndGetResult("DROP DATABASE " + CREATED_DB_NAME + ";");
            String showAfterDropResult = queryProcessor.executeAndGetResult("SHOW DATABASES;");

            assertTrue(createResult.contains("created") || createResult.contains("Query OK"));
            assertTrue(showResult.contains(CREATED_DB_NAME));
            assertTrue(useResult.contains("Database changed to '" + CREATED_DB_NAME + "'."));
            assertTrue(dropResult.contains("dropped") || dropResult.contains("Query OK"));
            assertTrue(!showAfterDropResult.contains(CREATED_DB_NAME));
        } finally {
            queryProcessor.close();
            deleteDirectory(new File("data/" + TEST_DB_NAME));
            deleteDirectory(new File("data/" + CREATED_DB_NAME));
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

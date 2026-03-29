package com.indolyn.rill.core.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.indolyn.rill.core.execution.QueryProcessor;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class QueryFailurePathSmokeTest {

    private static final String TEST_DB_NAME = "core_query_failure_path_smoke_test_db";

    private QueryProcessor queryProcessor;

    @BeforeEach
    void setUp() {
        deleteDirectory(new File("data/" + TEST_DB_NAME));
        queryProcessor = new QueryProcessor(TEST_DB_NAME);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (queryProcessor != null) {
            queryProcessor.close();
        }
        deleteDirectory(new File("data/" + TEST_DB_NAME));
    }

    @Test
    void unsupportedSubqueryShouldReturnStructuredError() {
        queryProcessor.executeAndGetResult("CREATE TABLE users (id INT, name VARCHAR);");

        String result =
            queryProcessor.executeAndGetResult(
                "SELECT * FROM users WHERE id = (SELECT id FROM users);");

        assertTrue(result.contains("ERROR:"));
        assertTrue(result.toLowerCase().contains("expression"));
    }

    @Test
    void alterTableShouldRejectDuplicateColumnAndMissingTableMetadataCommands() {
        queryProcessor.executeAndGetResult("CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(20));");

        String duplicateColumnResult =
            queryProcessor.executeAndGetResult(
                "ALTER TABLE users ADD COLUMN name VARCHAR(10);");
        String missingShowCreate =
            queryProcessor.executeAndGetResult("SHOW CREATE TABLE missing_users;");

        assertTrue(duplicateColumnResult.contains("ERROR:"));
        assertTrue(duplicateColumnResult.contains("already exists"));
        assertTrue(missingShowCreate.contains("ERROR:"));
        assertTrue(missingShowCreate.contains("missing_users"));
    }

    private void deleteDirectory(File directory) {
        if (!directory.exists()) {
            return;
        }
        File[] allContents = directory.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        directory.delete();
    }
}

package com.indolyn.rill.core.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.indolyn.rill.core.execution.QueryProcessor;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class QueryProcessorSmokeTest {

    private static final String TEST_DB_NAME = "core_smoke_test_db";

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
    void queryProcessorShouldExecuteMinimalCreateInsertSelectFlow() {
        String createResult = queryProcessor.executeAndGetResult("CREATE TABLE users (id INT, name VARCHAR);");
        String insertResult = queryProcessor.executeAndGetResult("INSERT INTO users (id, name) VALUES (1, 'alice');");
        String selectResult = queryProcessor.executeAndGetResult("SELECT * FROM users;");

        assertTrue(createResult.contains("Table 'users' created") || createResult.contains("Query OK"));
        assertTrue(insertResult.contains("1 rows affected"));
        assertTrue(selectResult.contains("alice"));
        assertTrue(selectResult.contains("id"));
        assertTrue(selectResult.contains("name"));
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

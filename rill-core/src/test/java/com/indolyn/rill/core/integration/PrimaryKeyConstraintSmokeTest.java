package com.indolyn.rill.core.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.indolyn.rill.core.execution.QueryProcessor;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PrimaryKeyConstraintSmokeTest {

    private static final String TEST_DB_NAME = "core_primary_key_constraint_smoke_test_db";

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
    void duplicatePrimaryKeyInsertShouldBeRejectedAndKeepOriginalRow() {
        queryProcessor.executeAndGetResult("CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(20));");
        queryProcessor.executeAndGetResult("INSERT INTO users (id, name) VALUES (1, 'alice');");

        String duplicateInsertResult =
            queryProcessor.executeAndGetResult("INSERT INTO users (id, name) VALUES (1, 'bob');");
        String selectResult = queryProcessor.executeAndGetResult("SELECT * FROM users;");

        assertTrue(duplicateInsertResult.startsWith("ERROR:"));
        assertTrue(duplicateInsertResult.toLowerCase().contains("primary key"));
        assertTrue(selectResult.contains("alice"));
        assertTrue(!selectResult.contains("bob"));
        assertTrue(selectResult.contains("1 rows returned"));
    }

    @Test
    void updateShouldRejectPrimaryKeyCollisionAndPreserveRows() {
        queryProcessor.executeAndGetResult("CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(20));");
        queryProcessor.executeAndGetResult("INSERT INTO users (id, name) VALUES (1, 'alice');");
        queryProcessor.executeAndGetResult("INSERT INTO users (id, name) VALUES (2, 'bob');");

        String collisionUpdateResult =
            queryProcessor.executeAndGetResult("UPDATE users SET id = 1 WHERE id = 2;");
        String selectResult = queryProcessor.executeAndGetResult("SELECT * FROM users ORDER BY id ASC;");

        assertTrue(collisionUpdateResult.startsWith("ERROR:"));
        assertTrue(collisionUpdateResult.toLowerCase().contains("primary key"));
        assertTrue(selectResult.contains("alice"));
        assertTrue(selectResult.contains("bob"));
        assertTrue(selectResult.contains("2 rows returned"));
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

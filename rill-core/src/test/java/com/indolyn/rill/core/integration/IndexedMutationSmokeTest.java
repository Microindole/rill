package com.indolyn.rill.core.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.indolyn.rill.core.execution.QueryProcessor;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IndexedMutationSmokeTest {

    private static final String TEST_DB_NAME = "core_indexed_mutation_smoke_test_db";

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
    void createIndexShouldSupportIndexedReadAndIndexedColumnUpdate() {
        queryProcessor.executeAndGetResult(
            "CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(20), score INT);");
        queryProcessor.executeAndGetResult("INSERT INTO users (id, name, score) VALUES (1, 'alice', 88);");
        queryProcessor.executeAndGetResult("INSERT INTO users (id, name, score) VALUES (2, 'bob', 91);");
        queryProcessor.executeAndGetResult("CREATE INDEX idx_users_score ON users (score);");

        String initialLookup =
            queryProcessor.executeAndGetResult("SELECT * FROM users WHERE score = 88;");
        String updateResult =
            queryProcessor.executeAndGetResult("UPDATE users SET score = 92 WHERE id = 1;");
        String updatedLookup =
            queryProcessor.executeAndGetResult("SELECT * FROM users WHERE score = 92;");
        String oldLookup =
            queryProcessor.executeAndGetResult("SELECT * FROM users WHERE score = 88;");

        assertTrue(initialLookup.contains("alice"));
        assertTrue(initialLookup.contains("1 rows returned"));
        assertTrue(updateResult.contains("1 rows affected"));
        assertTrue(updatedLookup.contains("alice"));
        assertTrue(updatedLookup.contains("92"));
        assertTrue(updatedLookup.contains("1 rows returned"));
        assertTrue(oldLookup.contains("0 rows returned"));
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

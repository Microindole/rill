package com.indolyn.rill.core.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.indolyn.rill.core.execution.QueryProcessor;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DmlMutationSmokeTest {

    private static final String TEST_DB_NAME = "core_dml_mutation_smoke_test_db";

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
    void updateAndDeleteShouldWorkOnIntPredicateWithSmallIntegerLiteral() {
        queryProcessor.executeAndGetResult("CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR);");
        queryProcessor.executeAndGetResult("INSERT INTO users (id, name) VALUES (1, 'alice');");

        String updateResult = queryProcessor.executeAndGetResult("UPDATE users SET name = 'bob' WHERE id = 1;");
        String selectAfterUpdate = queryProcessor.executeAndGetResult("SELECT * FROM users;");
        String deleteResult = queryProcessor.executeAndGetResult("DELETE FROM users WHERE id = 1;");
        String selectAfterDelete = queryProcessor.executeAndGetResult("SELECT * FROM users;");

        assertTrue(updateResult.contains("1 rows affected"));
        assertTrue(selectAfterUpdate.contains("bob"));
        assertTrue(deleteResult.contains("1 rows affected"));
        assertTrue(selectAfterDelete.contains("0 rows returned") || !selectAfterDelete.contains("bob"));
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

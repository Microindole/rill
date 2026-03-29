package com.indolyn.rill.core.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.indolyn.rill.core.execution.QueryProcessor;
import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TypeConstraintSmokeTest {

    private static final String TEST_DB_NAME = "core_type_constraint_smoke_test_db";

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
    void notNullColumnWithoutValueShouldRejectInsertAndKeepTableEmpty() {
        queryProcessor.executeAndGetResult(
            "CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(10) NOT NULL DEFAULT 'guest');");

        String insertResult = queryProcessor.executeAndGetResult("INSERT INTO users (id) VALUES (1);");
        String selectResult = queryProcessor.executeAndGetResult("SELECT * FROM users;");

        assertTrue(insertResult.startsWith("ERROR:"));
        assertTrue(selectResult.contains("0 rows returned"));
    }

    @Test
    void varcharAndNumericConstraintsShouldRejectInvalidInsertAndUpdate() {
        queryProcessor.executeAndGetResult(
            "CREATE TABLE invoices (id INT PRIMARY KEY, code VARCHAR(5), amount NUMERIC(5, 2));");
        queryProcessor.executeAndGetResult(
            "INSERT INTO invoices (id, code, amount) VALUES (1, 'A100', 12.34);");

        String tooLongCodeResult =
            queryProcessor.executeAndGetResult(
                "INSERT INTO invoices (id, code, amount) VALUES (2, 'TOO-LONG', 10.00);");
        String overflowUpdateResult =
            queryProcessor.executeAndGetResult(
                "UPDATE invoices SET amount = 12345.67 WHERE id = 1;");
        String selectResult = queryProcessor.executeAndGetResult("SELECT * FROM invoices;");

        assertTrue(tooLongCodeResult.startsWith("ERROR:"));
        assertTrue(overflowUpdateResult.startsWith("ERROR:"));
        assertTrue(selectResult.contains("A100"));
        assertTrue(selectResult.contains("12.34"));
        assertTrue(!selectResult.contains("12345.67"));
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

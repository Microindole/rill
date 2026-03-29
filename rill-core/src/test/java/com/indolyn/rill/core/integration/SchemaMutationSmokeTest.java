package com.indolyn.rill.core.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.indolyn.rill.core.execution.QueryProcessor;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SchemaMutationSmokeTest {

    private static final String TEST_DB_NAME = "core_schema_mutation_smoke_test_db";

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
    void createIndexShouldBeReflectedInMetadataViews() {
        queryProcessor.executeAndGetResult("CREATE TABLE users (id INT PRIMARY KEY, email VARCHAR(50));");

        String createIndexResult =
            queryProcessor.executeAndGetResult("CREATE INDEX idx_users_email ON users (email);");
        String showColumnsResult = queryProcessor.executeAndGetResult("SHOW COLUMNS FROM users;");
        String showCreateResult = queryProcessor.executeAndGetResult("SHOW CREATE TABLE users;");

        assertTrue(createIndexResult.contains("index") || createIndexResult.contains("Query OK"));
        assertTrue(showColumnsResult.contains("email"));
        assertTrue(showCreateResult.contains("KEY `idx_users_email` (`email`)"));
        assertTrue(showCreateResult.contains("`email` VARCHAR(50)"));
    }

    @Test
    void alterTableShouldAppendColumnAndUpdateMetadataViews() {
        queryProcessor.executeAndGetResult("CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(20));");

        String alterResult = queryProcessor.executeAndGetResult("ALTER TABLE users ADD COLUMN email VARCHAR(50);");
        String showColumnsResult = queryProcessor.executeAndGetResult("SHOW COLUMNS FROM users;");
        String showCreateResult = queryProcessor.executeAndGetResult("SHOW CREATE TABLE users;");

        assertTrue(alterResult.contains("altered"));
        assertTrue(showColumnsResult.contains("email"));
        assertTrue(showCreateResult.contains("`email` VARCHAR(50)"));
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

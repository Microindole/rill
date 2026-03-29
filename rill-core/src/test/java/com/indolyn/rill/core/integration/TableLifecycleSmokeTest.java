package com.indolyn.rill.core.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.indolyn.rill.core.execution.QueryProcessor;
import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TableLifecycleSmokeTest {

    private static final String TEST_DB_NAME = "core_table_lifecycle_smoke_test_db";

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
    void createShowAndDropTableShouldKeepMetadataViewsInSync() {
        String createUsersResult =
            queryProcessor.executeAndGetResult("CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR);");
        String createOrdersResult =
            queryProcessor.executeAndGetResult("CREATE TABLE orders (id INT PRIMARY KEY, amount INT);");
        String showTablesBeforeDrop = queryProcessor.executeAndGetResult("SHOW TABLES;");
        String dropUsersResult = queryProcessor.executeAndGetResult("DROP TABLE users;");
        String showTablesAfterDrop = queryProcessor.executeAndGetResult("SHOW TABLES;");

        assertTrue(createUsersResult.contains("created"));
        assertTrue(createOrdersResult.contains("created"));
        assertTrue(showTablesBeforeDrop.contains("users"));
        assertTrue(showTablesBeforeDrop.contains("orders"));
        assertTrue(dropUsersResult.contains("dropped") || dropUsersResult.contains("Query OK"));
        assertTrue(!showTablesAfterDrop.contains("users"));
        assertTrue(showTablesAfterDrop.contains("orders"));
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

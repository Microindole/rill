package com.indolyn.rill.core.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.indolyn.rill.core.execution.QueryProcessor;
import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AggregateFunctionSmokeTest {

    private static final String TEST_DB_NAME = "core_aggregate_function_smoke_test_db";

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
    void countMinMaxAndAvgShouldProduceExpectedAggregateValues() {
        queryProcessor.executeAndGetResult("CREATE TABLE scores (id INT PRIMARY KEY, score INT);");
        queryProcessor.executeAndGetResult("INSERT INTO scores (id, score) VALUES (1, 80);");
        queryProcessor.executeAndGetResult("INSERT INTO scores (id, score) VALUES (2, 90);");
        queryProcessor.executeAndGetResult("INSERT INTO scores (id, score) VALUES (3, 100);");

        String result =
            queryProcessor.executeAndGetResult(
                "SELECT COUNT(*), MIN(score), MAX(score), AVG(score) FROM scores;");

        assertTrue(result.contains("3"));
        assertTrue(result.contains("80"));
        assertTrue(result.contains("100"));
        assertTrue(result.contains("90"));
        assertTrue(result.contains("1 rows returned"));
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

package com.indolyn.rill.core.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.indolyn.rill.core.execution.QueryProcessor;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class QueryFeatureSmokeTest {

    private static final String TEST_DB_NAME = "core_query_feature_smoke_test_db";

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
    void orderByAndLimitShouldReturnTopRow() {
        queryProcessor.executeAndGetResult("CREATE TABLE scores (id INT PRIMARY KEY, name VARCHAR, score INT);");
        queryProcessor.executeAndGetResult("INSERT INTO scores (id, name, score) VALUES (1, 'alice', 88);");
        queryProcessor.executeAndGetResult("INSERT INTO scores (id, name, score) VALUES (2, 'bob', 95);");
        queryProcessor.executeAndGetResult("INSERT INTO scores (id, name, score) VALUES (3, 'carol', 91);");

        String result =
            queryProcessor.executeAndGetResult(
                "SELECT id, name, score FROM scores ORDER BY score DESC LIMIT 1;");

        assertTrue(result.contains("bob"));
        assertTrue(result.contains("95"));
        assertTrue(result.contains("1 rows returned"));
    }

    @Test
    void groupByHavingAndJoinShouldProduceExpectedRows() {
        queryProcessor.executeAndGetResult("CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR);");
        queryProcessor.executeAndGetResult("CREATE TABLE orders (id INT PRIMARY KEY, user_id INT, amount INT);");
        queryProcessor.executeAndGetResult("INSERT INTO users (id, name) VALUES (1, 'alice');");
        queryProcessor.executeAndGetResult("INSERT INTO users (id, name) VALUES (2, 'bob');");
        queryProcessor.executeAndGetResult("INSERT INTO orders (id, user_id, amount) VALUES (1, 1, 5);");
        queryProcessor.executeAndGetResult("INSERT INTO orders (id, user_id, amount) VALUES (2, 1, 4);");
        queryProcessor.executeAndGetResult("INSERT INTO orders (id, user_id, amount) VALUES (3, 2, 3);");

        String aggregateResult =
            queryProcessor.executeAndGetResult(
                "SELECT user_id, SUM(amount) FROM orders GROUP BY user_id HAVING SUM(amount) > 5 ORDER BY user_id ASC;");
        String joinResult =
            queryProcessor.executeAndGetResult(
                "SELECT users.name, orders.amount FROM users JOIN orders ON users.id = orders.user_id ORDER BY orders.amount DESC LIMIT 1;");

        assertTrue(aggregateResult.contains("| 1"));
        assertTrue(aggregateResult.contains("9"));
        assertTrue(aggregateResult.contains("1 rows returned"));
        assertTrue(joinResult.contains("alice"));
        assertTrue(joinResult.contains("5"));
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

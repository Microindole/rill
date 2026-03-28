package com.indolyn.rill.core.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.indolyn.rill.core.execution.QueryProcessor;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SecurityCommandSmokeTest {

    private static final String TEST_DB_NAME = "core_security_command_smoke_test_db";

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
    void createUserAndGrantShouldSucceedThroughCoreExecutionPipeline() {
        queryProcessor.executeAndGetResult("CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR);");

        String createUserResult =
            queryProcessor.executeAndGetResult("CREATE USER 'reporter' IDENTIFIED BY 'secret';");
        String grantResult =
            queryProcessor.executeAndGetResult("GRANT SELECT, INSERT ON users TO 'reporter';");

        assertTrue(createUserResult.contains("User 'reporter' created."));
        assertTrue(grantResult.contains("Grants successful for user 'reporter'."));
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

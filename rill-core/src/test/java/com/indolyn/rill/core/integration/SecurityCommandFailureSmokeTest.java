package com.indolyn.rill.core.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.indolyn.rill.core.execution.QueryProcessor;
import com.indolyn.rill.core.session.Session;
import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SecurityCommandFailureSmokeTest {

    private static final String TEST_DB_NAME = "core_security_command_failure_smoke_test_db";

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
    void nonRootSessionShouldBeRejectedForCreateUserAndGrant() {
        queryProcessor.executeAndGetResult("CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR);");

        Session analystSession = Session.createAuthenticatedSession(-1, "analyst");
        String createUserResult =
            queryProcessor.executeAndGetResult(
                "CREATE USER 'reporter' IDENTIFIED BY 'secret';", analystSession);
        String grantResult =
            queryProcessor.executeAndGetResult(
                "GRANT SELECT ON users TO 'reporter';", analystSession);

        assertTrue(createUserResult.startsWith("ERROR:"));
        assertTrue(createUserResult.toLowerCase().contains("access denied"));
        assertTrue(grantResult.startsWith("ERROR:"));
        assertTrue(grantResult.toLowerCase().contains("access denied"));
    }

    @Test
    void duplicateCreateUserShouldReturnStructuredError() {
        String firstCreate =
            queryProcessor.executeAndGetResult("CREATE USER 'reporter' IDENTIFIED BY 'secret';");
        String duplicateCreate =
            queryProcessor.executeAndGetResult("CREATE USER 'reporter' IDENTIFIED BY 'secret';");

        assertTrue(firstCreate.contains("created"));
        assertTrue(duplicateCreate.startsWith("ERROR:"));
        assertTrue(duplicateCreate.toLowerCase().contains("already exists"));
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

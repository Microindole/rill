package com.indolyn.rill.core.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.indolyn.rill.core.execution.operator.TupleIterator;
import com.indolyn.rill.core.session.Session;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class QueryProcessorTest {

    private static final String TEST_DB_NAME = "query_processor_test_db";
    private static final String TEST_USE_DB_NAME = "query_processor_use_db";

    private QueryProcessor queryProcessor;

    @BeforeEach
    void setUp() {
        deleteDirectory(new File("data/" + TEST_DB_NAME));
        deleteDirectory(new File("data/" + TEST_USE_DB_NAME));
        queryProcessor = new QueryProcessor(TEST_DB_NAME);
        queryProcessor.executeAndGetResult("CREATE TABLE users (id INT, name VARCHAR);");
        queryProcessor.executeAndGetResult("INSERT INTO users (id, name) VALUES (1, 'alice');");
    }

    @AfterEach
    void tearDown() throws IOException {
        if (queryProcessor != null) {
            queryProcessor.close();
        }
        deleteDirectory(new File("data/" + TEST_DB_NAME));
        deleteDirectory(new File("data/" + TEST_USE_DB_NAME));
    }

    @Test
    void executeAndGetResultShouldHandleFlushBufferBuiltInCommand() {
        String result = queryProcessor.executeAndGetResult("FLUSH_BUFFER;");

        assertEquals("Buffer pool cleared.", result);
    }

    @Test
    void executeMysqlShouldAcceptStatementsWithoutTrailingSemicolon() throws Exception {
        TupleIterator iterator =
            queryProcessor.executeMysql(
                "SELECT * FROM users",
                Session.createAuthenticatedSession(-1, "root"));

        assertNotNull(iterator);
    }

    @Test
    void executeAndGetResultShouldSupportUseDatabaseWithoutNullPlanCrash() {
        queryProcessor.executeAndGetResult("CREATE DATABASE " + TEST_USE_DB_NAME + ";");
        Session session = Session.createAuthenticatedSession(-1, "root");

        String result = queryProcessor.executeAndGetResult("USE " + TEST_USE_DB_NAME + ";", session);

        assertTrue(result.contains("Database changed to '" + TEST_USE_DB_NAME + "'."));
        assertEquals(TEST_USE_DB_NAME, session.getCurrentDatabase());
    }

    @Test
    void executeAndGetResultShouldRejectUnknownDatabaseOnUse() {
        Session session = Session.createAuthenticatedSession(-1, "root");

        String result = queryProcessor.executeAndGetResult("USE missing_db_123;", session);

        assertTrue(result.contains("does not exist"));
        assertNull(session.getCurrentDatabase());
    }

    @Test
    void executeAndGetResultShouldReflectCreateShowAndDropDatabaseLifecycle() {
        String createResult = queryProcessor.executeAndGetResult("CREATE DATABASE " + TEST_USE_DB_NAME + ";");
        String showAfterCreate = queryProcessor.executeAndGetResult("SHOW DATABASES;");
        String dropResult = queryProcessor.executeAndGetResult("DROP DATABASE " + TEST_USE_DB_NAME + ";");
        String showAfterDrop = queryProcessor.executeAndGetResult("SHOW DATABASES;");

        assertTrue(createResult.contains("Query OK."));
        assertTrue(showAfterCreate.contains(TEST_USE_DB_NAME));
        assertTrue(dropResult.contains("Query OK."));
        assertTrue(!showAfterDrop.contains(TEST_USE_DB_NAME));
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

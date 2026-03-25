package com.indolyn.rill.core.transaction;

import static org.junit.jupiter.api.Assertions.*;

import com.indolyn.rill.core.execution.QueryProcessor;
import com.indolyn.rill.core.execution.operator.TupleIterator;
import com.indolyn.rill.core.session.Session;
import com.indolyn.rill.core.storage.database.DatabaseManager;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RecoveryTest {

    private static final String TEST_DB_NAME = "recovery_test";
    private QueryProcessor queryProcessorForVerification;

    @BeforeEach
    void setUp() {
        deleteTestDatabase();
    }

    @AfterEach
    void tearDown() throws IOException {
        if (queryProcessorForVerification != null) {
            queryProcessorForVerification.close();
        }
        deleteTestDatabase();
    }

    @Test
    void shouldRollbackUncommittedChangesAfterRestart() throws Exception {
        QueryProcessor setupProcessor = new QueryProcessor(TEST_DB_NAME);
        setupProcessor.execute("CREATE TABLE recovery_test (id INT, status VARCHAR);");
        setupProcessor.execute("INSERT INTO recovery_test (id, status) VALUES (1, 'committed_A');");
        setupProcessor.execute("INSERT INTO recovery_test (id, status) VALUES (2, 'committed_B');");
        setupProcessor.close();

        QueryProcessor crashingProcessor = new QueryProcessor(TEST_DB_NAME);

        TransactionManager transactionManager =
            (TransactionManager) getField(crashingProcessor, "transactionManager");
        Transaction uncommittedTxn = transactionManager.begin();

        executeSqlInTransaction(
            crashingProcessor,
            "UPDATE recovery_test SET status = 'updated_but_crashed' WHERE id = 2;",
            uncommittedTxn);
        executeSqlInTransaction(
            crashingProcessor,
            "INSERT INTO recovery_test (id, status) VALUES (3, 'inserted_but_crashed');",
            uncommittedTxn);

        crashingProcessor.getBufferPoolManager().flushAllPages();
        crashingProcessor.getLogManager().flush();
        crashingProcessor.close();

        queryProcessorForVerification = new QueryProcessor(TEST_DB_NAME);

        String result =
            queryProcessorForVerification.executeAndGetResult(
                "SELECT * FROM recovery_test ORDER BY id;");

        assertTrue(result.contains("committed_A"), "Should contain committed data 'committed_A'");
        assertTrue(
            result.contains("committed_B"),
            "Should contain original 'committed_B' because UPDATE was rolled back");
        assertFalse(
            result.contains("updated_but_crashed"), "Should NOT contain rolled-back updated data");
        assertFalse(
            result.contains("inserted_but_crashed"), "Should NOT contain rolled-back inserted data");
        assertTrue(
            result.contains("2 rows returned"),
            "Should only have 2 rows after recovery, because INSERT was rolled back");
    }

    private void executeSqlInTransaction(QueryProcessor qp, String sql, Transaction txn)
        throws Exception {
        TupleIterator iterator =
            qp.createExecutorForQuery(
                sql, txn, Session.createAuthenticatedSession(txn.getTransactionId(), "root"));
        assertNotNull(iterator);
        while (iterator.hasNext()) {
            iterator.next();
        }
    }

    private Object getField(Object obj, String fieldName)
        throws NoSuchFieldException, IllegalAccessException {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }

    private void deleteTestDatabase() {
        File databaseDir = new File(DatabaseManager.getDbFilePath(TEST_DB_NAME)).getParentFile();
        deleteRecursively(databaseDir);
    }

    private void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        file.delete();
    }
}


package com.indolyn.rill.core.execution;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.indolyn.rill.core.catalog.Catalog;
import com.indolyn.rill.core.exception.SemanticException;
import com.indolyn.rill.core.model.Column;
import com.indolyn.rill.core.model.DataType;
import com.indolyn.rill.core.model.Schema;
import com.indolyn.rill.core.session.Session;
import com.indolyn.rill.core.sql.planner.Planner;
import com.indolyn.rill.core.sql.planner.plan.command.InsertPlanNode;
import com.indolyn.rill.core.storage.buffer.BufferPoolManager;
import com.indolyn.rill.core.storage.disk.DiskManager;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class QueryCompilerTest {

    private static final String TEST_DB_FILE = "query_compiler_test.db";

    private DiskManager diskManager;
    private BufferPoolManager bufferPoolManager;
    private Catalog catalog;
    private QueryCompiler queryCompiler;

    @BeforeEach
    void setUp() throws IOException {
        new File(TEST_DB_FILE).delete();
        diskManager = new DiskManager(TEST_DB_FILE);
        diskManager.open();
        bufferPoolManager = new BufferPoolManager(10, diskManager, "LRU");
        catalog = new Catalog(bufferPoolManager);
        catalog.createTable(
            "users",
            new Schema(List.of(new Column("id", DataType.INT), new Column("name", DataType.VARCHAR))));
        queryCompiler = new QueryCompiler(catalog, new Planner(catalog));
    }

    @AfterEach
    void tearDown() throws IOException {
        if (diskManager != null) {
            diskManager.close();
        }
        new File(TEST_DB_FILE).delete();
    }

    @Test
    void compileShouldProduceInsertPlanForValidInsert() {
        CompiledStatement compiledStatement =
            queryCompiler.compile(
                "INSERT INTO users (id, name) VALUES (1, 'alice');",
                Session.createAuthenticatedSession(-1, "root"));

        assertNotNull(compiledStatement);
        assertInstanceOf(InsertPlanNode.class, compiledStatement.plan());
    }

    @Test
    void compileShouldRejectUnauthorizedSelect() {
        assertThrows(
            SemanticException.class,
            () ->
                queryCompiler.compile(
                    "SELECT * FROM users;",
                    Session.createAuthenticatedSession(2, "guest")));
    }
}

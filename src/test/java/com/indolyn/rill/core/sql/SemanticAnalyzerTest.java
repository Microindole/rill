package com.indolyn.rill.core.sql;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.indolyn.rill.core.catalog.Catalog;
import com.indolyn.rill.core.exception.SemanticException;
import com.indolyn.rill.core.model.Column;
import com.indolyn.rill.core.model.DataType;
import com.indolyn.rill.core.model.Schema;
import com.indolyn.rill.core.session.Session;
import com.indolyn.rill.core.sql.ast.StatementNode;
import com.indolyn.rill.core.sql.lexer.Lexer;
import com.indolyn.rill.core.sql.parser.Parser;
import com.indolyn.rill.core.sql.semantic.SemanticAnalyzer;
import com.indolyn.rill.core.storage.buffer.BufferPoolManager;
import com.indolyn.rill.core.storage.disk.DiskManager;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SemanticAnalyzerTest {

    private static final String TEST_DB_FILE = "test_semantic.db";

    private DiskManager diskManager;
    private SemanticAnalyzer semanticAnalyzer;
    private Session rootSession;

    @BeforeEach
    void setUp() throws IOException {
        new File(TEST_DB_FILE).delete();
        diskManager = new DiskManager(TEST_DB_FILE);
        diskManager.open();
        BufferPoolManager bufferPoolManager = new BufferPoolManager(10, diskManager, "LRU");
        Catalog catalog = new Catalog(bufferPoolManager);
        semanticAnalyzer = new SemanticAnalyzer(catalog);
        rootSession = Session.createAuthenticatedSession(-1, "root");

        catalog.createTable(
            "users",
            new Schema(List.of(new Column("id", DataType.INT), new Column("name", DataType.VARCHAR))));
    }

    @AfterEach
    void tearDown() throws IOException {
        if (diskManager != null) {
            diskManager.close();
        }
        new File(TEST_DB_FILE).delete();
    }

    @Test
    void validStatementsShouldPassSemanticAnalysis() {
        assertDoesNotThrow(() -> semanticAnalyzer.analyze(parseSql("SELECT id, name FROM users;"), rootSession));
        assertDoesNotThrow(
            () -> semanticAnalyzer.analyze(parseSql("SELECT * FROM users WHERE id = 1;"), rootSession));
        assertDoesNotThrow(
            () ->
                semanticAnalyzer.analyze(
                    parseSql("INSERT INTO users (id, name) VALUES (100, 'test');"), rootSession));
        assertDoesNotThrow(
            () -> semanticAnalyzer.analyze(parseSql("DELETE FROM users WHERE name = 'test';"), rootSession));
    }

    @Test
    void createExistingTableShouldFail() {
        assertThrows(
            SemanticException.class,
            () -> semanticAnalyzer.analyze(parseSql("CREATE TABLE users (id INT);"), rootSession));
    }

    @Test
    void selectingMissingTableShouldFail() {
        assertThrows(
            SemanticException.class,
            () -> semanticAnalyzer.analyze(parseSql("SELECT * FROM non_existent_table;"), rootSession));
    }

    @Test
    void selectingMissingColumnShouldFail() {
        assertThrows(
            SemanticException.class,
            () -> semanticAnalyzer.analyze(parseSql("SELECT age FROM users;"), rootSession));
    }

    @Test
    void insertingMissingColumnShouldFail() {
        assertThrows(
            SemanticException.class,
            () -> semanticAnalyzer.analyze(parseSql("INSERT INTO users (age) VALUES (30);"), rootSession));
    }

    @Test
    void insertingWrongTypeShouldFail() {
        assertThrows(
            SemanticException.class,
            () ->
                semanticAnalyzer.analyze(
                    parseSql("INSERT INTO users (id) VALUES ('a string');"), rootSession));
    }

    @Test
    void whereClauseTypeMismatchShouldFail() {
        assertThrows(
            SemanticException.class,
            () -> semanticAnalyzer.analyze(parseSql("SELECT * FROM users WHERE name > 123;"), rootSession));
    }

    private StatementNode parseSql(String sql) {
        Lexer lexer = new Lexer(sql);
        Parser parser = new Parser(lexer.tokenize());
        return parser.parse();
    }
}

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
import com.indolyn.rill.core.sql.ast.statement.UseDatabaseStatementNode;
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
    private Catalog catalog;
    private SemanticAnalyzer semanticAnalyzer;
    private Session rootSession;

    @BeforeEach
    void setUp() throws IOException {
        new File(TEST_DB_FILE).delete();
        diskManager = new DiskManager(TEST_DB_FILE);
        diskManager.open();
        BufferPoolManager bufferPoolManager = new BufferPoolManager(10, diskManager, "LRU");
        catalog = new Catalog(bufferPoolManager);
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
        assertDoesNotThrow(
            () ->
                semanticAnalyzer.analyze(
                    parseSql("CREATE TABLE audit_log (id INTEGER, payload TEXT, amount NUMERIC);"),
                    rootSession));
        assertDoesNotThrow(
            () ->
                semanticAnalyzer.analyze(
                    parseSql(
                        "CREATE TABLE event_log (sid SMALLINT, bid BIGINT, created_at TIMESTAMP WITHOUT TIME ZONE);"),
                    rootSession));
        assertDoesNotThrow(
            () ->
                semanticAnalyzer.analyze(
                    parseSql(
                        "CREATE TABLE account_state (enabled BOOLEAN, birthday DATE, name VARCHAR(64), code CHAR(8));"),
                    rootSession));
        assertDoesNotThrow(() -> semanticAnalyzer.analyze(parseSql("USE demo;"), rootSession));
    }

    @Test
    void useDatabaseAstShouldStillBeAcceptedWhenNoDedicatedSemanticRuleExists() {
        StatementNode node = parseSql("USE demo;");

        assertDoesNotThrow(() -> semanticAnalyzer.analyze(node, rootSession));
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

    @Test
    void timestampLiteralShouldPassSemanticAnalysis() throws IOException {
        catalog.createTable("events", new Schema(List.of(new Column("created_at", DataType.TIMESTAMP))));
        assertDoesNotThrow(
            () ->
                semanticAnalyzer.analyze(
                    parseSql(
                        "INSERT INTO events (created_at) VALUES ('2026-03-26 10:11:12');"),
                    rootSession));
    }

    @Test
    void invalidTimestampLiteralShouldFail() throws IOException {
        catalog.createTable("events", new Schema(List.of(new Column("created_at", DataType.TIMESTAMP))));
        assertThrows(
            SemanticException.class,
            () ->
                semanticAnalyzer.analyze(
                    parseSql("INSERT INTO events (created_at) VALUES ('not-a-timestamp');"),
                    rootSession));
    }

    @Test
    void invalidVarcharArgumentShouldFail() {
        assertThrows(
            SemanticException.class,
            () ->
                semanticAnalyzer.analyze(
                    parseSql("CREATE TABLE invalid_name (name VARCHAR(0));"), rootSession));
    }

    @Test
    void invalidCharArgumentShouldFail() {
        assertThrows(
            SemanticException.class,
            () ->
                semanticAnalyzer.analyze(
                    parseSql("CREATE TABLE invalid_code (code CHAR(0));"), rootSession));
    }

    @Test
    void invalidNumericPrecisionScaleShouldFail() {
        assertThrows(
            SemanticException.class,
            () ->
                semanticAnalyzer.analyze(
                    parseSql("CREATE TABLE invalid_amount (amount NUMERIC(4, 8));"), rootSession));
    }

    @Test
    void textShouldRejectLengthArgument() {
        assertThrows(
            SemanticException.class,
            () ->
                semanticAnalyzer.analyze(
                    parseSql("CREATE TABLE invalid_payload (payload TEXT(10));"), rootSession));
    }

    @Test
    void booleanShouldRejectLengthArgument() {
        assertThrows(
            SemanticException.class,
            () ->
                semanticAnalyzer.analyze(
                    parseSql("CREATE TABLE invalid_flag (flag BOOLEAN(1));"), rootSession));
    }

    @Test
    void numericAndFloatingAliasesShouldPassSemanticAnalysis() {
        assertDoesNotThrow(
            () ->
                semanticAnalyzer.analyze(
                    parseSql(
                        "CREATE TABLE metrics (ratio REAL, score FLOAT8, amount NUMERIC(12, 4), payload TEXT);"),
                    rootSession));
    }

    @Test
    void createTableWithCompatibleDefaultsShouldPassSemanticAnalysis() {
        assertDoesNotThrow(
            () ->
                semanticAnalyzer.analyze(
                    parseSql(
                        "CREATE TABLE user_defaults (id INT PRIMARY KEY, name VARCHAR(5) NOT NULL DEFAULT 'guest', enabled BOOLEAN DEFAULT TRUE, amount NUMERIC(5, 2) DEFAULT 12.34);"),
                    rootSession));
    }

    @Test
    void createTableWithInvalidDefaultShouldFail() {
        assertThrows(
            SemanticException.class,
            () ->
                semanticAnalyzer.analyze(
                    parseSql(
                        "CREATE TABLE invalid_defaults (name VARCHAR(3) DEFAULT 'toolong');"),
                    rootSession));
        assertThrows(
            SemanticException.class,
            () ->
                semanticAnalyzer.analyze(
                    parseSql(
                        "CREATE TABLE invalid_null_default (name VARCHAR(3) NOT NULL DEFAULT NULL);"),
                    rootSession));
    }

    @Test
    void numericLiteralExceedingPrecisionOrScaleShouldFail() throws IOException {
        catalog.createTable(
            "prices",
            new Schema(List.of(new Column("amount", DataType.DECIMAL, "DECIMAL", List.of(5, 2)))));

        assertThrows(
            SemanticException.class,
            () ->
                semanticAnalyzer.analyze(
                    parseSql("INSERT INTO prices (amount) VALUES (1234.56);"),
                    rootSession));
        assertThrows(
            SemanticException.class,
            () ->
                semanticAnalyzer.analyze(
                    parseSql("INSERT INTO prices (amount) VALUES (12.345);"),
                    rootSession));
        assertDoesNotThrow(
            () ->
                semanticAnalyzer.analyze(
                    parseSql("INSERT INTO prices (amount) VALUES (123.45);"),
                    rootSession));
    }

    @Test
    void numericUpdateExceedingPrecisionOrScaleShouldFail() throws IOException {
        catalog.createTable(
            "prices",
            new Schema(List.of(new Column("amount", DataType.DECIMAL, "DECIMAL", List.of(5, 2)))));

        assertThrows(
            SemanticException.class,
            () ->
                semanticAnalyzer.analyze(
                    parseSql("UPDATE prices SET amount = 12.345;"),
                    rootSession));
        assertThrows(
            SemanticException.class,
            () ->
                semanticAnalyzer.analyze(
                    parseSql("UPDATE prices SET amount = 1234.56;"),
                    rootSession));
    }

    @Test
    void stringLiteralExceedingDeclaredLengthShouldFail() throws IOException {
        catalog.createTable(
            "limited_users",
            new Schema(
                List.of(
                    new Column("name", DataType.VARCHAR, "VARCHAR", List.of(5)),
                    new Column("code", DataType.CHAR, "CHAR", List.of(3)))));

        assertThrows(
            SemanticException.class,
            () ->
                semanticAnalyzer.analyze(
                    parseSql("INSERT INTO limited_users (name) VALUES ('toolong');"),
                    rootSession));
        assertThrows(
            SemanticException.class,
            () ->
                semanticAnalyzer.analyze(
                    parseSql("UPDATE limited_users SET code = 'abcd';"),
                    rootSession));
    }

    private StatementNode parseSql(String sql) {
        Lexer lexer = new Lexer(sql);
        Parser parser = new Parser(lexer.tokenize());
        return parser.parse();
    }
}

package com.indolyn.rill.core.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.indolyn.rill.core.catalog.Catalog;
import com.indolyn.rill.core.model.Column;
import com.indolyn.rill.core.model.DataType;
import com.indolyn.rill.core.model.Schema;
import com.indolyn.rill.core.session.Session;
import com.indolyn.rill.core.sql.ast.StatementNode;
import com.indolyn.rill.core.sql.lexer.Lexer;
import com.indolyn.rill.core.sql.parser.Parser;
import com.indolyn.rill.core.sql.planner.Planner;
import com.indolyn.rill.core.sql.planner.plan.PlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.CreateTablePlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.CreateDatabasePlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.DropDatabasePlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.InsertPlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.ShowDatabasesPlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.UseDatabasePlanNode;
import com.indolyn.rill.core.sql.planner.plan.query.ProjectPlanNode;
import com.indolyn.rill.core.sql.planner.plan.query.SeqScanPlanNode;
import com.indolyn.rill.core.sql.semantic.SemanticAnalyzer;
import com.indolyn.rill.core.storage.buffer.BufferPoolManager;
import com.indolyn.rill.core.storage.disk.DiskManager;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlannerTest {

    private static final String TEST_DB_FILE = "test_planner.db";

    private DiskManager diskManager;
    private Catalog catalog;
    private Planner planner;

    @BeforeEach
    void setUp() throws IOException {
        new File(TEST_DB_FILE).delete();
        diskManager = new DiskManager(TEST_DB_FILE);
        diskManager.open();
        BufferPoolManager bufferPoolManager = new BufferPoolManager(10, diskManager, "LRU");
        catalog = new Catalog(bufferPoolManager);
        planner = new Planner(catalog);

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
    void createTableShouldBuildCreateTablePlan() {
        CreateTablePlanNode plan =
            assertInstanceOf(
                CreateTablePlanNode.class,
                createPlanForSql("CREATE TABLE products (pid INT, pname VARCHAR);"));

        assertEquals("products", plan.getTableName());
        assertEquals(2, plan.getOutputSchema().getColumns().size());
    }

    @Test
    void systemStatementsShouldBuildDedicatedPlans() {
        CreateDatabasePlanNode createPlan =
            assertInstanceOf(
                CreateDatabasePlanNode.class, createPlanForSql("CREATE DATABASE analytics;"));
        ShowDatabasesPlanNode showPlan =
            assertInstanceOf(ShowDatabasesPlanNode.class, createPlanForSql("SHOW DATABASES;"));
        UseDatabasePlanNode usePlan =
            assertInstanceOf(UseDatabasePlanNode.class, createPlanForSql("USE analytics;"));
        DropDatabasePlanNode dropPlan =
            assertInstanceOf(
                DropDatabasePlanNode.class, createPlanForSql("DROP DATABASE analytics;"));

        assertEquals("analytics", createPlan.getDbName());
        assertNotNull(showPlan);
        assertEquals("analytics", usePlan.getDbName());
        assertEquals("analytics", dropPlan.getDbName());
    }

    @Test
    void createTableShouldAcceptPostgreSqlTypeAliases() {
        CreateTablePlanNode plan =
            assertInstanceOf(
                CreateTablePlanNode.class,
                createPlanForSql(
                    "CREATE TABLE metrics (id INTEGER, note TEXT, amount NUMERIC, ratio DOUBLE PRECISION);"));

        assertEquals(DataType.INT, plan.getOutputSchema().getColumns().get(0).getType());
        assertEquals(DataType.VARCHAR, plan.getOutputSchema().getColumns().get(1).getType());
        assertEquals(DataType.DECIMAL, plan.getOutputSchema().getColumns().get(2).getType());
        assertEquals(DataType.DOUBLE, plan.getOutputSchema().getColumns().get(3).getType());
    }

    @Test
    void createTableShouldMapPostgreSqlPhysicalTypes() {
        CreateTablePlanNode plan =
            assertInstanceOf(
                CreateTablePlanNode.class,
                createPlanForSql(
                    "CREATE TABLE events (sid SMALLINT, bid BIGINT, created_at TIMESTAMP, recorded_at TIMESTAMP WITHOUT TIME ZONE);"));

        assertEquals(DataType.SMALLINT, plan.getOutputSchema().getColumns().get(0).getType());
        assertEquals(DataType.BIGINT, plan.getOutputSchema().getColumns().get(1).getType());
        assertEquals(DataType.TIMESTAMP, plan.getOutputSchema().getColumns().get(2).getType());
        assertEquals(DataType.TIMESTAMP, plan.getOutputSchema().getColumns().get(3).getType());
    }

    @Test
    void createTableShouldMapBooleanDateAndLengthTypes() {
        CreateTablePlanNode plan =
            assertInstanceOf(
                CreateTablePlanNode.class,
                createPlanForSql(
                    "CREATE TABLE account_state (enabled BOOLEAN, birthday DATE, name VARCHAR(64), code CHAR(8));"));

        assertEquals(DataType.BOOLEAN, plan.getOutputSchema().getColumns().get(0).getType());
        assertEquals(DataType.DATE, plan.getOutputSchema().getColumns().get(1).getType());
        assertEquals(DataType.VARCHAR, plan.getOutputSchema().getColumns().get(2).getType());
        assertEquals(DataType.CHAR, plan.getOutputSchema().getColumns().get(3).getType());
        assertEquals("VARCHAR", plan.getOutputSchema().getColumns().get(2).getDeclaredTypeName());
        assertEquals(List.of(64), plan.getOutputSchema().getColumns().get(2).getTypeArguments());
        assertEquals("CHAR", plan.getOutputSchema().getColumns().get(3).getDeclaredTypeName());
        assertEquals(List.of(8), plan.getOutputSchema().getColumns().get(3).getTypeArguments());
    }

    @Test
    void createTableShouldMapNumericAndFloatingAliases() {
        CreateTablePlanNode plan =
            assertInstanceOf(
                CreateTablePlanNode.class,
                createPlanForSql(
                    "CREATE TABLE metrics (ratio REAL, score FLOAT8, amount NUMERIC(12, 4), payload TEXT);"));

        assertEquals(DataType.FLOAT, plan.getOutputSchema().getColumns().get(0).getType());
        assertEquals(DataType.DOUBLE, plan.getOutputSchema().getColumns().get(1).getType());
        assertEquals(DataType.DECIMAL, plan.getOutputSchema().getColumns().get(2).getType());
        assertEquals(DataType.VARCHAR, plan.getOutputSchema().getColumns().get(3).getType());
        assertEquals("DECIMAL", plan.getOutputSchema().getColumns().get(2).getDeclaredTypeName());
        assertEquals(List.of(12, 4), plan.getOutputSchema().getColumns().get(2).getTypeArguments());
    }

    @Test
    void createTableShouldPreserveNullabilityDefaultsAndPrimaryKeyMetadata() {
        CreateTablePlanNode plan =
            assertInstanceOf(
                CreateTablePlanNode.class,
                createPlanForSql(
                    "CREATE TABLE users_meta (id INT PRIMARY KEY, name VARCHAR(32) NOT NULL DEFAULT 'guest', enabled BOOLEAN DEFAULT TRUE);"));

        Column id = plan.getOutputSchema().getColumns().get(0);
        Column name = plan.getOutputSchema().getColumns().get(1);
        Column enabled = plan.getOutputSchema().getColumns().get(2);

        assertEquals("id", plan.getOutputSchema().getPrimaryKeyColumnName());
        assertEquals(false, id.isNullable());
        assertEquals(true, id.isPrimaryKey());
        assertEquals(false, name.isNullable());
        assertEquals("'guest'", name.getDefaultValue());
        assertEquals("TRUE", enabled.getDefaultValue());
    }

    @Test
    void insertShouldBuildTupleForSmallIntBigIntAndTimestamp() throws IOException {
        catalog.createTable(
            "events",
            new Schema(
                List.of(
                    new Column("sid", DataType.SMALLINT),
                    new Column("bid", DataType.BIGINT),
                    new Column("created_at", DataType.TIMESTAMP))));

        InsertPlanNode plan =
            assertInstanceOf(
                InsertPlanNode.class,
                createPlanForSql(
                    "INSERT INTO events (sid, bid, created_at) VALUES (12, 922337203685477580, '2026-03-26 10:11:12');"));

        assertEquals((short) 12, plan.getRawTuples().get(0).getValues().get(0).getValue());
        assertEquals(922337203685477580L, plan.getRawTuples().get(0).getValues().get(1).getValue());
        assertEquals(
            "2026-03-26T10:11:12",
            plan.getRawTuples().get(0).getValues().get(2).getValue().toString());
    }

    @Test
    void insertShouldBuildInsertPlanWithTuple() {
        InsertPlanNode plan =
            assertInstanceOf(
                InsertPlanNode.class,
                createPlanForSql("INSERT INTO users (id, name) VALUES (1, 'test');"));

        assertEquals("users", plan.getTableInfo().getTableName());
        assertEquals(1, plan.getRawTuples().size());
        assertEquals(1, plan.getRawTuples().get(0).getValues().get(0).getValue());
    }

    @Test
    void selectAllShouldBuildSeqScanPlan() {
        PlanNode plan = createPlanForSql("SELECT * FROM users;");

        assertInstanceOf(SeqScanPlanNode.class, plan);
        assertEquals(2, plan.getOutputSchema().getColumns().size());
    }

    @Test
    void selectProjectionShouldBuildProjectOnSeqScan() {
        ProjectPlanNode plan =
            assertInstanceOf(ProjectPlanNode.class, createPlanForSql("SELECT id FROM users;"));

        assertEquals(1, plan.getOutputSchema().getColumns().size());
        assertEquals("id", plan.getOutputSchema().getColumns().get(0).getName());
        assertInstanceOf(SeqScanPlanNode.class, plan.getChild());
    }

    @Test
    void selectWithWhereShouldPushPredicateIntoSeqScan() {
        SeqScanPlanNode plan =
            assertInstanceOf(
                SeqScanPlanNode.class, createPlanForSql("SELECT * FROM users WHERE id = 1;"));

        assertNotNull(plan.getPredicate());
        assertEquals(2, plan.getOutputSchema().getColumns().size());
    }

    @Test
    void selectWithWhereAndProjectionShouldBuildProjectOnPredicateSeqScan() {
        ProjectPlanNode plan =
            assertInstanceOf(
                ProjectPlanNode.class, createPlanForSql("SELECT name FROM users WHERE id > 10;"));
        SeqScanPlanNode childPlan = assertInstanceOf(SeqScanPlanNode.class, plan.getChild());

        assertEquals(1, plan.getOutputSchema().getColumns().size());
        assertEquals("name", plan.getOutputSchema().getColumns().get(0).getName());
        assertNotNull(childPlan.getPredicate());
    }

    private PlanNode createPlanForSql(String sql) {
        Lexer lexer = new Lexer(sql);
        Parser parser = new Parser(lexer.tokenize());
        StatementNode ast = parser.parse();

        SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer(catalog);
        semanticAnalyzer.analyze(ast, Session.createAuthenticatedSession(-1, "root"));

        return planner.createPlan(ast);
    }
}

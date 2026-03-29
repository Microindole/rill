package com.indolyn.rill.core.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.indolyn.rill.core.model.Column;
import com.indolyn.rill.core.model.DataType;
import com.indolyn.rill.core.model.Schema;
import com.indolyn.rill.core.session.Session;
import com.indolyn.rill.core.sql.ast.StatementNode;
import com.indolyn.rill.core.sql.ast.statement.SelectStatementNode;
import com.indolyn.rill.core.sql.ast.statement.UseDatabaseStatementNode;
import com.indolyn.rill.core.sql.planner.plan.query.JoinPlanNode;
import com.indolyn.rill.core.sql.planner.plan.query.ProjectPlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.UseDatabasePlanNode;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class QueryCompilerBaselineTest {

    private static final String TEST_DB_NAME = "compiler_baseline_test_db";

    private QueryRuntime queryRuntime;

    @BeforeEach
    void setUp() throws IOException {
        deleteDirectory(new File("data/" + TEST_DB_NAME));
        queryRuntime = new QueryRuntime(TEST_DB_NAME);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (queryRuntime != null) {
            queryRuntime.getBufferPoolManager().flushAllPages();
            queryRuntime.getLogManager().flush();
            queryRuntime.getDiskManager().close();
            queryRuntime.getLogManager().close();
        }
        deleteDirectory(new File("data/" + TEST_DB_NAME));
    }

    @Test
    void queryCompilerShouldParseSelectIntoAst() {
        QueryCompiler compiler = new QueryCompiler(queryRuntime.getCatalog(), queryRuntime.getPlanner());

        StatementNode ast = compiler.parse("SELECT * FROM users;");

        SelectStatementNode select = assertInstanceOf(SelectStatementNode.class, ast);
        assertEquals("users", select.fromTable().getName());
        assertTrue(select.isSelectAll());
    }

    @Test
    void queryCompilerShouldCompileUseDatabaseIntoPlanNode() {
        QueryCompiler compiler = new QueryCompiler(queryRuntime.getCatalog(), queryRuntime.getPlanner());

        StatementNode ast = compiler.parse("USE demo;");
        UseDatabaseStatementNode use = assertInstanceOf(UseDatabaseStatementNode.class, ast);

        UseDatabasePlanNode plan =
            assertInstanceOf(UseDatabasePlanNode.class, compiler.compileSystemStatement(use));
        assertEquals("demo", plan.getDbName());
    }

    @Test
    void queryCompilerShouldCompileSelectAfterSchemaIsPrepared() throws Exception {
        QueryCompiler compiler = new QueryCompiler(queryRuntime.getCatalog(), queryRuntime.getPlanner());
        queryRuntime.getCatalog().createTable(
            "users",
            new Schema(List.of(new Column("id", DataType.INT), new Column("name", DataType.VARCHAR))));

        CompiledStatement compiled =
            compiler.compile("SELECT * FROM users;", Session.createAuthenticatedSession(-1, "root"));

        assertNotNull(compiled);
        assertInstanceOf(SelectStatementNode.class, compiled.ast());
        assertNotNull(compiled.plan());
    }

    @Test
    void queryCompilerShouldCompileJoinAndAggregationQueriesAgainstPreparedCatalog() throws Exception {
        QueryCompiler compiler = new QueryCompiler(queryRuntime.getCatalog(), queryRuntime.getPlanner());
        queryRuntime.getCatalog().createTable(
            "users",
            new Schema(List.of(new Column("id", DataType.INT), new Column("name", DataType.VARCHAR))));
        queryRuntime.getCatalog().createTable(
            "orders",
            new Schema(List.of(new Column("id", DataType.INT), new Column("user_id", DataType.INT), new Column("amount", DataType.INT))));

        CompiledStatement joinCompiled =
            compiler.compile(
                "SELECT users.name, orders.amount FROM users JOIN orders ON users.id = orders.user_id;",
                Session.createAuthenticatedSession(-1, "root"));
        CompiledStatement aggregateCompiled =
            compiler.compile(
                "SELECT user_id, SUM(amount) FROM orders GROUP BY user_id HAVING SUM(amount) > 5;",
                Session.createAuthenticatedSession(-1, "root"));

        ProjectPlanNode joinProjectPlan = assertInstanceOf(ProjectPlanNode.class, joinCompiled.plan());
        assertInstanceOf(JoinPlanNode.class, joinProjectPlan.getChild());
        ProjectPlanNode aggregateProjectPlan =
            assertInstanceOf(ProjectPlanNode.class, aggregateCompiled.plan());

        assertNotNull(aggregateProjectPlan.getOutputSchema());
        assertEquals(2, aggregateProjectPlan.getOutputSchema().getColumns().size());
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

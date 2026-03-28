package com.indolyn.rill.core.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.indolyn.rill.core.model.Column;
import com.indolyn.rill.core.model.DataType;
import com.indolyn.rill.core.model.Schema;
import com.indolyn.rill.core.session.Session;
import com.indolyn.rill.core.sql.ast.StatementNode;
import com.indolyn.rill.core.sql.ast.statement.CreateIndexStatementNode;
import com.indolyn.rill.core.sql.ast.statement.CreateUserStatementNode;
import com.indolyn.rill.core.sql.ast.statement.GrantStatementNode;
import com.indolyn.rill.core.sql.ast.statement.ShowColumnsStatementNode;
import com.indolyn.rill.core.sql.ast.statement.ShowCreateTableStatementNode;
import com.indolyn.rill.core.sql.planner.plan.command.CreateIndexPlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.GrantPlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.ShowColumnsPlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.ShowCreateTablePlanNode;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommandPlanningTest {

    private static final String TEST_DB_NAME = "compiler_command_planning_test_db";

    private QueryRuntime queryRuntime;
    private QueryCompiler compiler;

    @BeforeEach
    void setUp() throws IOException {
        deleteDirectory(new File("data/" + TEST_DB_NAME));
        queryRuntime = new QueryRuntime(TEST_DB_NAME);
        compiler = new QueryCompiler(queryRuntime.getCatalog(), queryRuntime.getPlanner());
        queryRuntime.getCatalog()
            .createTable(
                "users",
                new Schema(List.of(new Column("id", DataType.INT), new Column("name", DataType.VARCHAR))));
        queryRuntime.getCatalog().createUser("reporter", "secret");
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
    void parserShouldRecognizeMetadataAndSecurityStatements() {
        StatementNode showColumns = compiler.parse("SHOW COLUMNS FROM users;");
        StatementNode showCreate = compiler.parse("SHOW CREATE TABLE users;");
        StatementNode createUser = compiler.parse("CREATE USER 'reporter' IDENTIFIED BY 'secret';");
        StatementNode grant = compiler.parse("GRANT SELECT, INSERT ON users TO 'reporter';");
        StatementNode createIndex = compiler.parse("CREATE INDEX idx_users_name ON users (name);");

        assertInstanceOf(ShowColumnsStatementNode.class, showColumns);
        assertInstanceOf(ShowCreateTableStatementNode.class, showCreate);
        assertInstanceOf(CreateUserStatementNode.class, createUser);
        assertInstanceOf(GrantStatementNode.class, grant);
        assertInstanceOf(CreateIndexStatementNode.class, createIndex);
    }

    @Test
    void compilerShouldPlanMetadataAndIndexStatementsAgainstPreparedCatalog() {
        CompiledStatement showColumns =
            compiler.compile(
                "SHOW COLUMNS FROM users;", Session.createAuthenticatedSession(-1, "root"));
        CompiledStatement showCreate =
            compiler.compile(
                "SHOW CREATE TABLE users;", Session.createAuthenticatedSession(-1, "root"));
        CompiledStatement createIndex =
            compiler.compile(
                "CREATE INDEX idx_users_name ON users (name);",
                Session.createAuthenticatedSession(-1, "root"));
        CompiledStatement grant =
            compiler.compile(
                "GRANT SELECT, INSERT ON users TO 'reporter';",
                Session.createAuthenticatedSession(-1, "root"));

        assertNotNull(showColumns);
        assertInstanceOf(ShowColumnsPlanNode.class, showColumns.plan());
        assertInstanceOf(ShowCreateTablePlanNode.class, showCreate.plan());
        assertInstanceOf(CreateIndexPlanNode.class, createIndex.plan());
        GrantPlanNode grantPlan = assertInstanceOf(GrantPlanNode.class, grant.plan());
        assertEquals("users", grantPlan.getTableName());
        assertEquals("reporter", grantPlan.getUsername());
        assertEquals(List.of("SELECT", "INSERT"), grantPlan.getPrivileges());
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

package com.indolyn.rill.core.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.indolyn.rill.core.sql.ast.statement.CreateTableStatementNode;
import com.indolyn.rill.core.sql.ast.statement.InsertStatementNode;
import com.indolyn.rill.core.sql.ast.statement.ShowColumnsStatementNode;
import com.indolyn.rill.core.sql.ast.statement.ShowCreateTableStatementNode;
import com.indolyn.rill.core.sql.ast.statement.UseDatabaseStatementNode;
import com.indolyn.rill.core.sql.ast.expression.IdentifierNode;

import java.util.List;

import org.junit.jupiter.api.Test;

class StatementTableNameResolverTest {

    @Test
    void resolverShouldExtractTableNamesFromSupportedStatements() {
        StatementTableNameResolver resolver = new StatementTableNameResolver();

        assertEquals(
            "users",
            resolver.resolve(new InsertStatementNode(new IdentifierNode("users"), List.of(), List.of())));
        assertEquals(
            "users",
            resolver.resolve(new ShowColumnsStatementNode(new IdentifierNode("users"))));
        assertEquals(
            "users",
            resolver.resolve(new ShowCreateTableStatementNode(new IdentifierNode("users"))));
        assertEquals(
            "users",
            resolver.resolve(new CreateTableStatementNode(new IdentifierNode("users"), List.of(), null)));
    }

    @Test
    void resolverShouldReturnNullForStatementsWithoutTableContext() {
        StatementTableNameResolver resolver = new StatementTableNameResolver();

        assertNull(resolver.resolve(new UseDatabaseStatementNode(new IdentifierNode("demo"))));
    }
}

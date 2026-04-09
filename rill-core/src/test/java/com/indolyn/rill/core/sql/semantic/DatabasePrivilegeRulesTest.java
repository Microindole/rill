package com.indolyn.rill.core.sql.semantic;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.indolyn.rill.core.catalog.Catalog;
import com.indolyn.rill.core.catalog.TableInfo;
import com.indolyn.rill.core.exception.SemanticException;
import com.indolyn.rill.core.model.Column;
import com.indolyn.rill.core.model.DataType;
import com.indolyn.rill.core.model.Schema;
import com.indolyn.rill.core.sql.ast.expression.ColumnDefinitionNode;
import com.indolyn.rill.core.sql.ast.expression.IdentifierNode;
import com.indolyn.rill.core.sql.ast.expression.LiteralNode;
import com.indolyn.rill.core.sql.ast.statement.AlterTableStatementNode;
import com.indolyn.rill.core.sql.ast.statement.CreateTableStatementNode;
import com.indolyn.rill.core.sql.ast.statement.DropTableStatementNode;
import com.indolyn.rill.core.sql.ast.type.TypeReferenceNode;
import com.indolyn.rill.core.sql.lexer.Token;
import com.indolyn.rill.core.sql.lexer.TokenType;
import com.indolyn.rill.core.session.Session;
import com.indolyn.rill.core.sql.type.PostgreSqlTypeResolver;

import java.util.List;

import org.junit.jupiter.api.Test;

class DatabasePrivilegeRulesTest {

    @Test
    void createTableShouldBeAllowedInOwnDatabaseAndDefaultDatabase() {
        Catalog catalog = mock(Catalog.class);
        when(catalog.getTable("users")).thenReturn(null);
        DefinitionValidationSupport definitionValidationSupport =
            new DefinitionValidationSupport(new PostgreSqlTypeResolver());
        CreateTableSemanticValidator validator =
            new CreateTableSemanticValidator(catalog, definitionValidationSupport);

        ColumnDefinitionNode columnDefinition =
            new ColumnDefinitionNode(
                new IdentifierNode("id"),
                new TypeReferenceNode(List.of("INT"), List.of()),
                false,
                null,
                true);
        CreateTableStatementNode node =
            new CreateTableStatementNode(new IdentifierNode("users"), List.of(columnDefinition), new IdentifierNode("id"));

        Session ownerSession = Session.createAuthenticatedSession(1, "alice");
        ownerSession.setCurrentDatabase("alice");
        Session defaultSession = Session.createAuthenticatedSession(2, "bob");
        defaultSession.setCurrentDatabase("default");

        assertDoesNotThrow(() -> validator.analyze(node, ownerSession));
        assertDoesNotThrow(() -> validator.analyze(node, defaultSession));
    }

    @Test
    void createTableShouldRejectForeignDatabase() {
        Catalog catalog = mock(Catalog.class);
        when(catalog.getTable("users")).thenReturn(null);
        DefinitionValidationSupport definitionValidationSupport =
            new DefinitionValidationSupport(new PostgreSqlTypeResolver());
        CreateTableSemanticValidator validator =
            new CreateTableSemanticValidator(catalog, definitionValidationSupport);

        ColumnDefinitionNode columnDefinition =
            new ColumnDefinitionNode(
                new IdentifierNode("id"),
                new TypeReferenceNode(List.of("INT"), List.of()),
                false,
                null,
                true);
        CreateTableStatementNode node =
            new CreateTableStatementNode(new IdentifierNode("users"), List.of(columnDefinition), new IdentifierNode("id"));

        Session session = Session.createAuthenticatedSession(1, "alice");
        session.setCurrentDatabase("bill");

        assertThrows(SemanticException.class, () -> validator.analyze(node, session));
    }

    @Test
    void dropTableShouldFollowSameDatabaseBoundary() {
        Catalog catalog = mock(Catalog.class);
        Schema schema = new Schema(List.of(new Column("id", DataType.INT)));
        when(catalog.getTable("users")).thenReturn(new TableInfo("users", schema, null));
        DefinitionValidationSupport definitionValidationSupport =
            new DefinitionValidationSupport(new PostgreSqlTypeResolver());
        DropTableSemanticValidator validator =
            new DropTableSemanticValidator(definitionValidationSupport, new SemanticValidationSupport(catalog));

        DropTableStatementNode node = new DropTableStatementNode(new IdentifierNode("users"));

        Session session = Session.createAuthenticatedSession(1, "alice");
        session.setCurrentDatabase("alice");

        assertDoesNotThrow(() -> validator.analyze(node, session));
    }

    @Test
    void alterTableShouldFollowSameDatabaseBoundary() {
        Catalog catalog = mock(Catalog.class);
        Schema schema = new Schema(List.of(new Column("id", DataType.INT)));
        when(catalog.getTable("users")).thenReturn(new TableInfo("users", schema, null));
        DefinitionValidationSupport definitionValidationSupport =
            new DefinitionValidationSupport(new PostgreSqlTypeResolver());
        AlterTableSemanticValidator validator =
            new AlterTableSemanticValidator(definitionValidationSupport, new SemanticValidationSupport(catalog));

        ColumnDefinitionNode newColumn =
            new ColumnDefinitionNode(
                new IdentifierNode("name"),
                new TypeReferenceNode(List.of("VARCHAR"), List.of(32)),
                true,
                new LiteralNode(new Token(TokenType.STRING_CONST, "guest", 1, 1)),
                false);
        AlterTableStatementNode node =
            new AlterTableStatementNode(new IdentifierNode("users"), newColumn);

        Session session = Session.createAuthenticatedSession(1, "alice");
        session.setCurrentDatabase("alice");

        assertDoesNotThrow(() -> validator.analyze(node, session));
    }
}

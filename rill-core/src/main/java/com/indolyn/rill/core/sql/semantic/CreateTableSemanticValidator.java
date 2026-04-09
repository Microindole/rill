package com.indolyn.rill.core.sql.semantic;

import com.indolyn.rill.core.catalog.Catalog;
import com.indolyn.rill.core.exception.SemanticException;
import com.indolyn.rill.core.sql.ast.expression.ColumnDefinitionNode;
import com.indolyn.rill.core.sql.ast.statement.CreateTableStatementNode;
import com.indolyn.rill.core.session.Session;

class CreateTableSemanticValidator {

    private final Catalog catalog;
    private final DefinitionValidationSupport definitionValidationSupport;

    CreateTableSemanticValidator(Catalog catalog, DefinitionValidationSupport definitionValidationSupport) {
        this.catalog = catalog;
        this.definitionValidationSupport = definitionValidationSupport;
    }

    void analyze(CreateTableStatementNode node, Session session) {
        definitionValidationSupport.requireDatabaseOwnerOrRoot(
            session, session.getCurrentDatabase(), "CREATE TABLE");

        String tableName = node.tableName().getName();
        if (catalog.getTable(tableName) != null) {
            throw new SemanticException("Table '" + tableName + "' already exists.");
        }

        if (node.primaryKeyColumn() != null) {
            validatePrimaryKey(node);
        }

        for (ColumnDefinitionNode columnDefinition : node.columns()) {
            definitionValidationSupport.validateDataType(columnDefinition);
        }
    }

    private void validatePrimaryKey(CreateTableStatementNode node) {
        String primaryKeyColumnName = node.primaryKeyColumn().getName();
        boolean primaryKeyExists =
            node.columns().stream()
                .anyMatch(
                    columnDefinition ->
                        columnDefinition
                            .columnName()
                            .getName()
                            .equalsIgnoreCase(primaryKeyColumnName));
        if (!primaryKeyExists) {
            throw new SemanticException(
                "Primary key column '" + primaryKeyColumnName + "' not found in column list.");
        }
    }
}

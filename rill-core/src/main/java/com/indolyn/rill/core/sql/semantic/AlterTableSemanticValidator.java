package com.indolyn.rill.core.sql.semantic;

import com.indolyn.rill.core.catalog.TableInfo;
import com.indolyn.rill.core.exception.SemanticException;
import com.indolyn.rill.core.sql.ast.statement.AlterTableStatementNode;
import com.indolyn.rill.core.session.Session;

class AlterTableSemanticValidator {

    private final DefinitionValidationSupport definitionValidationSupport;
    private final SemanticValidationSupport semanticValidationSupport;

    AlterTableSemanticValidator(
        DefinitionValidationSupport definitionValidationSupport,
        SemanticValidationSupport semanticValidationSupport) {
        this.definitionValidationSupport = definitionValidationSupport;
        this.semanticValidationSupport = semanticValidationSupport;
    }

    void analyze(AlterTableStatementNode node, Session session) {
        definitionValidationSupport.requireRoot(session, "ALTER TABLE");

        String tableName = node.tableName().getName();
        TableInfo tableInfo = semanticValidationSupport.getTableOrThrow(tableName);

        String newColumnName = node.newColumnDefinition().columnName().getName();
        boolean columnExists =
            tableInfo.getSchema().getColumns().stream()
                .anyMatch(column -> column.getName().equalsIgnoreCase(newColumnName));
        if (columnExists) {
            throw new SemanticException(
                "Column '" + newColumnName + "' already exists in table '" + tableName + "'.");
        }

        definitionValidationSupport.validateDataType(node.newColumnDefinition());
    }
}

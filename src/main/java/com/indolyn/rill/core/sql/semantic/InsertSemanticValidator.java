package com.indolyn.rill.core.sql.semantic;

import com.indolyn.rill.core.catalog.TableInfo;
import com.indolyn.rill.core.exception.SemanticException;
import com.indolyn.rill.core.model.Column;
import com.indolyn.rill.core.model.Schema;
import com.indolyn.rill.core.sql.ast.ExpressionNode;
import com.indolyn.rill.core.sql.ast.expression.LiteralNode;
import com.indolyn.rill.core.sql.ast.statement.InsertStatementNode;
import com.indolyn.rill.core.session.Session;

class InsertSemanticValidator {

    private final SemanticValidationSupport validationSupport;

    InsertSemanticValidator(SemanticValidationSupport validationSupport) {
        this.validationSupport = validationSupport;
    }

    void analyze(InsertStatementNode node, Session session) {
        String tableName = node.tableName().getName();
        validationSupport.requireTablePermission(session, tableName, "INSERT");

        TableInfo tableInfo = validationSupport.getTableOrThrow(tableName);
        Schema schema = tableInfo.getSchema();

        if (node.columns().size() != node.values().size()) {
            throw new SemanticException("Number of columns does not match number of values.");
        }

        for (int i = 0; i < node.columns().size(); i++) {
            String columnName = node.columns().get(i).getName();
            Column schemaColumn =
                schema.getColumns().stream()
                    .filter(c -> c.getName().equalsIgnoreCase(columnName))
                    .findFirst()
                    .orElseThrow(
                        () ->
                            new SemanticException(
                                "Column '"
                                    + columnName
                                    + "' does not exist in table '"
                                    + tableName
                                    + "'."));

            ExpressionNode valueNode = node.values().get(i);
            if (!(valueNode instanceof LiteralNode literal)) {
                throw new SemanticException("INSERT statements currently only support literal values.");
            }

            validationSupport.validateLiteralAssignment(schemaColumn, literal);
        }
    }
}

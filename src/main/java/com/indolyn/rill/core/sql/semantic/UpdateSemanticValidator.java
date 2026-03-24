package com.indolyn.rill.core.sql.semantic;

import com.indolyn.rill.core.catalog.TableInfo;
import com.indolyn.rill.core.exception.SemanticException;
import com.indolyn.rill.core.model.Column;
import com.indolyn.rill.core.sql.ast.expression.LiteralNode;
import com.indolyn.rill.core.sql.ast.expression.SetClauseNode;
import com.indolyn.rill.core.sql.ast.statement.UpdateStatementNode;
import com.indolyn.rill.core.session.Session;

class UpdateSemanticValidator {

    private final SemanticValidationSupport validationSupport;

    UpdateSemanticValidator(SemanticValidationSupport validationSupport) {
        this.validationSupport = validationSupport;
    }

    void analyze(UpdateStatementNode node, Session session) {
        String tableName = node.tableName().getName();
        validationSupport.requireTablePermission(session, tableName, "UPDATE");

        TableInfo tableInfo = validationSupport.getTableOrThrow(tableName);
        validateAssignments(node, tableInfo);

        if (node.whereClause() != null) {
            validationSupport.analyzeSingleTableExpression(node.whereClause(), tableInfo);
        }
    }

    private void validateAssignments(UpdateStatementNode node, TableInfo tableInfo) {
        for (SetClauseNode clause : node.setClauses()) {
            Column column = validationSupport.checkColumnExists(tableInfo, clause.column());
            if (!(clause.value() instanceof LiteralNode literal)) {
                throw new SemanticException("SET clause currently only supports literal values.");
            }
            validationSupport.validateLiteralAssignment(column.getName(), column.getType(), literal);
        }
    }
}

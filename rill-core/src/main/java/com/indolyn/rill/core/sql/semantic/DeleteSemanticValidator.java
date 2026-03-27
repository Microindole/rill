package com.indolyn.rill.core.sql.semantic;

import com.indolyn.rill.core.catalog.TableInfo;
import com.indolyn.rill.core.sql.ast.statement.DeleteStatementNode;
import com.indolyn.rill.core.session.Session;

class DeleteSemanticValidator {

    private final SemanticValidationSupport validationSupport;

    DeleteSemanticValidator(SemanticValidationSupport validationSupport) {
        this.validationSupport = validationSupport;
    }

    void analyze(DeleteStatementNode node, Session session) {
        String tableName = node.tableName().getName();
        validationSupport.requireTablePermission(session, tableName, "DELETE");

        TableInfo tableInfo = validationSupport.getTableOrThrow(tableName);
        if (node.whereClause() != null) {
            validationSupport.analyzeSingleTableExpression(node.whereClause(), tableInfo);
        }
    }
}

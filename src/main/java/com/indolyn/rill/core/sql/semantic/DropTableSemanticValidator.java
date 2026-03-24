package com.indolyn.rill.core.sql.semantic;

import com.indolyn.rill.core.sql.ast.statement.DropTableStatementNode;
import com.indolyn.rill.core.session.Session;

class DropTableSemanticValidator {

    private final DefinitionValidationSupport definitionValidationSupport;
    private final SemanticValidationSupport semanticValidationSupport;

    DropTableSemanticValidator(
        DefinitionValidationSupport definitionValidationSupport,
        SemanticValidationSupport semanticValidationSupport) {
        this.definitionValidationSupport = definitionValidationSupport;
        this.semanticValidationSupport = semanticValidationSupport;
    }

    void analyze(DropTableStatementNode node, Session session) {
        definitionValidationSupport.requireRoot(session, "DROP TABLE");
        semanticValidationSupport.getTableOrThrow(node.tableName().getName());
    }
}

package com.indolyn.rill.core.sql.semantic;

import com.indolyn.rill.core.catalog.Catalog;
import com.indolyn.rill.core.exception.SemanticException;
import com.indolyn.rill.core.sql.ast.statement.CreateUserStatementNode;
import com.indolyn.rill.core.session.Session;

class CreateUserSemanticValidator {

    private final Catalog catalog;
    private final DefinitionValidationSupport definitionValidationSupport;

    CreateUserSemanticValidator(Catalog catalog, DefinitionValidationSupport definitionValidationSupport) {
        this.catalog = catalog;
        this.definitionValidationSupport = definitionValidationSupport;
    }

    void analyze(CreateUserStatementNode node, Session session) {
        definitionValidationSupport.requireRoot(session, "CREATE USER");
        String newUsername = node.username().getName();
        if (catalog.getPasswordHash(newUsername) != null) {
            throw new SemanticException("User '" + newUsername + "' already exists.");
        }
    }
}

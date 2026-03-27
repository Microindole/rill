package com.indolyn.rill.core.sql.semantic;

import com.indolyn.rill.core.catalog.Catalog;
import com.indolyn.rill.core.exception.SemanticException;
import com.indolyn.rill.core.sql.ast.expression.IdentifierNode;
import com.indolyn.rill.core.sql.ast.statement.GrantStatementNode;
import com.indolyn.rill.core.session.Session;

import java.util.Set;

class GrantSemanticValidator {

    private static final Set<String> VALID_PRIVILEGES =
        Set.of("SELECT", "INSERT", "UPDATE", "DELETE", "ALL");

    private final Catalog catalog;
    private final DefinitionValidationSupport definitionValidationSupport;

    GrantSemanticValidator(Catalog catalog, DefinitionValidationSupport definitionValidationSupport) {
        this.catalog = catalog;
        this.definitionValidationSupport = definitionValidationSupport;
    }

    void analyze(GrantStatementNode node, Session session) {
        definitionValidationSupport.requireRoot(session, "GRANT");
        String targetUsername = node.username().getName();
        if (catalog.getPasswordHash(targetUsername) == null) {
            throw new SemanticException("User '" + targetUsername + "' does not exist.");
        }
        for (IdentifierNode privilegeNode : node.privileges()) {
            String privilegeType = privilegeNode.getName().toUpperCase();
            if (!VALID_PRIVILEGES.contains(privilegeType)) {
                throw new SemanticException("Invalid privilege type '" + privilegeNode.getName() + "'.");
            }
        }
    }
}

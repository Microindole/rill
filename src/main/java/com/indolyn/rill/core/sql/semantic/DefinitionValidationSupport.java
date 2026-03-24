package com.indolyn.rill.core.sql.semantic;

import com.indolyn.rill.core.exception.SemanticException;
import com.indolyn.rill.core.model.DataType;
import com.indolyn.rill.core.sql.ast.expression.ColumnDefinitionNode;
import com.indolyn.rill.core.session.Session;

class DefinitionValidationSupport {

    void requireRoot(Session session, String privilege) {
        if (!"root".equalsIgnoreCase(session.getUsername())) {
            throw new SemanticException(
                "Access denied for user '"
                    + session.getUsername()
                    + "'. "
                    + privilege
                    + " privilege required.");
        }
    }

    void validateDataType(ColumnDefinitionNode columnDefinition) {
        try {
            DataType.valueOf(columnDefinition.dataType().getName().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new SemanticException(
                "Invalid data type '"
                    + columnDefinition.dataType().getName()
                    + "' for column '"
                    + columnDefinition.columnName().getName()
                    + "'.");
        }
    }
}

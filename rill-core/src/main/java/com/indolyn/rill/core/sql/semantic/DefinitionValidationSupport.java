package com.indolyn.rill.core.sql.semantic;

import com.indolyn.rill.core.exception.SemanticException;
import com.indolyn.rill.core.model.Column;
import com.indolyn.rill.core.model.DataType;
import com.indolyn.rill.core.sql.ast.expression.ColumnDefinitionNode;
import com.indolyn.rill.core.sql.lexer.TokenType;
import com.indolyn.rill.core.sql.type.SqlTypeResolver;
import com.indolyn.rill.core.session.Session;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

class DefinitionValidationSupport {

    private final SqlTypeResolver sqlTypeResolver;
    private static final String DEFAULT_DATABASE = "default";

    DefinitionValidationSupport(SqlTypeResolver sqlTypeResolver) {
        this.sqlTypeResolver = sqlTypeResolver;
    }

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

    void requireDatabaseOwnerOrRoot(Session session, String databaseName, String privilege) {
        if (isRoot(session) || isDefaultDatabase(databaseName) || isOwnedDatabase(session, databaseName)) {
            return;
        }
        throw new SemanticException(
            "Access denied for user '"
                + session.getUsername()
                + "'. "
                + privilege
                + " privilege required.");
    }

    void requireDatabaseCreatePermission(Session session, String databaseName) {
        if (isRoot(session) || isOwnedDatabase(session, databaseName)) {
            return;
        }
        throw new SemanticException(
            "Access denied for user '"
                + session.getUsername()
                + "'. CREATE DATABASE privilege required.");
    }

    void requireDatabaseDropPermission(Session session, String databaseName) {
        if (isRoot(session)) {
            return;
        }
        throw new SemanticException(
            "Access denied for user '"
                + session.getUsername()
                + "'. DROP DATABASE privilege required.");
    }

    private boolean isRoot(Session session) {
        return session != null && "root".equalsIgnoreCase(session.getUsername());
    }

    private boolean isDefaultDatabase(String databaseName) {
        return databaseName != null && DEFAULT_DATABASE.equalsIgnoreCase(databaseName);
    }

    private boolean isOwnedDatabase(Session session, String databaseName) {
        return session != null
            && session.getUsername() != null
            && databaseName != null
            && session.getUsername().equalsIgnoreCase(databaseName);
    }

    void validateDataType(ColumnDefinitionNode columnDefinition) {
        DataType physicalType;
        try {
            physicalType = sqlTypeResolver.resolve(columnDefinition.dataType()).physicalType();
        } catch (SemanticException e) {
            throw new SemanticException(
                "Invalid data type '"
                    + columnDefinition.dataType().displayName()
                    + "' for column '"
                    + columnDefinition.columnName().getName()
                    + "'.");
        }

        validateDefaultValue(columnDefinition, physicalType);
    }

    private void validateDefaultValue(ColumnDefinitionNode columnDefinition, DataType physicalType) {
        if (columnDefinition.defaultValue() == null) {
            return;
        }

        TokenType tokenType = columnDefinition.defaultValue().literal().type();
        if (tokenType == TokenType.NULL) {
            if (!columnDefinition.nullable() || columnDefinition.primaryKey()) {
                throw new SemanticException(
                    "Column '" + columnDefinition.columnName().getName() + "' cannot use DEFAULT NULL.");
            }
            return;
        }

        Column column =
            new Column(
                columnDefinition.columnName().getName(),
                physicalType,
                sqlTypeResolver.resolve(columnDefinition.dataType()).canonicalName(),
                columnDefinition.dataType().arguments(),
                columnDefinition.nullable(),
                null,
                columnDefinition.primaryKey());

        switch (physicalType) {
            case SMALLINT, INT, BIGINT -> {
                if (tokenType != TokenType.INTEGER_CONST) {
                    throw defaultMismatch(columnDefinition, physicalType);
                }
            }
            case DECIMAL, FLOAT, DOUBLE -> {
                if (tokenType != TokenType.INTEGER_CONST && tokenType != TokenType.DECIMAL_CONST) {
                    throw defaultMismatch(columnDefinition, physicalType);
                }
                if (physicalType == DataType.DECIMAL) {
                    BigDecimal value = new BigDecimal(columnDefinition.defaultValue().literal().lexeme());
                    if (!column.supportsDecimalValue(value)) {
                        throw new SemanticException(
                            "Default value for column '"
                                + columnDefinition.columnName().getName()
                                + "' exceeds NUMERIC("
                                + column.getNumericPrecision()
                                + ", "
                                + column.getNumericScale()
                                + ") constraints.");
                    }
                }
            }
            case VARCHAR, CHAR -> {
                if (tokenType != TokenType.STRING_CONST) {
                    throw defaultMismatch(columnDefinition, physicalType);
                }
                if (column.hasLengthLimit()
                    && columnDefinition.defaultValue().literal().lexeme().length() > column.getLengthLimit()) {
                    throw new SemanticException(
                        "Default value for column '"
                            + columnDefinition.columnName().getName()
                            + "' exceeds length limit "
                            + column.getLengthLimit()
                            + ".");
                }
            }
            case BOOLEAN -> {
                if (tokenType != TokenType.TRUE && tokenType != TokenType.FALSE) {
                    throw defaultMismatch(columnDefinition, physicalType);
                }
            }
            case DATE -> {
                if (tokenType != TokenType.STRING_CONST) {
                    throw defaultMismatch(columnDefinition, physicalType);
                }
                try {
                    LocalDate.parse(columnDefinition.defaultValue().literal().lexeme());
                } catch (DateTimeParseException e) {
                    throw new SemanticException(
                        "Invalid DATE default for column '" + columnDefinition.columnName().getName() + "'.");
                }
            }
            case TIMESTAMP -> {
                if (tokenType != TokenType.STRING_CONST) {
                    throw defaultMismatch(columnDefinition, physicalType);
                }
                try {
                    LocalDateTime.parse(
                        columnDefinition.defaultValue().literal().lexeme().replace(" ", "T"));
                } catch (DateTimeParseException e) {
                    throw new SemanticException(
                        "Invalid TIMESTAMP default for column '"
                            + columnDefinition.columnName().getName()
                            + "'.");
                }
            }
        }
    }

    private SemanticException defaultMismatch(ColumnDefinitionNode columnDefinition, DataType physicalType) {
        return new SemanticException(
            "Default value for column '"
                + columnDefinition.columnName().getName()
                + "' is incompatible with "
                + physicalType
                + ".");
    }
}

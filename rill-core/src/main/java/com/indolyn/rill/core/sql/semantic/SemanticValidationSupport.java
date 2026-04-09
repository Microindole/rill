package com.indolyn.rill.core.sql.semantic;

import com.indolyn.rill.core.catalog.Catalog;
import com.indolyn.rill.core.catalog.TableInfo;
import com.indolyn.rill.core.exception.SemanticException;
import com.indolyn.rill.core.model.Column;
import com.indolyn.rill.core.model.DataType;
import com.indolyn.rill.core.sql.ast.ExpressionNode;
import com.indolyn.rill.core.sql.ast.expression.BinaryExpressionNode;
import com.indolyn.rill.core.sql.ast.expression.IdentifierNode;
import com.indolyn.rill.core.sql.ast.expression.LiteralNode;
import com.indolyn.rill.core.sql.lexer.TokenType;
import com.indolyn.rill.core.session.Session;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

class SemanticValidationSupport {

    private static final String DEFAULT_DATABASE = "default";
    private final Catalog catalog;

    SemanticValidationSupport(Catalog catalog) {
        this.catalog = catalog;
    }

    void requireTablePermission(Session session, String tableName, String privilege) {
        if (isPrivilegedDatabase(session)) {
            return;
        }
        if (!catalog.hasPermission(session.getUsername(), tableName, privilege)) {
            throw new SemanticException(
                "Access denied for user '"
                    + session.getUsername()
                    + "'. "
                    + privilege
                    + " command denied on table '"
                    + tableName
                    + "'.");
        }
    }

    private boolean isPrivilegedDatabase(Session session) {
        if (session == null) {
            return false;
        }
        String username = session.getUsername();
        String currentDatabase = session.getCurrentDatabase();
        if (username == null || currentDatabase == null) {
            return false;
        }
        return DEFAULT_DATABASE.equalsIgnoreCase(currentDatabase)
            || username.equalsIgnoreCase(currentDatabase)
            || "root".equalsIgnoreCase(username);
    }

    TableInfo getTableOrThrow(String tableName) {
        TableInfo tableInfo = catalog.getTable(tableName);
        if (tableInfo == null) {
            throw new SemanticException("Table '" + tableName + "' not found.");
        }
        return tableInfo;
    }

    Column checkColumnExists(TableInfo tableInfo, IdentifierNode columnIdentifier) {
        String tableName = tableInfo.getTableName();
        String columnName = columnIdentifier.getName();
        String qualifier = columnIdentifier.getTableQualifier();

        if (qualifier != null && !qualifier.equalsIgnoreCase(tableName)) {
            throw new SemanticException(
                "Table qualifier '"
                    + qualifier
                    + "' does not match the table '"
                    + tableName
                    + "' in FROM clause.");
        }

        return tableInfo.getSchema().getColumns().stream()
            .filter(c -> c.getName().equalsIgnoreCase(columnName))
            .findFirst()
            .orElseThrow(
                () ->
                    new SemanticException(
                        "Column '" + columnName + "' not found in table '" + tableName + "'."));
    }

    DataType getLiteralType(LiteralNode literal) {
        TokenType type = literal.literal().type();
        if (type == TokenType.INTEGER_CONST) {
            return inferIntegerLiteralType(literal.literal().lexeme());
        }
        if (type == TokenType.DECIMAL_CONST) {
            return DataType.DECIMAL;
        }
        if (type == TokenType.STRING_CONST) {
            return DataType.VARCHAR;
        }
        if (type == TokenType.TRUE || type == TokenType.FALSE) {
            return DataType.BOOLEAN;
        }
        throw new SemanticException("Unsupported literal type: " + literal.literal().type());
    }

    void analyzeSingleTableExpression(ExpressionNode expr, TableInfo tableInfo) {
        if (expr instanceof BinaryExpressionNode binaryExpr) {
            TokenType opType = binaryExpr.operator().type();
            if (opType == TokenType.AND || opType == TokenType.OR) {
                analyzeSingleTableExpression(binaryExpr.left(), tableInfo);
                analyzeSingleTableExpression(binaryExpr.right(), tableInfo);
                return;
            }

            if (binaryExpr.left() instanceof IdentifierNode colNode
                && binaryExpr.right() instanceof LiteralNode literalNode) {
                Column column = checkColumnExists(tableInfo, colNode);
                DataType expectedType = column.getType();
                DataType actualType = getLiteralType(literalNode);
                if (!isCompatible(expectedType, actualType)) {
                    throw new SemanticException(
                        "Data type mismatch for column '"
                            + colNode.getFullName()
                            + "'. Expected "
                            + expectedType
                            + " but got "
                            + actualType
                            + ".");
                }
                return;
            }

            throw new SemanticException("Unsupported expression format in WHERE clause.");
        }
    }

    void validateLiteralAssignment(Column column, LiteralNode literal) {
        String columnName = column.getName();
        DataType expectedType = column.getType();
        DataType actualType = getLiteralType(literal);

        if (isCompatible(expectedType, actualType)) {
            validateLengthConstraint(column, literal);
            validateNumericConstraint(column, literal);
            return;
        }

        if (expectedType == DataType.DATE && actualType == DataType.VARCHAR) {
            validateDateLiteral(columnName, literal);
            return;
        }
        if (expectedType == DataType.TIMESTAMP && actualType == DataType.VARCHAR) {
            validateTimestampLiteral(columnName, literal);
            return;
        }

        throw new SemanticException(
            "Data type mismatch for column '"
                + columnName
                + "'. Expected "
                + expectedType
                + " but got "
                + actualType
                + ".");
    }

    private void validateLengthConstraint(Column column, LiteralNode literal) {
        if (!column.hasLengthLimit() || literal.literal().type() != TokenType.STRING_CONST) {
            return;
        }
        if (literal.literal().lexeme().length() > column.getLengthLimit()) {
            throw new SemanticException(
                "Value for column '"
                    + column.getName()
                    + "' exceeds length limit "
                    + column.getLengthLimit()
                    + ".");
        }
    }

    private void validateNumericConstraint(Column column, LiteralNode literal) {
        if (!column.hasNumericPrecision()) {
            return;
        }

        BigDecimal value = switch (literal.literal().type()) {
            case INTEGER_CONST, DECIMAL_CONST -> new BigDecimal(literal.literal().lexeme());
            default -> null;
        };

        if (value != null && !column.supportsDecimalValue(value)) {
            throw new SemanticException(
                "Value for column '"
                    + column.getName()
                    + "' exceeds NUMERIC("
                    + column.getNumericPrecision()
                    + ", "
                    + column.getNumericScale()
                    + ") constraints.");
        }
    }

    private boolean isCompatible(DataType expectedType, DataType actualType) {
        if (expectedType == actualType) {
            return true;
        }
        if (expectedType == DataType.SMALLINT && actualType == DataType.INT) {
            return true;
        }
        if (expectedType == DataType.INT
            && (actualType == DataType.SMALLINT || actualType == DataType.INT)) {
            return true;
        }
        if (expectedType == DataType.BIGINT
            && (actualType == DataType.SMALLINT || actualType == DataType.INT || actualType == DataType.BIGINT)) {
            return true;
        }
        if (expectedType == DataType.DECIMAL && actualType == DataType.INT) {
            return true;
        }
        if (expectedType == DataType.DECIMAL
            && (actualType == DataType.SMALLINT || actualType == DataType.BIGINT)) {
            return true;
        }
        if (expectedType == DataType.FLOAT
            && (actualType == DataType.SMALLINT
                || actualType == DataType.INT
                || actualType == DataType.BIGINT
                || actualType == DataType.DECIMAL)) {
            return true;
        }
        if (expectedType == DataType.DOUBLE
            && (actualType == DataType.SMALLINT
                || actualType == DataType.INT
                || actualType == DataType.BIGINT
                || actualType == DataType.DECIMAL
                || actualType == DataType.FLOAT)) {
            return true;
        }
        return expectedType == DataType.CHAR && actualType == DataType.VARCHAR;
    }

    private void validateDateLiteral(String columnName, LiteralNode literal) {
        try {
            LocalDate.parse(literal.literal().lexeme());
        } catch (DateTimeParseException e) {
            throw new SemanticException(
                "Invalid DATE format for column '" + columnName + "'. Expected 'YYYY-MM-DD'.");
        }
    }

    private void validateTimestampLiteral(String columnName, LiteralNode literal) {
        try {
            LocalDateTime.parse(literal.literal().lexeme().replace(" ", "T"));
        } catch (DateTimeParseException e) {
            throw new SemanticException(
                "Invalid TIMESTAMP format for column '"
                    + columnName
                    + "'. Expected 'YYYY-MM-DD HH:MM:SS'.");
        }
    }

    private DataType inferIntegerLiteralType(String lexeme) {
        try {
            short ignored = Short.parseShort(lexeme);
            return DataType.SMALLINT;
        } catch (NumberFormatException ignored) {
        }
        try {
            Integer.parseInt(lexeme);
            return DataType.INT;
        } catch (NumberFormatException ignored) {
        }
        try {
            Long.parseLong(lexeme);
            return DataType.BIGINT;
        } catch (NumberFormatException e) {
            throw new SemanticException("Integer literal out of range: " + lexeme);
        }
    }
}

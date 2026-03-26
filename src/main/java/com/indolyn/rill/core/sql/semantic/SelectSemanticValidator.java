package com.indolyn.rill.core.sql.semantic;

import com.indolyn.rill.core.catalog.Catalog;
import com.indolyn.rill.core.catalog.TableInfo;
import com.indolyn.rill.core.exception.SemanticException;
import com.indolyn.rill.core.model.Column;
import com.indolyn.rill.core.model.DataType;
import com.indolyn.rill.core.sql.lexer.TokenType;
import com.indolyn.rill.core.sql.ast.ExpressionNode;
import com.indolyn.rill.core.sql.ast.statement.SelectStatementNode;
import com.indolyn.rill.core.sql.ast.expression.AggregateExpressionNode;
import com.indolyn.rill.core.sql.ast.expression.BinaryExpressionNode;
import com.indolyn.rill.core.sql.ast.expression.IdentifierNode;
import com.indolyn.rill.core.sql.ast.expression.LiteralNode;
import com.indolyn.rill.core.session.Session;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

class SelectSemanticValidator {

    private final Catalog catalog;

    SelectSemanticValidator(Catalog catalog) {
        this.catalog = catalog;
    }

    void analyze(SelectStatementNode node, Session session) {
        String tableName = node.fromTable().getName();
        if (!catalog.hasPermission(session.getUsername(), tableName, "SELECT")) {
            throw new SemanticException(
                "Access denied for user '"
                    + session.getUsername()
                    + "'. SELECT command denied on table '"
                    + tableName
                    + "'.");
        }

        TableInfo leftTableInfo = getTableOrThrow(node.fromTable().getName());
        TableInfo rightTableInfo = validateJoin(node, leftTableInfo);
        validateProjection(node, leftTableInfo, rightTableInfo);
        validateWhere(node, leftTableInfo, rightTableInfo);
        validateOrderBy(node, leftTableInfo, rightTableInfo);
    }

    private TableInfo validateJoin(SelectStatementNode node, TableInfo leftTableInfo) {
        if (node.joinTable() == null) {
            return null;
        }

        TableInfo rightTableInfo = getTableOrThrow(node.joinTable().getName());
        if (node.joinCondition() == null) {
            throw new SemanticException("JOIN clause requires an ON condition.");
        }
        analyzeJoinExpression(node.joinCondition(), leftTableInfo, rightTableInfo);
        return rightTableInfo;
    }

    private void validateProjection(
        SelectStatementNode node, TableInfo leftTableInfo, TableInfo rightTableInfo) {
        Set<String> groupByColumnNames = new HashSet<>();
        if (!node.isSelectAll()) {
            for (ExpressionNode expr : node.selectList()) {
                if (expr instanceof IdentifierNode idNode) {
                    checkColumnExistsInJoinedTables(leftTableInfo, rightTableInfo, idNode);
                    if (!groupByColumnNames.isEmpty()
                        && !groupByColumnNames.contains(idNode.getName().toLowerCase())) {
                        throw new SemanticException(
                            "Column '"
                                + idNode.getFullName()
                                + "' must appear in the GROUP BY clause or be used in an aggregate function.");
                    }
                }
            }
        } else if (!groupByColumnNames.isEmpty()) {
            throw new SemanticException("SELECT * is not allowed with GROUP BY clause.");
        }

        analyzeSelectProjectAndGroupBy(node, leftTableInfo, rightTableInfo);
    }

    private void validateWhere(
        SelectStatementNode node, TableInfo leftTableInfo, TableInfo rightTableInfo) {
        if (node.whereClause() != null) {
            analyzeWhereOrJoinExpression(node.whereClause(), leftTableInfo, rightTableInfo);
        }
    }

    private void validateOrderBy(
        SelectStatementNode node, TableInfo leftTableInfo, TableInfo rightTableInfo) {
        if (node.orderByClause() != null) {
            checkColumnExistsInJoinedTables(leftTableInfo, rightTableInfo, node.orderByClause().column());
        }
    }

    private void analyzeSelectProjectAndGroupBy(
        SelectStatementNode node, TableInfo fromTable, TableInfo joinTable) {
        boolean hasAggregate =
            node.selectList().stream().anyMatch(e -> e instanceof AggregateExpressionNode);
        List<IdentifierNode> groupByCols = node.groupByClause();

        if (groupByCols != null && !groupByCols.isEmpty()) {
            for (IdentifierNode col : groupByCols) {
                checkColumnExistsInJoinedTables(fromTable, joinTable, col);
            }
        }

        if (!node.isSelectAll()) {
            for (ExpressionNode expr : node.selectList()) {
                if ((hasAggregate || (groupByCols != null && !groupByCols.isEmpty()))
                    && expr instanceof IdentifierNode idNode) {
                    boolean isInGroupBy =
                        groupByCols != null
                            && groupByCols.stream()
                            .anyMatch(gbCol -> gbCol.getName().equalsIgnoreCase(idNode.getName()));
                    if (!isInGroupBy) {
                        throw new SemanticException(
                            "Column '"
                                + idNode.getFullName()
                                + "' must appear in the GROUP BY clause or be used in an aggregate function.");
                    }
                } else if (expr instanceof IdentifierNode idNode) {
                    checkColumnExistsInJoinedTables(fromTable, joinTable, idNode);
                } else if (expr instanceof AggregateExpressionNode aggNode
                    && aggNode.argument() instanceof IdentifierNode argId) {
                    checkColumnExistsInJoinedTables(fromTable, joinTable, argId);
                }
            }
        }

        if (node.havingClause() != null) {
            if (groupByCols == null || groupByCols.isEmpty()) {
                throw new SemanticException("HAVING clause requires a GROUP BY clause.");
            }
            analyzeHavingClause(node.havingClause(), fromTable, joinTable, groupByCols);
        }
    }

    private void analyzeHavingClause(
        ExpressionNode havingNode,
        TableInfo fromTable,
        TableInfo joinTable,
        List<IdentifierNode> groupByCols) {
        if (havingNode instanceof BinaryExpressionNode binExpr) {
            analyzeHavingClause(binExpr.left(), fromTable, joinTable, groupByCols);
            analyzeHavingClause(binExpr.right(), fromTable, joinTable, groupByCols);
        } else if (havingNode instanceof IdentifierNode idNode) {
            boolean isInGroupBy =
                groupByCols.stream()
                    .anyMatch(gbCol -> gbCol.getName().equalsIgnoreCase(idNode.getName()));
            if (!isInGroupBy) {
                throw new SemanticException(
                    "Column '" + idNode.getFullName() + "' in HAVING clause must be in the GROUP BY list.");
            }
        } else if (havingNode instanceof AggregateExpressionNode aggNode) {
            if (aggNode.argument() instanceof IdentifierNode argId) {
                checkColumnExistsInJoinedTables(fromTable, joinTable, argId);
            }
        } else if (!(havingNode instanceof LiteralNode)) {
            throw new SemanticException("Unsupported expression in HAVING clause.");
        }
    }

    private TableInfo getTableOrThrow(String tableName) {
        TableInfo tableInfo = catalog.getTable(tableName);
        if (tableInfo == null) {
            throw new SemanticException("Table '" + tableName + "' not found.");
        }
        return tableInfo;
    }

    private DataType getLiteralType(LiteralNode literal) {
        TokenType type = literal.literal().type();
        if (type == TokenType.INTEGER_CONST) {
            String lexeme = literal.literal().lexeme();
            try {
                Short.parseShort(lexeme);
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

    private void analyzeWhereOrJoinExpression(
        ExpressionNode expr, TableInfo leftTable, TableInfo rightTable) {
        if (!(expr instanceof BinaryExpressionNode binaryExpr)) {
            return;
        }

        TokenType opType = binaryExpr.operator().type();
        if (opType == TokenType.AND || opType == TokenType.OR) {
            analyzeWhereOrJoinExpression(binaryExpr.left(), leftTable, rightTable);
            analyzeWhereOrJoinExpression(binaryExpr.right(), leftTable, rightTable);
            return;
        }

        if (binaryExpr.left() instanceof IdentifierNode colNode
            && binaryExpr.right() instanceof LiteralNode literalNode) {
            Column column = checkColumnExistsInJoinedTables(leftTable, rightTable, colNode);
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

        if (binaryExpr.left() instanceof IdentifierNode leftCol
            && binaryExpr.right() instanceof IdentifierNode rightCol) {
            checkColumnExistsInJoinedTables(leftTable, rightTable, leftCol);
            checkColumnExistsInJoinedTables(leftTable, rightTable, rightCol);
            return;
        }

        throw new SemanticException("Unsupported expression format in WHERE or ON clause.");
    }

    private boolean isCompatible(DataType expectedType, DataType actualType) {
        if (expectedType == actualType) {
            return true;
        }
        if (expectedType == DataType.INT && actualType == DataType.SMALLINT) {
            return true;
        }
        if (expectedType == DataType.BIGINT
            && (actualType == DataType.SMALLINT || actualType == DataType.INT)) {
            return true;
        }
        if (expectedType == DataType.DECIMAL
            && (actualType == DataType.SMALLINT || actualType == DataType.INT || actualType == DataType.BIGINT)) {
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
        return false;
    }

    private void analyzeJoinExpression(
        ExpressionNode expr, TableInfo leftTable, TableInfo rightTable) {
        if (!(expr instanceof BinaryExpressionNode binaryExpr)) {
            throw new SemanticException("Unsupported JOIN condition expression.");
        }
        if (!(binaryExpr.left() instanceof IdentifierNode)
            || !(binaryExpr.right() instanceof IdentifierNode)) {
            throw new SemanticException(
                "JOIN ON condition must be in the format 'table1.column1 = table2.column2'.");
        }
        checkColumnExistsInJoinedTables(leftTable, rightTable, (IdentifierNode) binaryExpr.left());
        checkColumnExistsInJoinedTables(leftTable, rightTable, (IdentifierNode) binaryExpr.right());
    }

    private Column checkColumnExistsInJoinedTables(
        TableInfo left, TableInfo right, IdentifierNode columnIdentifier) {
        String columnName = columnIdentifier.getName();
        String qualifier = columnIdentifier.getTableQualifier();

        if (qualifier != null) {
            if (qualifier.equalsIgnoreCase(left.getTableName())) {
                return checkColumnExists(left, columnIdentifier);
            }
            if (right != null && qualifier.equalsIgnoreCase(right.getTableName())) {
                return checkColumnExists(right, columnIdentifier);
            }
            throw new SemanticException(
                "Table qualifier '" + qualifier + "' not found in FROM or JOIN clause.");
        }

        boolean inLeft =
            left.getSchema().getColumns().stream()
                .anyMatch(c -> c.getName().equalsIgnoreCase(columnName));
        boolean inRight =
            right != null
                && right.getSchema().getColumns().stream()
                .anyMatch(c -> c.getName().equalsIgnoreCase(columnName));

        if (inLeft && inRight) {
            throw new SemanticException(
                "Column '"
                    + columnName
                    + "' is ambiguous; it exists in both tables. Please use a table qualifier (e.g., '"
                    + left.getTableName()
                    + "."
                    + columnName
                    + "').");
        }
        if (inLeft) {
            return checkColumnExists(left, columnIdentifier);
        }
        if (inRight) {
            return checkColumnExists(right, columnIdentifier);
        }
        throw new SemanticException("Column '" + columnName + "' not found in any specified table.");
    }

    private Column checkColumnExists(TableInfo tableInfo, IdentifierNode columnIdentifier) {
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
}

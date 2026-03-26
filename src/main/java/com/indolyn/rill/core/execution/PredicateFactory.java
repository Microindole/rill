package com.indolyn.rill.core.execution;

import com.indolyn.rill.core.model.Schema;
import com.indolyn.rill.core.model.Value;
import com.indolyn.rill.core.sql.ast.ExpressionNode;
import com.indolyn.rill.core.sql.ast.expression.BinaryExpressionNode;
import com.indolyn.rill.core.sql.ast.expression.IdentifierNode;
import com.indolyn.rill.core.sql.ast.expression.LiteralNode;
import com.indolyn.rill.core.execution.predicate.AbstractPredicate;
import com.indolyn.rill.core.execution.predicate.ComparisonPredicate;
import com.indolyn.rill.core.execution.predicate.LogicalPredicate;

class PredicateFactory {

    AbstractPredicate create(ExpressionNode expression, Schema schema) {
        if (expression instanceof BinaryExpressionNode node) {
            String operatorName = node.operator().type().name();
            if ("AND".equals(operatorName) || "OR".equals(operatorName)) {
                AbstractPredicate left = create(node.left(), schema);
                AbstractPredicate right = create(node.right(), schema);
                return new LogicalPredicate(left, right, operatorName);
            }

            if (!(node.left() instanceof IdentifierNode) || !(node.right() instanceof LiteralNode)) {
                throw new UnsupportedOperationException(
                    "WHERE clause only supports 'column_name op literal' format.");
            }

            String columnName = ((IdentifierNode) node.left()).getName();
            int columnIndex = getColumnIndex(schema, columnName);
            Value literalValue = getLiteralValue((LiteralNode) node.right());
            return new ComparisonPredicate(columnIndex, literalValue, operatorName);
        }

        throw new UnsupportedOperationException(
            "Unsupported expression type in WHERE clause: " + expression.getClass().getSimpleName());
    }

    private int getColumnIndex(Schema schema, String columnName) {
        for (int i = 0; i < schema.getColumns().size(); i++) {
            if (schema.getColumns().get(i).getName().equalsIgnoreCase(columnName)) {
                return i;
            }
        }
        throw new IllegalStateException(
            "Column '" + columnName + "' not found in schema during execution planning.");
    }

    private Value getLiteralValue(LiteralNode literalNode) {
        String lexeme = literalNode.literal().lexeme();
        return switch (literalNode.literal().type()) {
            case INTEGER_CONST -> parseIntegerLiteral(lexeme);
            case STRING_CONST -> new Value(lexeme);
            default -> throw new IllegalStateException("Unsupported literal type in expression.");
        };
    }

    private Value parseIntegerLiteral(String lexeme) {
        try {
            return new Value(Short.parseShort(lexeme));
        } catch (NumberFormatException ignored) {
        }
        try {
            return new Value(Integer.parseInt(lexeme));
        } catch (NumberFormatException ignored) {
        }
        return new Value(Long.parseLong(lexeme));
    }
}

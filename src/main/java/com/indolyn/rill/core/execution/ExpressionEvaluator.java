package com.indolyn.rill.core.execution;

import com.indolyn.rill.core.catalog.TableInfo;
import com.indolyn.rill.core.model.Schema;
import com.indolyn.rill.core.model.Tuple;
import com.indolyn.rill.core.model.Value;
import com.indolyn.rill.core.sql.ast.ExpressionNode;
import com.indolyn.rill.core.sql.ast.expression.AggregateExpressionNode;
import com.indolyn.rill.core.sql.ast.expression.BinaryExpressionNode;
import com.indolyn.rill.core.sql.ast.expression.IdentifierNode;
import com.indolyn.rill.core.sql.ast.expression.LiteralNode;

/**
 * 负责在执行阶段计算过滤条件和连接条件。
 */
public class ExpressionEvaluator {
    public static boolean evaluate(ExpressionNode expression, Schema schema, Tuple tuple) {
        return evaluate(expression, schema, tuple, null, null);
    }

    public static boolean evaluate(
        ExpressionNode expression,
        Schema schema,
        Tuple tuple,
        TableInfo leftTable,
        TableInfo rightTable) {
        if (expression instanceof BinaryExpressionNode node) {
            if (node.left() instanceof AggregateExpressionNode leftAgg
                && node.right() instanceof LiteralNode rightLiteral) {
                String aggColumnName = leftAgg.toString();
                int colIndex = schema.getColumnIndex(aggColumnName);
                if (colIndex == -1)
                    throw new IllegalStateException(
                        "Aggregate column '" + aggColumnName + "' not found in post-aggregation schema.");
                Value leftValue = tuple.getValues().get(colIndex);
                Value rightValue = getLiteralValue(rightLiteral);
                return compareValues(leftValue, rightValue, node.operator().type().name());
            } else if (node.left() instanceof LiteralNode leftLiteral
                && node.right() instanceof AggregateExpressionNode rightAgg) {
                String aggColumnName = rightAgg.toString();
                int colIndex = schema.getColumnIndex(aggColumnName);
                if (colIndex == -1)
                    throw new IllegalStateException(
                        "Aggregate column '" + aggColumnName + "' not found in post-aggregation schema.");
                Value leftValue = getLiteralValue(leftLiteral);
                Value rightValue = tuple.getValues().get(colIndex);
                return compareValues(leftValue, rightValue, node.operator().type().name());
            }

            Value leftValue = getValue(node.left(), schema, tuple, leftTable, rightTable);
            Value rightValue = getValue(node.right(), schema, tuple, leftTable, rightTable);

            return compareValues(leftValue, rightValue, node.operator().type().name());
        }
        throw new UnsupportedOperationException(
            "Unsupported expression type in WHERE or HAVING clause: "
                + expression.getClass().getSimpleName());
    }

    private static Value getValue(
        ExpressionNode node, Schema schema, Tuple tuple, TableInfo leftTable, TableInfo rightTable) {
        if (node instanceof IdentifierNode idNode) {
            int colIndex = getColumnIndex(schema, idNode, leftTable, rightTable);
            return tuple.getValues().get(colIndex);
        }
        if (node instanceof LiteralNode literalNode) {
            return getLiteralValue(literalNode);
        }
        throw new IllegalStateException(
            "Unsupported node type in expression: " + node.getClass().getSimpleName());
    }

    private static int getColumnIndex(
        Schema combinedSchema, IdentifierNode columnNode, TableInfo leftTable, TableInfo rightTable) {
        String columnName = columnNode.getName();
        String tableQualifier = columnNode.getTableQualifier();

        if (leftTable == null || rightTable == null) {
            return combinedSchema.getColumnIndex(columnName);
        }

        int leftSchemaSize = leftTable.getSchema().getColumns().size();

        if (tableQualifier != null) {
            if (tableQualifier.equalsIgnoreCase(leftTable.getTableName())) {
                return leftTable.getSchema().getColumnIndex(columnName);
            } else if (tableQualifier.equalsIgnoreCase(rightTable.getTableName())) {
                return leftSchemaSize + rightTable.getSchema().getColumnIndex(columnName);
            }
        }

        return combinedSchema.getColumnIndex(columnName);
    }

    private static Value getColumnValue(Tuple tuple, Schema schema, IdentifierNode columnNode) {
        String columnName = columnNode.getName();
        for (int i = 0; i < schema.getColumns().size(); i++) {
            if (schema.getColumns().get(i).getName().equalsIgnoreCase(columnName)) {
                return tuple.getValues().get(i);
            }
        }
        throw new IllegalStateException(
            "Column '"
                + columnName
                + "' not found in tuple schema. This should have been caught during semantic analysis.");
    }

    private static Value getLiteralValue(LiteralNode literalNode) {
        String lexeme = literalNode.literal().lexeme();
        return switch (literalNode.literal().type()) {
            case INTEGER_CONST -> parseIntegerLiteral(lexeme);
            case STRING_CONST -> new Value(lexeme);
            case DECIMAL_CONST -> new Value(new java.math.BigDecimal(lexeme));
            default -> throw new IllegalStateException("Unsupported literal type in expression.");
        };
    }

    private static Value parseIntegerLiteral(String lexeme) {
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

    private static boolean compareValues(Value val1, Value val2, String operator) {
        if (val1 == null || val1.getValue() == null || val2 == null || val2.getValue() == null) {
            return false;
        }
        if (val1.getValue() instanceof Number && val2.getValue() instanceof Number) {
            double v1 = ((Number) val1.getValue()).doubleValue();
            double v2 = ((Number) val2.getValue()).doubleValue();
            return switch (operator) {
                case "EQUAL" -> v1 == v2;
                case "NOT_EQUAL" -> v1 != v2;
                case "GREATER" -> v1 > v2;
                case "GREATER_EQUAL" -> v1 >= v2;
                case "LESS" -> v1 < v2;
                case "LESS_EQUAL" -> v1 <= v2;
                default -> false;
            };
        }
        if (val1.getType() != val2.getType()) {
            return false;
        }

        Comparable v1 = (Comparable) val1.getValue();
        Comparable v2 = (Comparable) val2.getValue();
        int cmp = v1.compareTo(v2);

        return switch (operator) {
            case "EQUAL" -> cmp == 0;
            case "NOT_EQUAL" -> cmp != 0;
            case "GREATER" -> cmp > 0;
            case "GREATER_EQUAL" -> cmp >= 0;
            case "LESS" -> cmp < 0;
            case "LESS_EQUAL" -> cmp <= 0;
            default -> throw new UnsupportedOperationException("Unsupported operator: " + operator);
        };
    }
}

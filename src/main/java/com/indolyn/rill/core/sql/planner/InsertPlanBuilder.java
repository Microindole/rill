package com.indolyn.rill.core.sql.planner;

import com.indolyn.rill.core.catalog.Catalog;
import com.indolyn.rill.core.catalog.TableInfo;
import com.indolyn.rill.core.model.Column;
import com.indolyn.rill.core.model.DataType;
import com.indolyn.rill.core.model.Schema;
import com.indolyn.rill.core.model.Tuple;
import com.indolyn.rill.core.model.Value;
import com.indolyn.rill.core.sql.ast.ExpressionNode;
import com.indolyn.rill.core.sql.ast.expression.LiteralNode;
import com.indolyn.rill.core.sql.ast.statement.InsertStatementNode;
import com.indolyn.rill.core.sql.lexer.TokenType;
import com.indolyn.rill.core.sql.planner.plan.PlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.InsertPlanNode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

class InsertPlanBuilder {

    private final Catalog catalog;

    InsertPlanBuilder(Catalog catalog) {
        this.catalog = catalog;
    }

    PlanNode build(InsertStatementNode ast) {
        TableInfo tableInfo = catalog.getTable(ast.tableName().getName());
        Schema schema = tableInfo.getSchema();
        List<Value> values = new ArrayList<>();

        for (int i = 0; i < ast.values().size(); i++) {
            ExpressionNode expr = ast.values().get(i);
            String colName = ast.columns().get(i).getName();
            Column column = schema.getColumn(colName);
            DataType expectedType = column.getType();

            if (expr instanceof LiteralNode literal) {
                values.add(convertLiteral(literal, expectedType));
            }
        }

        return new InsertPlanNode(tableInfo, List.of(new Tuple(values)));
    }

    private Value convertLiteral(LiteralNode literal, DataType expectedType) {
        String lexeme = literal.literal().lexeme();
        TokenType tokenType = literal.literal().type();

        return switch (expectedType) {
            case SMALLINT -> new Value(Short.parseShort(lexeme));
            case INT -> new Value(Integer.parseInt(lexeme));
            case BIGINT -> new Value(Long.parseLong(lexeme));
            case VARCHAR -> new Value(lexeme);
            case DECIMAL -> new Value(new BigDecimal(lexeme));
            case DATE -> new Value(LocalDate.parse(lexeme));
            case TIMESTAMP -> new Value(LocalDateTime.parse(lexeme.replace(" ", "T")));
            case BOOLEAN -> new Value(tokenType == TokenType.TRUE);
            case FLOAT -> new Value(Float.parseFloat(lexeme));
            case DOUBLE -> new Value(Double.parseDouble(lexeme));
            case CHAR -> new Value(DataType.CHAR, lexeme);
            default ->
                throw new IllegalStateException(
                    "Unsupported data type in planner for INSERT: " + expectedType);
        };
    }
}

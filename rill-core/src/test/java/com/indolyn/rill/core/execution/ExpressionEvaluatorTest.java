package com.indolyn.rill.core.execution;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.indolyn.rill.core.catalog.TableInfo;
import com.indolyn.rill.core.model.Column;
import com.indolyn.rill.core.model.DataType;
import com.indolyn.rill.core.model.Schema;
import com.indolyn.rill.core.model.Tuple;
import com.indolyn.rill.core.model.Value;
import com.indolyn.rill.core.sql.ast.expression.BinaryExpressionNode;
import com.indolyn.rill.core.sql.ast.expression.IdentifierNode;
import com.indolyn.rill.core.sql.ast.expression.LiteralNode;
import com.indolyn.rill.core.sql.lexer.Token;
import com.indolyn.rill.core.sql.lexer.TokenType;
import com.indolyn.rill.core.storage.page.PageId;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

class ExpressionEvaluatorTest {

    @Test
    void evaluatorShouldCompareNumericAndStringExpressionsAgainstTupleValues() {
        Schema schema =
            new Schema(List.of(new Column("id", DataType.INT), new Column("name", DataType.VARCHAR)));
        Tuple tuple = new Tuple(List.of(new Value(7), new Value("alice")));

        assertTrue(
            ExpressionEvaluator.evaluate(
                new BinaryExpressionNode(
                    new IdentifierNode("id"),
                    token(TokenType.GREATER_EQUAL, ">="),
                    new LiteralNode(token(TokenType.INTEGER_CONST, "7"))),
                schema,
                tuple));
        assertTrue(
            ExpressionEvaluator.evaluate(
                new BinaryExpressionNode(
                    new IdentifierNode("name"),
                    token(TokenType.EQUAL, "="),
                    new LiteralNode(token(TokenType.STRING_CONST, "alice"))),
                schema,
                tuple));
        assertFalse(
            ExpressionEvaluator.evaluate(
                new BinaryExpressionNode(
                    new IdentifierNode("name"),
                    token(TokenType.EQUAL, "="),
                    new LiteralNode(token(TokenType.STRING_CONST, "bob"))),
                schema,
                tuple));
    }

    @Test
    void evaluatorShouldResolveQualifiedJoinColumnsAgainstCombinedSchema() {
        Schema leftSchema = new Schema(List.of(new Column("id", DataType.INT), new Column("name", DataType.VARCHAR)));
        Schema rightSchema =
            new Schema(List.of(new Column("id", DataType.INT), new Column("user_id", DataType.INT)));
        Schema combinedSchema =
            new Schema(
                List.of(
                    new Column("id", DataType.INT),
                    new Column("name", DataType.VARCHAR),
                    new Column("id", DataType.INT),
                    new Column("user_id", DataType.INT)));
        Tuple joinedTuple = new Tuple(List.of(new Value(1), new Value("alice"), new Value(11), new Value(1)));
        TableInfo leftTable = new TableInfo("users", leftSchema, new PageId(4));
        TableInfo rightTable = new TableInfo("orders", rightSchema, new PageId(8));

        assertTrue(
            ExpressionEvaluator.evaluate(
                new BinaryExpressionNode(
                    new IdentifierNode("orders", "user_id"),
                    token(TokenType.EQUAL, "="),
                    new IdentifierNode("users", "id")),
                combinedSchema,
                joinedTuple,
                leftTable,
                rightTable));
    }

    @Test
    void evaluatorShouldHandleDecimalLiteralComparisons() {
        Schema schema = new Schema(List.of(new Column("amount", DataType.DECIMAL)));
        Tuple tuple = new Tuple(List.of(new Value(new BigDecimal("12.50"))));

        assertTrue(
            ExpressionEvaluator.evaluate(
                new BinaryExpressionNode(
                    new IdentifierNode("amount"),
                    token(TokenType.GREATER, ">"),
                    new LiteralNode(token(TokenType.DECIMAL_CONST, "10.25"))),
                schema,
                tuple));
    }

    @Test
    void evaluatorShouldRejectUnsupportedExpressionNodes() {
        Schema schema = new Schema(List.of(new Column("id", DataType.INT)));
        Tuple tuple = new Tuple(List.of(new Value(1)));

        assertThrows(UnsupportedOperationException.class, () -> ExpressionEvaluator.evaluate(new IdentifierNode("id"), schema, tuple));
    }

    private static Token token(TokenType type, String lexeme) {
        return new Token(type, lexeme, 1, 1);
    }
}

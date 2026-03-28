package com.indolyn.rill.core.execution;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.indolyn.rill.core.execution.predicate.AbstractPredicate;
import com.indolyn.rill.core.execution.predicate.ComparisonPredicate;
import com.indolyn.rill.core.execution.predicate.LogicalPredicate;
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

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

class PredicateFactoryTest {

    private final Schema schema =
        new Schema(List.of(new Column("id", DataType.INT), new Column("name", DataType.VARCHAR)));

    @Test
    void factoryShouldBuildComparisonPredicateForColumnLiteralExpression() throws Exception {
        PredicateFactory factory = new PredicateFactory();
        AbstractPredicate predicate =
            factory.create(
                new BinaryExpressionNode(
                    new IdentifierNode("id"),
                    token(TokenType.EQUAL, "="),
                    new LiteralNode(token(TokenType.INTEGER_CONST, "7"))),
                schema);

        assertInstanceOf(ComparisonPredicate.class, predicate);
        assertTrue(predicate.evaluate(new Tuple(List.of(new Value(7), new Value("alice")))));
        assertFalse(predicate.evaluate(new Tuple(List.of(new Value(8), new Value("alice")))));
    }

    @Test
    void factoryShouldBuildLogicalPredicateTree() throws Exception {
        PredicateFactory factory = new PredicateFactory();
        AbstractPredicate predicate =
            factory.create(
                new BinaryExpressionNode(
                    new BinaryExpressionNode(
                        new IdentifierNode("id"),
                        token(TokenType.GREATER_EQUAL, ">="),
                        new LiteralNode(token(TokenType.INTEGER_CONST, "5"))),
                    token(TokenType.AND, "AND"),
                    new BinaryExpressionNode(
                        new IdentifierNode("name"),
                        token(TokenType.EQUAL, "="),
                        new LiteralNode(token(TokenType.STRING_CONST, "alice")))),
                schema);

        assertInstanceOf(LogicalPredicate.class, predicate);
        assertTrue(predicate.evaluate(new Tuple(List.of(new Value(7), new Value("alice")))));
        assertFalse(predicate.evaluate(new Tuple(List.of(new Value(4), new Value("alice")))));
    }

    @Test
    void factoryShouldRejectUnsupportedExpressionShapes() {
        PredicateFactory factory = new PredicateFactory();

        UnsupportedOperationException unsupportedShape =
            assertThrows(
                UnsupportedOperationException.class,
                () ->
                    factory.create(
                        new BinaryExpressionNode(
                            new LiteralNode(token(TokenType.INTEGER_CONST, "1")),
                            token(TokenType.EQUAL, "="),
                            new IdentifierNode("id")),
                        schema));
        assertTrue(unsupportedShape.getMessage().contains("column_name op literal"));

        IllegalStateException missingColumn =
            assertThrows(
                IllegalStateException.class,
                () ->
                    factory.create(
                        new BinaryExpressionNode(
                            new IdentifierNode("missing_col"),
                            token(TokenType.EQUAL, "="),
                            new LiteralNode(token(TokenType.INTEGER_CONST, "1"))),
                        schema));
        assertTrue(missingColumn.getMessage().contains("missing_col"));
    }

    private static Token token(TokenType type, String lexeme) {
        return new Token(type, lexeme, 1, 1);
    }
}

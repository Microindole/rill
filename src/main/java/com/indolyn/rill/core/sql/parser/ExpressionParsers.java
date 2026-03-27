package com.indolyn.rill.core.sql.parser;

import com.indolyn.rill.core.exception.ParseException;
import com.indolyn.rill.core.sql.ast.ExpressionNode;
import com.indolyn.rill.core.sql.ast.expression.AggregateExpressionNode;
import com.indolyn.rill.core.sql.ast.expression.BinaryExpressionNode;
import com.indolyn.rill.core.sql.ast.expression.IdentifierNode;
import com.indolyn.rill.core.sql.ast.expression.LimitClauseNode;
import com.indolyn.rill.core.sql.ast.expression.LiteralNode;
import com.indolyn.rill.core.sql.ast.expression.OrderByClauseNode;
import com.indolyn.rill.core.sql.lexer.Token;
import com.indolyn.rill.core.sql.lexer.TokenType;

final class ExpressionParsers {

    private final Parser parser;

    ExpressionParsers(Parser parser) {
        this.parser = parser;
    }

    ExpressionNode parseExpression() {
        return parseOrExpression();
    }

    OrderByClauseNode parseOptionalOrderByClause() {
        if (!parser.match(TokenType.ORDER)) {
            return null;
        }
        parser.consume(TokenType.BY, "'BY' after 'ORDER'");
        ExpressionNode columnExpr = parsePrimaryExpression();
        if (!(columnExpr instanceof IdentifierNode identifierNode)) {
            throw new ParseException(parser.peek(), "Expected a column identifier for ORDER BY clause.");
        }
        boolean isAscending = true;
        if (parser.match(TokenType.ASC)) {
            isAscending = true;
        } else if (parser.match(TokenType.DESC)) {
            isAscending = false;
        }
        return new OrderByClauseNode(identifierNode, isAscending);
    }

    LimitClauseNode parseOptionalLimitClause() {
        if (!parser.match(TokenType.LIMIT)) {
            return null;
        }
        Token limitToken = parser.consume(TokenType.INTEGER_CONST, "integer value for LIMIT");
        try {
            return new LimitClauseNode(Integer.parseInt(limitToken.lexeme()));
        } catch (NumberFormatException e) {
            throw new ParseException(parser.peek(), "Invalid number for LIMIT: " + limitToken.lexeme());
        }
    }

    ExpressionNode parsePrimaryExpression() {
        if (parser.match(
            TokenType.INTEGER_CONST,
            TokenType.DECIMAL_CONST,
            TokenType.STRING_CONST,
            TokenType.TRUE,
            TokenType.FALSE)) {
            return new LiteralNode(parser.previous());
        }
        if (parser.isAggregateFunction(parser.peek().type())) {
            return parseAggregateExpression();
        }
        if (parser.check(TokenType.IDENTIFIER)) {
            Token firstIdentifier = parser.advance();
            if (parser.match(TokenType.DOT)) {
                Token secondIdentifier = parser.consume(TokenType.IDENTIFIER, "column name after '.'");
                return new IdentifierNode(firstIdentifier.lexeme(), secondIdentifier.lexeme());
            }
            return new IdentifierNode(firstIdentifier.lexeme());
        }
        if (parser.match(TokenType.LPAREN)) {
            ExpressionNode expr = parseExpression();
            parser.consume(TokenType.RPAREN, "')' after expression.");
            return expr;
        }
        throw new ParseException(
            parser.peek(), "an expression (a literal, an identifier, or an aggregate function)");
    }

    private ExpressionNode parseOrExpression() {
        ExpressionNode left = parseAndExpression();
        while (parser.match(TokenType.OR)) {
            Token operator = parser.previous();
            ExpressionNode right = parseAndExpression();
            left = new BinaryExpressionNode(left, operator, right);
        }
        return left;
    }

    private ExpressionNode parseAndExpression() {
        ExpressionNode left = parseComparison();
        while (parser.match(TokenType.AND)) {
            Token operator = parser.previous();
            ExpressionNode right = parseComparison();
            left = new BinaryExpressionNode(left, operator, right);
        }
        return left;
    }

    private ExpressionNode parseComparison() {
        ExpressionNode left = parsePrimaryExpression();
        if (parser.match(
            TokenType.GREATER,
            TokenType.GREATER_EQUAL,
            TokenType.LESS,
            TokenType.LESS_EQUAL,
            TokenType.EQUAL,
            TokenType.NOT_EQUAL)) {
            Token operator = parser.previous();
            ExpressionNode right = parsePrimaryExpression();
            left = new BinaryExpressionNode(left, operator, right);
        }
        return left;
    }

    private AggregateExpressionNode parseAggregateExpression() {
        Token functionToken = parser.advance();
        String functionName = functionToken.lexeme().toUpperCase();
        parser.consume(TokenType.LPAREN, "'(' after aggregate function name");
        boolean isStar = false;
        ExpressionNode argument = null;
        if (parser.match(TokenType.ASTERISK)) {
            isStar = true;
        } else {
            argument = parsePrimaryExpression();
        }
        parser.consume(TokenType.RPAREN, "')' after aggregate function argument");
        return new AggregateExpressionNode(functionName, argument, isStar);
    }
}

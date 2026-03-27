package com.indolyn.rill.core.sql.parser;

import com.indolyn.rill.core.exception.ParseException;
import com.indolyn.rill.core.sql.lexer.Token;
import com.indolyn.rill.core.sql.lexer.TokenType;
import com.indolyn.rill.core.sql.ast.*;
import com.indolyn.rill.core.sql.ast.statement.*;
import com.indolyn.rill.core.sql.ast.expression.ColumnDefinitionNode;
import com.indolyn.rill.core.sql.ast.expression.IdentifierNode;
import com.indolyn.rill.core.sql.ast.expression.LimitClauseNode;
import com.indolyn.rill.core.sql.ast.expression.OrderByClauseNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * &#064;description: 语法分析器 采用递归下降法，将Token流转换为抽象语法树(AST)
 */
public class Parser {

    private final List<Token> tokens;
    private int position = 0;
    private final Map<TokenType, StatementParser> statementParsers = new LinkedHashMap<>();
    private final ExpressionParsers expressionParsers;
    private final TypeDefinitionParsers typeDefinitionParsers;

    private static final Set<TokenType> AGGREGATE_FUNCTIONS =
        Set.of(TokenType.COUNT, TokenType.SUM, TokenType.AVG, TokenType.MIN, TokenType.MAX);

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.expressionParsers = new ExpressionParsers(this);
        this.typeDefinitionParsers = new TypeDefinitionParsers(this);
        ParserStatementRegistry.registerDefaultStatements(this, statementParsers);
    }

    public StatementNode parse() {
        if (peek().type() == TokenType.EOF) {
            return null;
        }

        StatementNode statement = parseStatement();

        if (check(TokenType.SEMICOLON)) {
            advance();
        } else if (!isAtEnd()) {
            throw new ParseException(peek(), "Expected ';' at the end of the statement");
        }
        return statement;
    }

    private StatementNode parseStatement() {
        StatementParser statementParser = statementParsers.get(peek().type());
        if (statementParser != null) {
            return statementParser.parse();
        }

        throw new ParseException(
            peek(), "a valid statement (CREATE, SELECT, INSERT, DELETE, DROP, etc.)");
    }

    ColumnDefinitionNode parseColumnDefinition() {
        return typeDefinitionParsers.parseColumnDefinition();
    }

    IdentifierNode parseTableName() {
        Token firstPart = consume(TokenType.IDENTIFIER, "database or table name");
        if (match(TokenType.DOT)) {
            Token tableNameToken = consume(TokenType.IDENTIFIER, "table name after '.'");
            return new IdentifierNode(tableNameToken.lexeme());
        } else {
            return new IdentifierNode(firstPart.lexeme());
        }
    }

    ExpressionNode parseExpression() {
        return expressionParsers.parseExpression();
    }

    OrderByClauseNode parseOptionalOrderByClause() {
        return expressionParsers.parseOptionalOrderByClause();
    }

    LimitClauseNode parseOptionalLimitClause() {
        return expressionParsers.parseOptionalLimitClause();
    }

    ExpressionNode parsePrimaryExpression() {
        return expressionParsers.parsePrimaryExpression();
    }

    boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw new ParseException(peek(), message);
    }

    boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type() == type;
    }

    Token advance() {
        if (!isAtEnd()) position++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type() == TokenType.EOF;
    }

    Token peek() {
        return tokens.get(position);
    }

    Token previous() {
        return tokens.get(position - 1);
    }

    boolean isAggregateFunction(TokenType tokenType) {
        return AGGREGATE_FUNCTIONS.contains(tokenType);
    }
}

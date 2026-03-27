package com.indolyn.rill.core.sql.parser;

import com.indolyn.rill.core.exception.ParseException;
import com.indolyn.rill.core.sql.ast.expression.ColumnDefinitionNode;
import com.indolyn.rill.core.sql.ast.expression.IdentifierNode;
import com.indolyn.rill.core.sql.ast.expression.LiteralNode;
import com.indolyn.rill.core.sql.ast.type.TypeReferenceNode;
import com.indolyn.rill.core.sql.lexer.Token;
import com.indolyn.rill.core.sql.lexer.TokenType;

import java.util.ArrayList;
import java.util.List;

final class TypeDefinitionParsers {

    private final Parser parser;

    TypeDefinitionParsers(Parser parser) {
        this.parser = parser;
    }

    ColumnDefinitionNode parseColumnDefinition() {
        Token columnNameToken = parser.consume(TokenType.IDENTIFIER, "column name");
        IdentifierNode columnName = new IdentifierNode(columnNameToken.lexeme());
        TypeReferenceNode dataType = parseTypeReference();
        boolean nullable = true;
        LiteralNode defaultValue = null;
        boolean primaryKey = false;

        while (true) {
            if (parser.match(TokenType.NOT)) {
                parser.consume(TokenType.NULL, "'NULL' after 'NOT'");
                nullable = false;
                continue;
            }
            if (parser.match(TokenType.NULL)) {
                nullable = true;
                continue;
            }
            if (parser.match(TokenType.DEFAULT)) {
                defaultValue = parseDefaultLiteral();
                continue;
            }
            if (parser.match(TokenType.PRIMARY)) {
                parser.consume(TokenType.KEY, "'KEY' after 'PRIMARY'");
                primaryKey = true;
                nullable = false;
                continue;
            }
            break;
        }

        return new ColumnDefinitionNode(columnName, dataType, nullable, defaultValue, primaryKey);
    }

    private LiteralNode parseDefaultLiteral() {
        if (parser.match(
            TokenType.INTEGER_CONST,
            TokenType.DECIMAL_CONST,
            TokenType.STRING_CONST,
            TokenType.TRUE,
            TokenType.FALSE,
            TokenType.NULL)) {
            return new LiteralNode(parser.previous());
        }
        throw new ParseException(parser.peek(), "a literal value after DEFAULT");
    }

    private TypeReferenceNode parseTypeReference() {
        List<String> nameParts = new ArrayList<>();
        Token current = parser.peek();
        if (!isTypeNameToken(current.type())) {
            throw new ParseException(parser.peek(), "a valid PostgreSQL data type name");
        }
        nameParts.add(parser.advance().lexeme());

        while (isTypeNameContinuation(parser.peek().type())) {
            nameParts.add(parser.advance().lexeme());
        }

        List<Integer> arguments = new ArrayList<>();
        if (parser.match(TokenType.LPAREN)) {
            arguments.add(Integer.parseInt(parser.consume(TokenType.INTEGER_CONST, "type argument").lexeme()));
            while (parser.match(TokenType.COMMA)) {
                arguments.add(Integer.parseInt(parser.consume(TokenType.INTEGER_CONST, "type argument").lexeme()));
            }
            parser.consume(TokenType.RPAREN, "Expected ')' after type arguments.");
        }
        return new TypeReferenceNode(nameParts, arguments);
    }

    private boolean isTypeNameToken(TokenType tokenType) {
        return tokenType == TokenType.INT
            || tokenType == TokenType.VARCHAR
            || tokenType == TokenType.DECIMAL
            || tokenType == TokenType.DATE
            || tokenType == TokenType.BOOLEAN
            || tokenType == TokenType.FLOAT
            || tokenType == TokenType.DOUBLE
            || tokenType == TokenType.CHAR
            || tokenType == TokenType.IDENTIFIER;
    }

    private boolean isTypeNameContinuation(TokenType tokenType) {
        return tokenType == TokenType.IDENTIFIER
            || tokenType == TokenType.DOUBLE
            || tokenType == TokenType.CHAR;
    }
}

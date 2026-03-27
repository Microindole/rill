package com.indolyn.rill.core.sql.parser;

import com.indolyn.rill.core.exception.ParseException;
import com.indolyn.rill.core.sql.ast.ExpressionNode;
import com.indolyn.rill.core.sql.ast.expression.IdentifierNode;
import com.indolyn.rill.core.sql.ast.expression.SetClauseNode;
import com.indolyn.rill.core.sql.ast.statement.DeleteStatementNode;
import com.indolyn.rill.core.sql.ast.statement.InsertStatementNode;
import com.indolyn.rill.core.sql.ast.statement.SelectStatementNode;
import com.indolyn.rill.core.sql.ast.statement.UpdateStatementNode;
import com.indolyn.rill.core.sql.lexer.Token;
import com.indolyn.rill.core.sql.lexer.TokenType;

import java.util.ArrayList;
import java.util.List;

final class QueryStatementParsers {

    private final Parser parser;

    QueryStatementParsers(Parser parser) {
        this.parser = parser;
    }

    SelectStatementNode parseSelectStatement() {
        List<ExpressionNode> selectList = new ArrayList<>();
        boolean isSelectAll = false;
        if (parser.match(TokenType.ASTERISK)) {
            isSelectAll = true;
        } else if (!parser.check(TokenType.FROM)) {
            do {
                selectList.add(parser.parseExpression());
            } while (parser.match(TokenType.COMMA));
        }
        parser.consume(TokenType.FROM, "'FROM' keyword");

        IdentifierNode fromTable = parser.parseTableName();

        IdentifierNode joinTable = null;
        ExpressionNode joinCondition = null;
        if (parser.match(TokenType.JOIN)) {
            joinTable = parser.parseTableName();
            parser.consume(TokenType.ON, "'ON' keyword after JOIN table");
            joinCondition = parser.parseExpression();
        }

        ExpressionNode whereClause = null;
        if (parser.match(TokenType.WHERE)) {
            whereClause = parser.parseExpression();
        }

        List<IdentifierNode> groupByClause = null;
        if (parser.match(TokenType.GROUP)) {
            parser.consume(TokenType.BY, "'BY' after 'GROUP'");
            groupByClause = new ArrayList<>();
            do {
                ExpressionNode groupByExpr = parser.parsePrimaryExpression();
                if (!(groupByExpr instanceof IdentifierNode identifierNode)) {
                    throw new ParseException(parser.peek(), "GROUP BY clause only supports column identifiers.");
                }
                groupByClause.add(identifierNode);
            } while (parser.match(TokenType.COMMA));
        }

        ExpressionNode havingClause = null;
        if (parser.match(TokenType.HAVING)) {
            havingClause = parser.parseExpression();
        }

        var orderByClause = parser.parseOptionalOrderByClause();
        var limitClause = parser.parseOptionalLimitClause();

        if (parser.peek().type() == TokenType.COMMA) {
            parser.match(TokenType.COMMA);
            parser.consume(TokenType.INTEGER_CONST, "integer value for LIMIT count");
        }

        return new SelectStatementNode(
            selectList,
            fromTable,
            joinTable,
            joinCondition,
            whereClause,
            isSelectAll,
            groupByClause,
            havingClause,
            orderByClause,
            limitClause);
    }

    InsertStatementNode parseInsertStatement() {
        parser.consume(TokenType.INTO, "'INTO' keyword after 'INSERT'");
        Token tableNameToken = parser.consume(TokenType.IDENTIFIER, "table name");
        IdentifierNode tableName = new IdentifierNode(tableNameToken.lexeme());
        parser.consume(TokenType.LPAREN, "'(' after table name");
        List<IdentifierNode> columns = new ArrayList<>();
        if (!parser.check(TokenType.RPAREN)) {
            do {
                Token colToken = parser.consume(TokenType.IDENTIFIER, "column name");
                columns.add(new IdentifierNode(colToken.lexeme()));
            } while (parser.match(TokenType.COMMA));
        }
        parser.consume(TokenType.RPAREN, "')' after column list");
        parser.consume(TokenType.VALUES, "'VALUES' keyword");
        parser.consume(TokenType.LPAREN, "'(' before value list");
        List<ExpressionNode> values = new ArrayList<>();
        if (!parser.check(TokenType.RPAREN)) {
            do {
                values.add(parser.parsePrimaryExpression());
            } while (parser.match(TokenType.COMMA));
        }
        parser.consume(TokenType.RPAREN, "')' after value list");
        return new InsertStatementNode(tableName, columns, values);
    }

    DeleteStatementNode parseDeleteStatement() {
        parser.consume(TokenType.FROM, "'FROM' keyword after 'DELETE'");
        Token tableNameToken = parser.consume(TokenType.IDENTIFIER, "table name");
        IdentifierNode tableName = new IdentifierNode(tableNameToken.lexeme());
        ExpressionNode whereClause = null;
        if (parser.match(TokenType.WHERE)) {
            whereClause = parser.parseExpression();
        }
        return new DeleteStatementNode(tableName, whereClause);
    }

    UpdateStatementNode parseUpdateStatement() {
        Token tableNameToken = parser.consume(TokenType.IDENTIFIER, "table name after UPDATE");
        IdentifierNode tableName = new IdentifierNode(tableNameToken.lexeme());
        parser.consume(TokenType.SET, "'SET' keyword");
        List<SetClauseNode> setClauses = new ArrayList<>();
        do {
            Token colToken = parser.consume(TokenType.IDENTIFIER, "column name in SET clause");
            IdentifierNode column = new IdentifierNode(colToken.lexeme());
            parser.consume(TokenType.EQUAL, "'=' after column name");
            ExpressionNode value = parser.parsePrimaryExpression();
            setClauses.add(new SetClauseNode(column, value));
        } while (parser.match(TokenType.COMMA));
        ExpressionNode whereClause = null;
        if (parser.match(TokenType.WHERE)) {
            whereClause = parser.parseExpression();
        }
        return new UpdateStatementNode(tableName, setClauses, whereClause);
    }
}

package com.indolyn.rill.core.sql.parser;

import com.indolyn.rill.core.sql.lexer.TokenType;

import java.util.Map;

final class ParserStatementRegistry {

    private ParserStatementRegistry() {
    }

    static void registerDefaultStatements(Parser parser, Map<TokenType, StatementParser> statementParsers) {
        DefinitionStatementParsers definitionParsers = new DefinitionStatementParsers(parser);
        QueryStatementParsers queryParsers = new QueryStatementParsers(parser);

        statementParsers.put(TokenType.CREATE, definitionParsers::parseCreateStatement);
        statementParsers.put(TokenType.GRANT, () -> {
            parser.consume(TokenType.GRANT, "'GRANT' keyword");
            return definitionParsers.parseGrantStatement();
        });
        statementParsers.put(TokenType.SHOW, () -> {
            parser.consume(TokenType.SHOW, "'SHOW' keyword");
            return definitionParsers.parseShowStatement();
        });
        statementParsers.put(TokenType.SELECT, () -> {
            parser.consume(TokenType.SELECT, "'SELECT' keyword");
            return queryParsers.parseSelectStatement();
        });
        statementParsers.put(TokenType.INSERT, () -> {
            parser.consume(TokenType.INSERT, "'INSERT' keyword");
            return queryParsers.parseInsertStatement();
        });
        statementParsers.put(TokenType.DELETE, () -> {
            parser.consume(TokenType.DELETE, "'DELETE' keyword");
            return queryParsers.parseDeleteStatement();
        });
        statementParsers.put(TokenType.UPDATE, () -> {
            parser.consume(TokenType.UPDATE, "'UPDATE' keyword");
            return queryParsers.parseUpdateStatement();
        });
        statementParsers.put(TokenType.DROP, () -> {
            parser.consume(TokenType.DROP, "'DROP' keyword");
            return definitionParsers.parseDropStatement();
        });
        statementParsers.put(TokenType.ALTER, () -> {
            parser.consume(TokenType.ALTER, "'ALTER' keyword");
            return definitionParsers.parseAlterTableStatement();
        });
        statementParsers.put(TokenType.USE, () -> {
            parser.consume(TokenType.USE, "'USE' keyword");
            return definitionParsers.parseUseDatabaseStatement();
        });
    }
}

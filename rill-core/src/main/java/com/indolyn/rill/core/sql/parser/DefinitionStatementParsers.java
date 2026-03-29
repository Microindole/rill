package com.indolyn.rill.core.sql.parser;

import com.indolyn.rill.core.exception.ParseException;
import com.indolyn.rill.core.sql.ast.StatementNode;
import com.indolyn.rill.core.sql.ast.expression.ColumnDefinitionNode;
import com.indolyn.rill.core.sql.ast.expression.IdentifierNode;
import com.indolyn.rill.core.sql.ast.expression.LiteralNode;
import com.indolyn.rill.core.sql.ast.statement.CreateDatabaseStatementNode;
import com.indolyn.rill.core.sql.ast.statement.CreateIndexStatementNode;
import com.indolyn.rill.core.sql.ast.statement.CreateTableStatementNode;
import com.indolyn.rill.core.sql.ast.statement.CreateUserStatementNode;
import com.indolyn.rill.core.sql.ast.statement.AlterTableStatementNode;
import com.indolyn.rill.core.sql.ast.statement.DropDatabaseStatementNode;
import com.indolyn.rill.core.sql.ast.statement.DropTableStatementNode;
import com.indolyn.rill.core.sql.ast.statement.GrantStatementNode;
import com.indolyn.rill.core.sql.ast.statement.ShowColumnsStatementNode;
import com.indolyn.rill.core.sql.ast.statement.ShowCreateTableStatementNode;
import com.indolyn.rill.core.sql.ast.statement.ShowDatabasesStatementNode;
import com.indolyn.rill.core.sql.ast.statement.ShowTablesStatementNode;
import com.indolyn.rill.core.sql.ast.statement.UseDatabaseStatementNode;
import com.indolyn.rill.core.sql.lexer.Token;
import com.indolyn.rill.core.sql.lexer.TokenType;

import java.util.ArrayList;
import java.util.List;

final class DefinitionStatementParsers {

    private final Parser parser;

    DefinitionStatementParsers(Parser parser) {
        this.parser = parser;
    }

    StatementNode parseCreateStatement() {
        parser.consume(TokenType.CREATE, "'CREATE' keyword");
        Token nextToken = parser.peek();
        if (nextToken.type() == TokenType.TABLE) {
            return parseCreateTableStatement();
        }
        if (nextToken.type() == TokenType.DATABASE) {
            return parseCreateDatabaseStatement();
        }
        if (nextToken.type() == TokenType.INDEX) {
            return parseCreateIndexStatement();
        }
        if (nextToken.type() == TokenType.USER) {
            return parseCreateUserStatement();
        }
        throw new ParseException(
            nextToken,
            "Expected 'TABLE', 'DATABASE', 'INDEX', or 'USER' after 'CREATE'");
    }

    StatementNode parseDropStatement() {
        if (parser.peek().type() == TokenType.DATABASE) {
            return parseDropDatabaseStatement();
        }
        return parseDropTableStatement();
    }

    StatementNode parseShowStatement() {
        if (parser.peek().type() == TokenType.DATABASES) {
            parser.consume(TokenType.DATABASES, "Expected 'DATABASES' after 'SHOW'");
            return new ShowDatabasesStatementNode();
        }

        parser.match(TokenType.FULL);

        if (parser.peek().type() == TokenType.COLUMNS) {
            parser.consume(TokenType.COLUMNS, "Expected 'COLUMNS' after 'SHOW'");
            parser.consume(TokenType.FROM, "Expected 'FROM' after 'COLUMNS'");
            IdentifierNode tableName = parser.parseTableName();
            return new ShowColumnsStatementNode(tableName);
        }

        if (parser.peek().type() == TokenType.TABLES) {
            parser.consume(TokenType.TABLES, "Expected 'TABLES' after 'SHOW'");
            if (parser.match(TokenType.FROM)) {
                parser.consume(TokenType.IDENTIFIER, "Expected database name after 'FROM'");
            }
            if (parser.match(TokenType.WHERE)) {
                while (parser.peek().type() != TokenType.SEMICOLON
                    && parser.peek().type() != TokenType.EOF) {
                    parser.advance();
                }
            }
            return new ShowTablesStatementNode();
        }
        if (parser.peek().type() == TokenType.CREATE) {
            parser.consume(TokenType.CREATE, "Expected 'CREATE' after 'SHOW'");
            parser.consume(TokenType.TABLE, "Expected 'TABLE' after 'CREATE'");
            IdentifierNode tableName = parser.parseTableName();
            return new ShowCreateTableStatementNode(tableName);
        }

        throw new ParseException(parser.peek(), "Expected DATABASES, COLUMNS, or TABLES after 'SHOW'");
    }

    StatementNode parseUseDatabaseStatement() {
        IdentifierNode dbName =
            new IdentifierNode(parser.consume(TokenType.IDENTIFIER, "database name").lexeme());
        return new UseDatabaseStatementNode(dbName);
    }

    StatementNode parseAlterTableStatement() {
        parser.consume(TokenType.TABLE, "'TABLE' keyword after 'ALTER'");
        IdentifierNode tableName =
            new IdentifierNode(parser.consume(TokenType.IDENTIFIER, "table name").lexeme());
        parser.consume(TokenType.ADD, "'ADD' keyword after table name");
        parser.match(TokenType.COLUMN);
        ColumnDefinitionNode newColumnDefinition = parser.parseColumnDefinition();
        return new AlterTableStatementNode(tableName, newColumnDefinition);
    }

    GrantStatementNode parseGrantStatement() {
        List<IdentifierNode> privileges = new ArrayList<>();
        do {
            Token privilegeToken = parser.peek();
            TokenType type = privilegeToken.type();

            if (type == TokenType.SELECT
                || type == TokenType.INSERT
                || type == TokenType.UPDATE
                || type == TokenType.DELETE
                || type == TokenType.IDENTIFIER) {
                parser.advance();
                privileges.add(new IdentifierNode(privilegeToken.lexeme()));
            } else {
                throw new ParseException(
                    privilegeToken, "a valid privilege type (e.g., SELECT, INSERT, ALL)");
            }
        } while (parser.match(TokenType.COMMA));

        parser.consume(TokenType.ON, "'ON' keyword");
        IdentifierNode tableName =
            new IdentifierNode(parser.consume(TokenType.IDENTIFIER, "table name").lexeme());
        parser.consume(TokenType.TO, "'TO' keyword");
        IdentifierNode username =
            new IdentifierNode(
                parser.consume(TokenType.STRING_CONST, "username as a string literal").lexeme());
        return new GrantStatementNode(privileges, tableName, username);
    }

    private StatementNode parseDropDatabaseStatement() {
        parser.consume(TokenType.DATABASE, "Expected 'DATABASE' after 'DROP'");
        IdentifierNode dbName =
            new IdentifierNode(parser.consume(TokenType.IDENTIFIER, "database name").lexeme());
        return new DropDatabaseStatementNode(dbName);
    }

    private StatementNode parseCreateDatabaseStatement() {
        parser.consume(TokenType.DATABASE, "Expected 'DATABASE' after 'CREATE'");
        Token dbNameToken = parser.consume(TokenType.IDENTIFIER, "database name");
        return new CreateDatabaseStatementNode(new IdentifierNode(dbNameToken.lexeme()));
    }

    private CreateUserStatementNode parseCreateUserStatement() {
        parser.consume(TokenType.USER, "'USER' keyword after 'CREATE'");
        IdentifierNode username =
            new IdentifierNode(
                parser.consume(TokenType.STRING_CONST, "username as a string literal").lexeme());
        parser.consume(TokenType.IDENTIFIED, "'IDENTIFIED' keyword");
        parser.consume(TokenType.BY, "'BY' keyword");
        LiteralNode password =
            new LiteralNode(parser.consume(TokenType.STRING_CONST, "password as a string literal"));
        return new CreateUserStatementNode(username, password);
    }

    private DropTableStatementNode parseDropTableStatement() {
        parser.consume(TokenType.TABLE, "'TABLE' keyword after 'DROP'");
        IdentifierNode tableName =
            new IdentifierNode(parser.consume(TokenType.IDENTIFIER, "table name").lexeme());
        return new DropTableStatementNode(tableName);
    }

    private CreateTableStatementNode parseCreateTableStatement() {
        parser.consume(TokenType.TABLE, "'TABLE' keyword after 'CREATE'");
        Token tableNameToken = parser.consume(TokenType.IDENTIFIER, "table name");
        IdentifierNode tableName = new IdentifierNode(tableNameToken.lexeme());

        parser.consume(TokenType.LPAREN, "'(' after table name");

        List<ColumnDefinitionNode> columns = new ArrayList<>();
        IdentifierNode primaryKeyColumn = null;
        if (!parser.check(TokenType.RPAREN)) {
            do {
                ColumnDefinitionNode colDef = parser.parseColumnDefinition();
                columns.add(colDef);
                if (colDef.primaryKey()) {
                    if (primaryKeyColumn != null) {
                        throw new ParseException(parser.peek(), "Only one primary key is allowed per table.");
                    }
                    primaryKeyColumn = colDef.columnName();
                }
            } while (parser.match(TokenType.COMMA));
        }

        parser.consume(TokenType.RPAREN, "')' after column definitions");
        return new CreateTableStatementNode(tableName, columns, primaryKeyColumn);
    }

    private CreateIndexStatementNode parseCreateIndexStatement() {
        parser.consume(TokenType.INDEX, "'INDEX' keyword after 'CREATE'");
        IdentifierNode indexName =
            new IdentifierNode(parser.consume(TokenType.IDENTIFIER, "index name").lexeme());
        parser.consume(TokenType.ON, "'ON' keyword after index name");
        IdentifierNode tableName =
            new IdentifierNode(parser.consume(TokenType.IDENTIFIER, "table name").lexeme());
        parser.consume(TokenType.LPAREN, "'(' before column name");
        List<IdentifierNode> columns = new ArrayList<>();
        columns.add(new IdentifierNode(parser.consume(TokenType.IDENTIFIER, "column name").lexeme()));
        while (parser.match(TokenType.COMMA)) {
            columns.add(new IdentifierNode(parser.consume(TokenType.IDENTIFIER, "column name").lexeme()));
        }
        parser.consume(TokenType.RPAREN, "')' after column name(s)");
        return new CreateIndexStatementNode(indexName, tableName, columns);
    }
}

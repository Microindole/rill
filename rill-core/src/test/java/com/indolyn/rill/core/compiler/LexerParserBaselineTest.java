package com.indolyn.rill.core.compiler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.indolyn.rill.core.sql.ast.StatementNode;
import com.indolyn.rill.core.sql.ast.statement.AlterTableStatementNode;
import com.indolyn.rill.core.sql.ast.statement.CreateTableStatementNode;
import com.indolyn.rill.core.sql.ast.statement.SelectStatementNode;
import com.indolyn.rill.core.sql.lexer.Lexer;
import com.indolyn.rill.core.sql.lexer.Token;
import com.indolyn.rill.core.sql.lexer.TokenType;
import com.indolyn.rill.core.sql.parser.Parser;

import java.util.List;

import org.junit.jupiter.api.Test;

class LexerParserBaselineTest {

    @Test
    void lexerShouldTokenizePostgreSqlStyleTypeAliasesAndKeywords() {
        List<Token> tokens =
            new Lexer("create table users (id integer, name character varying(20));").tokenize();

        assertEquals(TokenType.CREATE, tokens.get(0).type());
        assertEquals(TokenType.TABLE, tokens.get(1).type());
        assertTrue(tokens.stream().anyMatch(token -> token.type() == TokenType.IDENTIFIER && token.lexeme().equalsIgnoreCase("users")));
        assertTrue(tokens.stream().anyMatch(token -> token.type() == TokenType.IDENTIFIER && token.lexeme().equalsIgnoreCase("varying")));
        assertEquals(TokenType.EOF, tokens.get(tokens.size() - 1).type());
    }

    @Test
    void parserShouldBuildCreateTableAstForBaselineSchema() {
        StatementNode statement =
            new Parser(new Lexer("create table users (id int primary key, name varchar(20));").tokenize()).parse();

        CreateTableStatementNode createTable = assertInstanceOf(CreateTableStatementNode.class, statement);
        assertEquals("users", createTable.tableName().getName());
        assertEquals(2, createTable.columns().size());
        assertEquals("id", createTable.primaryKeyColumn().getName());
    }

    @Test
    void parserShouldBuildSelectAstWithWhereClause() {
        StatementNode statement =
            new Parser(new Lexer("select * from users where id = 1;").tokenize()).parse();

        SelectStatementNode select = assertInstanceOf(SelectStatementNode.class, statement);
        assertEquals("users", select.fromTable().getName());
        assertTrue(select.isSelectAll());
        assertTrue(select.whereClause() != null);
    }

    @Test
    void parserShouldBuildAlterTableAstForAddColumn() {
        StatementNode statement =
            new Parser(new Lexer("alter table users add column email varchar(50);").tokenize()).parse();

        AlterTableStatementNode alterTable = assertInstanceOf(AlterTableStatementNode.class, statement);
        assertEquals("users", alterTable.tableName().getName());
        assertEquals("email", alterTable.newColumnDefinition().columnName().getName());
        assertEquals("varchar", alterTable.newColumnDefinition().dataType().displayName());
    }
}

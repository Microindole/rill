package com.indolyn.rill.core.sql.lexer;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PostgreSqlKeywordRegistry implements KeywordRegistry {

    private final Map<String, TokenType> keywords = new HashMap<>();

    public PostgreSqlKeywordRegistry() {
        register("select", TokenType.SELECT);
        register("from", TokenType.FROM);
        register("where", TokenType.WHERE);
        register("create", TokenType.CREATE);
        register("table", TokenType.TABLE);
        register("primary", TokenType.PRIMARY);
        register("key", TokenType.KEY);
        register("not", TokenType.NOT);
        register("null", TokenType.NULL);
        register("default", TokenType.DEFAULT);
        register("database", TokenType.DATABASE);
        register("databases", TokenType.DATABASES);
        register("index", TokenType.INDEX);
        register("insert", TokenType.INSERT);
        register("into", TokenType.INTO);
        register("values", TokenType.VALUES);
        register("use", TokenType.USE);
        register("delete", TokenType.DELETE);
        register("update", TokenType.UPDATE);
        register("set", TokenType.SET);
        register("int", TokenType.INT);
        register("varchar", TokenType.VARCHAR);
        register("decimal", TokenType.DECIMAL);
        register("date", TokenType.DATE);
        register("boolean", TokenType.BOOLEAN);
        register("float", TokenType.FLOAT);
        register("double", TokenType.DOUBLE);
        register("char", TokenType.CHAR);
        register("true", TokenType.TRUE);
        register("false", TokenType.FALSE);
        register("order", TokenType.ORDER);
        register("by", TokenType.BY);
        register("asc", TokenType.ASC);
        register("desc", TokenType.DESC);
        register("limit", TokenType.LIMIT);
        register("and", TokenType.AND);
        register("or", TokenType.OR);
        register("join", TokenType.JOIN);
        register("on", TokenType.ON);
        register("drop", TokenType.DROP);
        register("alter", TokenType.ALTER);
        register("add", TokenType.ADD);
        register("column", TokenType.COLUMN);
        register("columns", TokenType.COLUMNS);
        register("group", TokenType.GROUP);
        register("count", TokenType.COUNT);
        register("sum", TokenType.SUM);
        register("avg", TokenType.AVG);
        register("min", TokenType.MIN);
        register("max", TokenType.MAX);
        register("show", TokenType.SHOW);
        register("tables", TokenType.TABLES);
        register("full", TokenType.FULL);
        register("user", TokenType.USER);
        register("identified", TokenType.IDENTIFIED);
        register("grant", TokenType.GRANT);
        register("to", TokenType.TO);
        register("having", TokenType.HAVING);
    }

    @Override
    public TokenType resolve(String text) {
        return keywords.getOrDefault(text.toLowerCase(Locale.ROOT), TokenType.IDENTIFIER);
    }

    private void register(String keyword, TokenType tokenType) {
        keywords.put(keyword, tokenType);
    }
}

package com.indolyn.rill.core.sql.lexer;

public interface KeywordRegistry {

    TokenType resolve(String text);
}

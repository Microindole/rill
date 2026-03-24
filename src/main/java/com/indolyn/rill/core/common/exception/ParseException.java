package com.indolyn.rill.core.common.exception;

import com.indolyn.rill.core.compiler.lexer.Token;

/**
 * @author hidyouth
 */
public class ParseException extends RuntimeException {

    public ParseException(String message) {
        super(message);
    }

    public ParseException(Token token, String expected) {
        super(
            String.format(
                "Syntax Error at line %d, column %d: Expected %s, but found '%s' (%s)",
                token.line(), token.column(), expected, token.lexeme(), token.type()));
    }
}

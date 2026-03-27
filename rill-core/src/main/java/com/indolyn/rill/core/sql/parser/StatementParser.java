package com.indolyn.rill.core.sql.parser;

import com.indolyn.rill.core.sql.ast.StatementNode;

@FunctionalInterface
interface StatementParser {
    StatementNode parse();
}

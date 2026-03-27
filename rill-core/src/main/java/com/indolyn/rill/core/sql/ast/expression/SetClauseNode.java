package com.indolyn.rill.core.sql.ast.expression;

import com.indolyn.rill.core.sql.ast.AstNode;
import com.indolyn.rill.core.sql.ast.ExpressionNode;

public record SetClauseNode(IdentifierNode column, ExpressionNode value) implements AstNode {
}


package com.indolyn.rill.core.compiler.parser.ast.expression;

import com.indolyn.rill.core.compiler.parser.ast.AstNode;
import com.indolyn.rill.core.compiler.parser.ast.ExpressionNode;

public record SetClauseNode(IdentifierNode column, ExpressionNode value) implements AstNode {}

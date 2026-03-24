package com.indolyn.rill.core.sql.ast.expression;

import com.indolyn.rill.core.sql.lexer.Token;
import com.indolyn.rill.core.sql.ast.ExpressionNode;

/**
 * AST 节点: 表示一个二元运算表达式 (e.g., age > 20)
 */
public record BinaryExpressionNode(ExpressionNode left, Token operator, ExpressionNode right)
    implements ExpressionNode {
}


package com.indolyn.rill.core.compiler.parser.ast.expression;

import com.indolyn.rill.core.compiler.lexer.Token;
import com.indolyn.rill.core.compiler.parser.ast.ExpressionNode;

/**
 * AST 节点: 表示一个二元运算表达式 (e.g., age > 20)
 */
public record BinaryExpressionNode(
        ExpressionNode left,
        Token operator,
        ExpressionNode right
) implements ExpressionNode {
}




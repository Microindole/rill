package com.indolyn.rill.core.compiler.parser.ast.expression;

import com.indolyn.rill.core.compiler.lexer.Token;
import com.indolyn.rill.core.compiler.parser.ast.ExpressionNode;

/**
 * AST 节点: 表示一个字面量 (如数字、字符串)
 */
public record LiteralNode(Token literal) implements ExpressionNode {
}




package com.indolyn.rill.core.sql.ast.expression;

import com.indolyn.rill.core.sql.ast.AstNode;

/**
 * AST 节点: 表示 LIMIT 子句
 *
 * @param limit 限制返回的行数
 */
public record LimitClauseNode(int limit) implements AstNode {
}


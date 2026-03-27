package com.indolyn.rill.core.sql.ast.statement;

import com.indolyn.rill.core.sql.ast.ExpressionNode;
import com.indolyn.rill.core.sql.ast.StatementNode;
import com.indolyn.rill.core.sql.ast.expression.IdentifierNode;

/**
 * AST 节点: 表示 DELETE 语句
 */
public record DeleteStatementNode(IdentifierNode tableName, ExpressionNode whereClause)
    implements StatementNode {
}


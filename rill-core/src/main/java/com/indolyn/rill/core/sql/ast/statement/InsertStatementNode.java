package com.indolyn.rill.core.sql.ast.statement;

import com.indolyn.rill.core.sql.ast.ExpressionNode;
import com.indolyn.rill.core.sql.ast.StatementNode;
import com.indolyn.rill.core.sql.ast.expression.IdentifierNode;

import java.util.List;

/**
 * AST 节点: 表示 INSERT 语句
 */
public record InsertStatementNode(
    IdentifierNode tableName, List<IdentifierNode> columns, List<ExpressionNode> values)
    implements StatementNode {
}


package com.indolyn.rill.core.compiler.parser.ast.dml;

import com.indolyn.rill.core.compiler.parser.ast.ExpressionNode;
import com.indolyn.rill.core.compiler.parser.ast.expression.IdentifierNode;
import com.indolyn.rill.core.compiler.parser.ast.StatementNode;

/**
 * AST 节点: 表示 DELETE 语句
 */
public record DeleteStatementNode(
        IdentifierNode tableName,
        ExpressionNode whereClause
) implements StatementNode {
}




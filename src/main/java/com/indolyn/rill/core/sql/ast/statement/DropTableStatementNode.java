package com.indolyn.rill.core.sql.ast.statement;

import com.indolyn.rill.core.sql.ast.StatementNode;
import com.indolyn.rill.core.sql.ast.expression.IdentifierNode;

/**
 * AST 节点: 表示 DROP TABLE 语句
 *
 * @param tableName 要删除的表名
 */
public record DropTableStatementNode(IdentifierNode tableName) implements StatementNode {
}


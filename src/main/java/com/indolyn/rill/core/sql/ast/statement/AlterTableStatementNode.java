package com.indolyn.rill.core.sql.ast.statement;

import com.indolyn.rill.core.sql.ast.StatementNode;
import com.indolyn.rill.core.sql.ast.expression.ColumnDefinitionNode;
import com.indolyn.rill.core.sql.ast.expression.IdentifierNode;

/**
 * AST 节点: 表示 ALTER TABLE 语句 (目前仅支持 ADD COLUMN)
 *
 * @param tableName           要修改的表名
 * @param newColumnDefinition 要追加的新列定义
 */
public record AlterTableStatementNode(
    IdentifierNode tableName, ColumnDefinitionNode newColumnDefinition) implements StatementNode {
}


package com.indolyn.rill.core.sql.ast.statement;

import com.indolyn.rill.core.sql.ast.StatementNode;
import com.indolyn.rill.core.sql.ast.expression.IdentifierNode;

/**
 * AST 节点: 表示 SHOW COLUMNS 语句 e.g., SHOW COLUMNS FROM my_table;
 */
public record ShowColumnsStatementNode(IdentifierNode tableName) implements StatementNode {
}


package com.indolyn.rill.core.sql.ast.statement;

import com.indolyn.rill.core.sql.ast.StatementNode;
import com.indolyn.rill.core.sql.ast.expression.IdentifierNode;

/**
 * AST 节点: 表示 USE database 语句
 */
public record UseDatabaseStatementNode(IdentifierNode databaseName) implements StatementNode {
}


package com.indolyn.rill.core.compiler.parser.ast.misc;

import com.indolyn.rill.core.compiler.parser.ast.StatementNode;
import com.indolyn.rill.core.compiler.parser.ast.expression.IdentifierNode;

/**
 * AST 节点: 表示 USE database 语句
 */
public record UseDatabaseStatementNode(IdentifierNode databaseName) implements StatementNode {
}

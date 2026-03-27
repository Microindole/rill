package com.indolyn.rill.core.sql.ast.statement;

import com.indolyn.rill.core.sql.ast.StatementNode;
import com.indolyn.rill.core.sql.ast.expression.IdentifierNode;
import com.indolyn.rill.core.sql.ast.expression.LiteralNode;

/**
 * AST 节点: 表示 CREATE USER 语句 e.g., CREATE USER 'new_user' IDENTIFIED BY 'password123';
 */
public record CreateUserStatementNode(IdentifierNode username, LiteralNode password)
    implements StatementNode {
}


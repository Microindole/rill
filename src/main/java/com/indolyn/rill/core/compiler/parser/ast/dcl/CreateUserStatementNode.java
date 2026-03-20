package com.indolyn.rill.core.compiler.parser.ast.dcl;

import com.indolyn.rill.core.compiler.parser.ast.expression.IdentifierNode;
import com.indolyn.rill.core.compiler.parser.ast.expression.LiteralNode;
import com.indolyn.rill.core.compiler.parser.ast.StatementNode;

/**
 * AST 节点: 表示 CREATE USER 语句
 * e.g., CREATE USER 'new_user' IDENTIFIED BY 'password123';
 */
public record CreateUserStatementNode(
        IdentifierNode username,
        LiteralNode password
) implements StatementNode {
}


package com.indolyn.rill.core.compiler.parser.ast.dcl;

import com.indolyn.rill.core.compiler.parser.ast.StatementNode;
import com.indolyn.rill.core.compiler.parser.ast.expression.IdentifierNode;

import java.util.List;

/**
 * AST 节点: 表示 GRANT 语句 e.g., GRANT SELECT, INSERT ON my_table TO 'some_user';
 */
public record GrantStatementNode(
    List<IdentifierNode> privileges, IdentifierNode tableName, IdentifierNode username)
    implements StatementNode {
}

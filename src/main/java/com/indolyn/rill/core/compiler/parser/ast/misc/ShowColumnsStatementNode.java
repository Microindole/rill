package com.indolyn.rill.core.compiler.parser.ast.misc;

import com.indolyn.rill.core.compiler.parser.ast.expression.IdentifierNode;
import com.indolyn.rill.core.compiler.parser.ast.StatementNode;

/**
 * AST 节点: 表示 SHOW COLUMNS 语句
 * e.g., SHOW COLUMNS FROM my_table;
 */
public record ShowColumnsStatementNode(
        IdentifierNode tableName
) implements StatementNode {
}


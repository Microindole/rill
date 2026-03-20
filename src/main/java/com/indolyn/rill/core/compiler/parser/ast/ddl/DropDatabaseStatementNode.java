package com.indolyn.rill.core.compiler.parser.ast.ddl;

import com.indolyn.rill.core.compiler.parser.ast.expression.IdentifierNode;
import com.indolyn.rill.core.compiler.parser.ast.StatementNode;

/**
 * AST 节点: 表示 DROP DATABASE 语句
 * e.g., DROP DATABASE my_database;
 */
public record DropDatabaseStatementNode(
        IdentifierNode databaseName
) implements StatementNode {
}


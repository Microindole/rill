package com.indolyn.rill.core.sql.ast.statement;

import com.indolyn.rill.core.sql.ast.StatementNode;
import com.indolyn.rill.core.sql.ast.expression.ColumnDefinitionNode;
import com.indolyn.rill.core.sql.ast.expression.IdentifierNode;

import java.util.List;

/**
 * @author hidyouth
 * @description: 表示一个 CREATE TABLE 语句
 */
public record CreateTableStatementNode(
    IdentifierNode tableName, List<ColumnDefinitionNode> columns, IdentifierNode primaryKeyColumn)
    implements StatementNode {
}


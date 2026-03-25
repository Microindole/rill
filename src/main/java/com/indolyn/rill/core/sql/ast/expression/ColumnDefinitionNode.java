package com.indolyn.rill.core.sql.ast.expression;

import com.indolyn.rill.core.sql.ast.AstNode;

/**
 * @author microindole
 * @description: 用于 CREATE TABLE 语句中的列定义
 */
public record ColumnDefinitionNode(IdentifierNode columnName, IdentifierNode dataType)
    implements AstNode {
}


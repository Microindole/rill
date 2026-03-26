package com.indolyn.rill.core.sql.ast.expression;

import com.indolyn.rill.core.sql.ast.AstNode;
import com.indolyn.rill.core.sql.ast.expression.LiteralNode;
import com.indolyn.rill.core.sql.ast.type.TypeReferenceNode;

/**
 * @author microindole
 * @description: 用于 CREATE TABLE 语句中的列定义
 */
public record ColumnDefinitionNode(
    IdentifierNode columnName,
    TypeReferenceNode dataType,
    boolean nullable,
    LiteralNode defaultValue,
    boolean primaryKey)
    implements AstNode {
}

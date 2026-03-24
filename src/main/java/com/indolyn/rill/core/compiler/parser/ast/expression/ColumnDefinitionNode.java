package com.indolyn.rill.core.compiler.parser.ast.expression;

import com.indolyn.rill.core.compiler.parser.ast.AstNode;

/**
 * @author hdiyouth
 * @description: 用于 CREATE TABLE 语句中的列定义
 */
public record ColumnDefinitionNode(IdentifierNode columnName, IdentifierNode dataType)
    implements AstNode {
}

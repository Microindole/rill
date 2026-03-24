package com.indolyn.rill.core.compiler.parser.ast.ddl;

import com.indolyn.rill.core.compiler.parser.ast.StatementNode;
import com.indolyn.rill.core.compiler.parser.ast.expression.ColumnDefinitionNode;
import com.indolyn.rill.core.compiler.parser.ast.expression.IdentifierNode;
import java.util.List;

/**
 * @author hidyouth
 * @description: 表示一个 CREATE TABLE 语句
 */
public record CreateTableStatementNode(
    IdentifierNode tableName, List<ColumnDefinitionNode> columns, IdentifierNode primaryKeyColumn)
    implements StatementNode {}

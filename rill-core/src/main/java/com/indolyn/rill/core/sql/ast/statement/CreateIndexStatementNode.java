package com.indolyn.rill.core.sql.ast.statement;

import com.indolyn.rill.core.sql.ast.StatementNode;
import com.indolyn.rill.core.sql.ast.expression.IdentifierNode;

import java.util.List;

public class CreateIndexStatementNode implements StatementNode {
    private final IdentifierNode indexName;
    private final IdentifierNode tableName;
    private final List<IdentifierNode> columnNames;

    public CreateIndexStatementNode(
        IdentifierNode indexName, IdentifierNode tableName, List<IdentifierNode> columnNames) {
        this.indexName = indexName;
        this.tableName = tableName;
        this.columnNames = List.copyOf(columnNames);
    }

    public IdentifierNode getIndexName() {
        return indexName;
    }

    public IdentifierNode getTableName() {
        return tableName;
    }

    public List<IdentifierNode> getColumnNames() {
        return columnNames;
    }
}

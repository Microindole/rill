package com.indolyn.rill.core.sql.planner;

import com.indolyn.rill.core.catalog.Catalog;
import com.indolyn.rill.core.catalog.TableInfo;
import com.indolyn.rill.core.sql.ast.statement.CreateIndexStatementNode;
import com.indolyn.rill.core.sql.planner.plan.PlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.CreateIndexPlanNode;

class CreateIndexPlanBuilder {

    private final Catalog catalog;

    CreateIndexPlanBuilder(Catalog catalog) {
        this.catalog = catalog;
    }

    PlanNode build(CreateIndexStatementNode ast) {
        String indexName = ast.getIndexName().getName();
        String tableName = ast.getTableName().getName();
        String columnName = ast.getColumnNames().get(0).getName();

        TableInfo tableInfo = catalog.getTable(tableName);
        if (tableInfo == null) {
            throw new IllegalStateException("Table '" + tableName + "' not found for index creation.");
        }
        return new CreateIndexPlanNode(indexName, tableName, columnName, tableInfo);
    }
}

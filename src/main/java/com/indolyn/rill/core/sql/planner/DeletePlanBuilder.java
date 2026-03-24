package com.indolyn.rill.core.sql.planner;

import com.indolyn.rill.core.catalog.Catalog;
import com.indolyn.rill.core.catalog.TableInfo;
import com.indolyn.rill.core.sql.ast.statement.DeleteStatementNode;
import com.indolyn.rill.core.sql.planner.plan.PlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.DeletePlanNode;
import com.indolyn.rill.core.sql.planner.plan.query.FilterPlanNode;
import com.indolyn.rill.core.sql.planner.plan.query.SeqScanPlanNode;

class DeletePlanBuilder {

    private final Catalog catalog;

    DeletePlanBuilder(Catalog catalog) {
        this.catalog = catalog;
    }

    PlanNode build(DeleteStatementNode ast) {
        TableInfo tableInfo = catalog.getTable(ast.tableName().getName());
        PlanNode childPlan = new SeqScanPlanNode(tableInfo, ast.whereClause());
        if (ast.whereClause() != null) {
            childPlan = new FilterPlanNode(childPlan, ast.whereClause());
        }
        return new DeletePlanNode(childPlan, tableInfo);
    }
}

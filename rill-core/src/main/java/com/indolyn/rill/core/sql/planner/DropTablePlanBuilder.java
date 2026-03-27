package com.indolyn.rill.core.sql.planner;

import com.indolyn.rill.core.sql.ast.statement.DropTableStatementNode;
import com.indolyn.rill.core.sql.planner.plan.PlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.DropTablePlanNode;

class DropTablePlanBuilder {

    PlanNode build(DropTableStatementNode ast) {
        return new DropTablePlanNode(ast.tableName().getName());
    }
}

package com.indolyn.rill.core.sql.planner;

import com.indolyn.rill.core.sql.ast.statement.GrantStatementNode;
import com.indolyn.rill.core.sql.planner.plan.PlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.GrantPlanNode;

class GrantPlanBuilder {

    PlanNode build(GrantStatementNode ast) {
        return new GrantPlanNode(ast.privileges(), ast.tableName(), ast.username());
    }
}

package com.indolyn.rill.core.sql.planner;

import com.indolyn.rill.core.sql.ast.statement.CreateUserStatementNode;
import com.indolyn.rill.core.sql.planner.plan.PlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.CreateUserPlanNode;

class CreateUserPlanBuilder {

    PlanNode build(CreateUserStatementNode ast) {
        return new CreateUserPlanNode(ast.username(), ast.password());
    }
}

package com.indolyn.rill.core.execution;

import com.indolyn.rill.core.sql.ast.StatementNode;
import com.indolyn.rill.core.sql.planner.plan.PlanNode;

record CompiledStatement(StatementNode ast, PlanNode plan) {
}

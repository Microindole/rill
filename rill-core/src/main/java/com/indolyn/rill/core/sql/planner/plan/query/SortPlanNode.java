package com.indolyn.rill.core.sql.planner.plan.query;

import com.indolyn.rill.core.sql.ast.expression.OrderByClauseNode;
import com.indolyn.rill.core.sql.planner.plan.PlanNode;

/**
 * 排序操作的执行计划节点
 */
public class SortPlanNode extends PlanNode {
    private final PlanNode child;
    private final OrderByClauseNode orderBy;

    public SortPlanNode(PlanNode child, OrderByClauseNode orderBy) {
        super(child.getOutputSchema()); // 排序不改变 Schema
        this.child = child;
        this.orderBy = orderBy;
    }

    public PlanNode getChild() {
        return child;
    }

    public OrderByClauseNode getOrderBy() {
        return orderBy;
    }
}


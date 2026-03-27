package com.indolyn.rill.core.sql.planner.plan.command;

import com.indolyn.rill.core.sql.planner.plan.PlanNode;

/**
 * 删除表的执行计划节点
 */
public class DropTablePlanNode extends PlanNode {
    private final String tableName;

    public DropTablePlanNode(String tableName) {
        super(null);
        this.tableName = tableName;
    }

    public String getTableName() {
        return tableName;
    }
}

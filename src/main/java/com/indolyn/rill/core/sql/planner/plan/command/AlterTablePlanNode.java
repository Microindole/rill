package com.indolyn.rill.core.sql.planner.plan.command;

import com.indolyn.rill.core.model.Column;
import com.indolyn.rill.core.sql.planner.plan.PlanNode;

/**
 * 修改表的执行计划节点
 */
public class AlterTablePlanNode extends PlanNode {
    private final String tableName;
    private final Column newColumn;

    public AlterTablePlanNode(String tableName, Column newColumn) {
        super(null);
        this.tableName = tableName;
        this.newColumn = newColumn;
    }

    public String getTableName() {
        return tableName;
    }

    public Column getNewColumn() {
        return newColumn;
    }
}

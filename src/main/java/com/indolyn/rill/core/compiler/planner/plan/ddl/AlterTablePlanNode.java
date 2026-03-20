package com.indolyn.rill.core.compiler.planner.plan.ddl;

import com.indolyn.rill.core.common.model.Column;
import com.indolyn.rill.core.compiler.planner.plan.PlanNode;

/**
 * 修改表的执行计划节点
 */
public class AlterTablePlanNode extends PlanNode {
    private final String tableName;
    private final Column newColumn;

    public AlterTablePlanNode(String tableName, Column newColumn) {
        super(null); // DDL 不向上层返回元组
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



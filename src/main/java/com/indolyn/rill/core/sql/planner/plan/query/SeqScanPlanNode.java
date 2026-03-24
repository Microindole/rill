package com.indolyn.rill.core.sql.planner.plan.query;

import com.indolyn.rill.core.catalog.TableInfo;
import com.indolyn.rill.core.sql.ast.ExpressionNode;
import com.indolyn.rill.core.sql.planner.plan.PlanNode;

/**
 * @author hidyouth
 * @description: 顺序扫描执行计划节点
 */
public class SeqScanPlanNode extends PlanNode {
    private final TableInfo tableInfo;
    private final ExpressionNode predicate;

    public SeqScanPlanNode(TableInfo tableInfo, ExpressionNode predicate) {
        super(tableInfo.getSchema());
        this.tableInfo = tableInfo;
        this.predicate = predicate;
    }

    public TableInfo getTableInfo() {
        return tableInfo;
    }

    public ExpressionNode getPredicate() {
        return predicate;
    }
}


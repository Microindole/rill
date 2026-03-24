package com.indolyn.rill.core.sql.planner.plan.command;

import com.indolyn.rill.core.catalog.TableInfo;
import com.indolyn.rill.core.model.Tuple;
import com.indolyn.rill.core.sql.planner.plan.PlanNode;

import java.util.List;

/**
 * @author hidyouth
 * @description: 插入操作的执行计划节点
 */
public class InsertPlanNode extends PlanNode {
    private final TableInfo tableInfo;
    private final List<Tuple> rawTuples;

    public InsertPlanNode(TableInfo tableInfo, List<Tuple> rawTuples) {
        super(null);
        this.tableInfo = tableInfo;
        this.rawTuples = rawTuples;
    }

    public TableInfo getTableInfo() {
        return tableInfo;
    }

    public List<Tuple> getRawTuples() {
        return rawTuples;
    }
}

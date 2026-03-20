package com.indolyn.rill.core.compiler.planner.plan.dml;

import com.indolyn.rill.core.catalog.TableInfo;
import com.indolyn.rill.core.compiler.planner.plan.PlanNode;

public class DeletePlanNode extends PlanNode {
    private final PlanNode child;
    private final TableInfo tableInfo;

    public DeletePlanNode(PlanNode child, TableInfo tableInfo) {
        super(null); // DELETE 不向上层返回元组
        this.child = child;
        this.tableInfo = tableInfo;
    }

    public PlanNode getChild() {
        return child;
    }
    
    public TableInfo getTableInfo(){
        return tableInfo;
    }
}


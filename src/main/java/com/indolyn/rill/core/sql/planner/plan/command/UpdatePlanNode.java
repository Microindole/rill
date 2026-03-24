package com.indolyn.rill.core.sql.planner.plan.command;

import com.indolyn.rill.core.catalog.TableInfo;
import com.indolyn.rill.core.sql.ast.expression.SetClauseNode;
import com.indolyn.rill.core.sql.planner.plan.PlanNode;

import java.util.List;

public class UpdatePlanNode extends PlanNode {
    private final PlanNode child;
    private final TableInfo tableInfo;
    private final List<SetClauseNode> setClauses;

    public UpdatePlanNode(PlanNode child, TableInfo tableInfo, List<SetClauseNode> setClauses) {
        super(null);
        this.child = child;
        this.tableInfo = tableInfo;
        this.setClauses = setClauses;
    }

    public PlanNode getChild() {
        return child;
    }

    public TableInfo getTableInfo() {
        return tableInfo;
    }

    public List<SetClauseNode> getSetClauses() {
        return setClauses;
    }
}


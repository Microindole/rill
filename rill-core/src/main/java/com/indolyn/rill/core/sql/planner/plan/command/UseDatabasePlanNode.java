package com.indolyn.rill.core.sql.planner.plan.command;

import com.indolyn.rill.core.sql.planner.plan.PlanNode;

public class UseDatabasePlanNode extends PlanNode {
    private final String dbName;

    public UseDatabasePlanNode(String dbName) {
        super(null);
        this.dbName = dbName;
    }

    public String getDbName() {
        return dbName;
    }
}

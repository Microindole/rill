package com.indolyn.rill.core.sql.planner.plan.command;

import com.indolyn.rill.core.sql.planner.plan.PlanNode;

public class CreateDatabasePlanNode extends PlanNode {
    private final String dbName;

    public CreateDatabasePlanNode(String dbName) {
        super(null); // DDL operations don't return tuples.
        this.dbName = dbName;
    }

    public String getDbName() {
        return dbName;
    }
}

package com.indolyn.rill.core.compiler.planner.plan.ddl;

import com.indolyn.rill.core.compiler.planner.plan.PlanNode;

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


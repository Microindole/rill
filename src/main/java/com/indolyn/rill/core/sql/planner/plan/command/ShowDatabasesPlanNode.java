package com.indolyn.rill.core.sql.planner.plan.command;

import com.indolyn.rill.core.model.Column;
import com.indolyn.rill.core.model.DataType;
import com.indolyn.rill.core.model.Schema;
import com.indolyn.rill.core.sql.planner.plan.PlanNode;

import java.util.List;

public class ShowDatabasesPlanNode extends PlanNode {
    private static final Schema SHOW_DATABASES_SCHEMA =
        new Schema(List.of(new Column("Database", DataType.VARCHAR)));

    public ShowDatabasesPlanNode() {
        super(SHOW_DATABASES_SCHEMA);
    }
}

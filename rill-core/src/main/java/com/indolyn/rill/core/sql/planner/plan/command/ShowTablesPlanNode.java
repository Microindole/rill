package com.indolyn.rill.core.sql.planner.plan.command;

import com.indolyn.rill.core.model.Column;
import com.indolyn.rill.core.model.DataType;
import com.indolyn.rill.core.model.Schema;
import com.indolyn.rill.core.sql.planner.plan.PlanNode;

import java.util.List;

/**
 * "SHOW TABLES" statement execution plan node.
 */
public class ShowTablesPlanNode extends PlanNode {

    private static final Schema SHOW_TABLES_SCHEMA =
        new Schema(List.of(new Column("TABLES", DataType.VARCHAR)));

    public ShowTablesPlanNode() {
        super(SHOW_TABLES_SCHEMA);
    }
}

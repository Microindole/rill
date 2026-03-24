package com.indolyn.rill.core.compiler.planner.plan.show;

import com.indolyn.rill.core.common.model.Column;
import com.indolyn.rill.core.common.model.DataType;
import com.indolyn.rill.core.common.model.Schema;
import com.indolyn.rill.core.compiler.planner.plan.PlanNode;

import java.util.List;

public class ShowCreateTablePlanNode extends PlanNode {

    private final String tableName;

    // 定义 SHOW CREATE TABLE 的输出 Schema
    private static final Schema SHOW_CREATE_TABLE_SCHEMA =
        new Schema(
            List.of(
                new Column("Table", DataType.VARCHAR), new Column("Create Table", DataType.VARCHAR)));

    public ShowCreateTablePlanNode(String tableName) {
        super(SHOW_CREATE_TABLE_SCHEMA);
        this.tableName = tableName;
    }

    public String getTableName() {
        return tableName;
    }
}

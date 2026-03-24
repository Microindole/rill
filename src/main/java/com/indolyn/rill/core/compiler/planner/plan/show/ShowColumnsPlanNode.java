package com.indolyn.rill.core.compiler.planner.plan.show;

import com.indolyn.rill.core.common.model.Column;
import com.indolyn.rill.core.common.model.DataType;
import com.indolyn.rill.core.common.model.Schema;
import com.indolyn.rill.core.compiler.planner.plan.PlanNode;
import java.util.List;

public class ShowColumnsPlanNode extends PlanNode {

  private final String tableName;

  // 定义 SHOW COLUMNS 的输出 Schema
  private static final Schema SHOW_COLUMNS_SCHEMA =
      new Schema(
          List.of(new Column("Field", DataType.VARCHAR), new Column("Type", DataType.VARCHAR)));

  public ShowColumnsPlanNode(String tableName) {
    super(SHOW_COLUMNS_SCHEMA);
    this.tableName = tableName;
  }

  public String getTableName() {
    return tableName;
  }
}

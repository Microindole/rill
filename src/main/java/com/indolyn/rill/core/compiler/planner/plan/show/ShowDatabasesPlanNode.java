package com.indolyn.rill.core.compiler.planner.plan.show;

import com.indolyn.rill.core.common.model.Column;
import com.indolyn.rill.core.common.model.DataType;
import com.indolyn.rill.core.common.model.Schema;
import com.indolyn.rill.core.compiler.planner.plan.PlanNode;
import java.util.List;

public class ShowDatabasesPlanNode extends PlanNode {
  private static final Schema SHOW_DATABASES_SCHEMA =
      new Schema(List.of(new Column("Database", DataType.VARCHAR)));

  public ShowDatabasesPlanNode() {
    super(SHOW_DATABASES_SCHEMA);
  }
}

package com.indolyn.rill.core.compiler.planner.plan.show;

import com.indolyn.rill.core.common.model.Column;
import com.indolyn.rill.core.common.model.DataType;
import com.indolyn.rill.core.common.model.Schema;
import com.indolyn.rill.core.compiler.planner.plan.PlanNode;
import java.util.List;

/** "SHOW TABLES" statement execution plan node. */
public class ShowTablesPlanNode extends PlanNode {

  private static final Schema SHOW_TABLES_SCHEMA =
      new Schema(List.of(new Column("TABLES", DataType.VARCHAR)));

  public ShowTablesPlanNode() {
    super(SHOW_TABLES_SCHEMA);
  }
}

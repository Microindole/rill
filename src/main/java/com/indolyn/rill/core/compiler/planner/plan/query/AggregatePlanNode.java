package com.indolyn.rill.core.compiler.planner.plan.query;

import com.indolyn.rill.core.common.model.Schema;
import com.indolyn.rill.core.compiler.parser.ast.ExpressionNode;
import com.indolyn.rill.core.compiler.parser.ast.expression.AggregateExpressionNode;
import com.indolyn.rill.core.compiler.parser.ast.expression.IdentifierNode;
import com.indolyn.rill.core.compiler.planner.plan.PlanNode;
import java.util.List;

/** 聚合操作的执行计划节点 */
public class AggregatePlanNode extends PlanNode {
  private final PlanNode child;
  private final List<IdentifierNode> groupBys;
  private final List<AggregateExpressionNode> aggregates;
  private final ExpressionNode havingClause; //

  public AggregatePlanNode(
      PlanNode child,
      List<IdentifierNode> groupBys,
      List<AggregateExpressionNode> aggregates,
      Schema outputSchema,
      ExpressionNode havingClause) {
    super(outputSchema);
    this.child = child;
    this.groupBys = groupBys;
    this.aggregates = aggregates;
    this.havingClause = havingClause;
  }

  public PlanNode getChild() {
    return child;
  }

  public List<IdentifierNode> getGroupBys() {
    return groupBys;
  }

  public List<AggregateExpressionNode> getAggregates() {
    return aggregates;
  }

  //
  public ExpressionNode getHavingClause() {
    return havingClause;
  }
}

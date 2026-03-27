package com.indolyn.rill.core.sql.planner.plan.query;

import com.indolyn.rill.core.model.Column;
import com.indolyn.rill.core.model.Schema;
import com.indolyn.rill.core.sql.ast.ExpressionNode;
import com.indolyn.rill.core.sql.planner.plan.PlanNode;

import java.util.ArrayList;
import java.util.List;

public class JoinPlanNode extends PlanNode {
    private final PlanNode left;
    private final PlanNode right;
    private final ExpressionNode joinCondition;

    public JoinPlanNode(PlanNode left, PlanNode right, ExpressionNode joinCondition) {
        super(createJoinSchema(left.getOutputSchema(), right.getOutputSchema()));
        this.left = left;
        this.right = right;
        this.joinCondition = joinCondition;
    }

    public PlanNode getLeft() {
        return left;
    }

    public PlanNode getRight() {
        return right;
    }

    public ExpressionNode getJoinCondition() {
        return joinCondition;
    }

    private static Schema createJoinSchema(Schema leftSchema, Schema rightSchema) {
        List<Column> allColumns = new ArrayList<>(leftSchema.getColumns());
        allColumns.addAll(rightSchema.getColumns());
        return new Schema(allColumns);
    }
}


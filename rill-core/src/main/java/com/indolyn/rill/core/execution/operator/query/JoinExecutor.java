package com.indolyn.rill.core.execution.operator.query;

import com.indolyn.rill.core.catalog.TableInfo;
import com.indolyn.rill.core.model.Schema;
import com.indolyn.rill.core.model.Tuple;
import com.indolyn.rill.core.model.Value;
import com.indolyn.rill.core.sql.planner.plan.PlanNode;
import com.indolyn.rill.core.sql.planner.plan.query.JoinPlanNode;
import com.indolyn.rill.core.sql.planner.plan.query.SeqScanPlanNode;
import com.indolyn.rill.core.execution.ExpressionEvaluator;
import com.indolyn.rill.core.execution.operator.TupleIterator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 使用嵌套循环连接算法执行 JOIN 操作。
 */
public class JoinExecutor implements TupleIterator {

    private final JoinPlanNode plan;
    private final TupleIterator leftChild;
    private final TupleIterator rightChild;
    private final Schema outputSchema;
    private final TableInfo leftTableInfo;
    private final TableInfo rightTableInfo;
    private Tuple leftTuple;
    private List<Tuple> rightTuples;
    private int rightTupleIndex;
    private Tuple nextTuple;

    public JoinExecutor(JoinPlanNode plan, TupleIterator leftChild, TupleIterator rightChild) {
        this.plan = plan;
        this.leftChild = leftChild;
        this.rightChild = rightChild;
        this.outputSchema = plan.getOutputSchema();
        this.leftTuple = null;
        this.rightTuples = null;
        this.rightTupleIndex = 0;
        this.nextTuple = null;
        this.leftTableInfo = findTableInfo(plan.getLeft());
        this.rightTableInfo = findTableInfo(plan.getRight());

        if (this.leftTableInfo == null || this.rightTableInfo == null) {
            throw new IllegalStateException(
                "Could not find table information for one of the join sides.");
        }
    }

    private TableInfo findTableInfo(PlanNode node) {
        if (node instanceof SeqScanPlanNode seqScanPlan) {
            return seqScanPlan.getTableInfo();
        }

        try {
            java.lang.reflect.Method getChildMethod = node.getClass().getMethod("getChild");
            PlanNode childNode = (PlanNode) getChildMethod.invoke(node);
            return findTableInfo(childNode);
        } catch (Exception e) {
            return null;
        }
    }

    private void init() throws IOException {
        if (rightTuples == null) {
            rightTuples = new ArrayList<>();
            while (rightChild.hasNext()) {
                rightTuples.add(rightChild.next());
            }
        }
    }

    @Override
    public Tuple next() throws IOException {
        if (nextTuple == null && !hasNext()) {
            return null;
        }
        Tuple result = this.nextTuple;
        this.nextTuple = null;
        return result;
    }

    @Override
    public boolean hasNext() throws IOException {
        if (nextTuple != null) {
            return true;
        }

        init();

        while (true) {
            if (leftTuple == null) {
                if (leftChild.hasNext()) {
                    leftTuple = leftChild.next();
                    rightTupleIndex = 0;
                } else {
                    return false;
                }
            }
            while (rightTupleIndex < rightTuples.size()) {
                Tuple rightTuple = rightTuples.get(rightTupleIndex);
                rightTupleIndex++;
                List<Value> combinedValues = new ArrayList<>(leftTuple.getValues());
                combinedValues.addAll(rightTuple.getValues());
                Tuple combinedTuple = new Tuple(combinedValues);
                if (ExpressionEvaluator.evaluate(
                    plan.getJoinCondition(), outputSchema, combinedTuple, leftTableInfo, rightTableInfo)) {
                    this.nextTuple = combinedTuple;
                    return true;
                }
            }
            leftTuple = null;
        }
    }

    @Override
    public Schema getOutputSchema() {
        return outputSchema;
    }
}

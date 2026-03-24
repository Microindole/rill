package com.indolyn.rill.core.execution.operator.command;

import com.indolyn.rill.core.model.Schema;
import com.indolyn.rill.core.model.Tuple;
import com.indolyn.rill.core.sql.planner.plan.command.ShowCreateTablePlanNode;
import com.indolyn.rill.core.execution.operator.TupleIterator;

import java.io.IOException;

public class ShowCreateTableExecutor implements TupleIterator {

    private final ShowCreateTablePlanNode plan;
    private boolean executed = false;

    public ShowCreateTableExecutor(ShowCreateTablePlanNode plan) {
        this.plan = plan;
    }

    @Override
    public Tuple next() throws IOException {
        // 我们不返回任何行，所以这个方法总是返回 null
        return null;
    }

    @Override
    public boolean hasNext() throws IOException {
        // 因为我们不返回任何行，所以总是不存在下一行
        return false;
    }

    @Override
    public Schema getOutputSchema() {
        return plan.getOutputSchema();
    }
}

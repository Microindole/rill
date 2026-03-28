package com.indolyn.rill.core.execution.operator.command;

import com.indolyn.rill.core.execution.operator.TupleIterator;
import com.indolyn.rill.core.model.Schema;
import com.indolyn.rill.core.model.Tuple;
import com.indolyn.rill.core.model.Value;
import com.indolyn.rill.core.sql.planner.plan.command.UseDatabasePlanNode;
import java.io.IOException;
import java.util.Collections;

public class UseDatabaseExecutor implements TupleIterator {

    private final UseDatabasePlanNode plan;
    private boolean executed = false;

    public UseDatabaseExecutor(UseDatabasePlanNode plan) {
        this.plan = plan;
    }

    @Override
    public Tuple next() throws IOException {
        if (!executed) {
            executed = true;
            return new Tuple(
                Collections.singletonList(new Value("Database changed to '" + plan.getDbName() + "'.")));
        }
        return null;
    }

    @Override
    public boolean hasNext() throws IOException {
        return !executed;
    }

    @Override
    public Schema getOutputSchema() {
        return null;
    }
}

package com.indolyn.rill.core.execution.operator.command;

import com.indolyn.rill.core.storage.database.DatabaseManager;
import com.indolyn.rill.core.model.Schema;
import com.indolyn.rill.core.model.Tuple;
import com.indolyn.rill.core.model.Value;
import com.indolyn.rill.core.sql.planner.plan.command.DropDatabasePlanNode;
import com.indolyn.rill.core.execution.operator.TupleIterator;

import java.io.IOException;
import java.util.Collections;

public class DropDatabaseExecutor implements TupleIterator {

    private final DropDatabasePlanNode plan;
    private final DatabaseManager dbManager;
    private boolean executed = false;

    public DropDatabaseExecutor(DropDatabasePlanNode plan, DatabaseManager dbManager) {
        this.plan = plan;
        this.dbManager = dbManager;
    }

    @Override
    public Tuple next() throws IOException {
        if (!executed) {
            dbManager.dropDatabase(plan.getDbName());
            executed = true;
            return new Tuple(
                Collections.singletonList(new Value("Database '" + plan.getDbName() + "' dropped.")));
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

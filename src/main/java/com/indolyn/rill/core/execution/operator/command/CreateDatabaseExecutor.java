package com.indolyn.rill.core.execution.operator.command;

import com.indolyn.rill.core.storage.database.DatabaseManager;
import com.indolyn.rill.core.model.Schema;
import com.indolyn.rill.core.model.Tuple;
import com.indolyn.rill.core.model.Value;
import com.indolyn.rill.core.sql.planner.plan.command.CreateDatabasePlanNode;
import com.indolyn.rill.core.execution.operator.TupleIterator;

import java.io.IOException;
import java.util.Collections;

public class CreateDatabaseExecutor implements TupleIterator {

    private final CreateDatabasePlanNode plan;
    private final DatabaseManager dbManager;
    private boolean executed = false;

    public CreateDatabaseExecutor(CreateDatabasePlanNode plan, DatabaseManager dbManager) {
        this.plan = plan;
        this.dbManager = dbManager;
    }

    @Override
    public Tuple next() throws IOException {
        if (!executed) {
            dbManager.createDatabase(plan.getDbName());
            executed = true;
            return new Tuple(
                Collections.singletonList(new Value("Database '" + plan.getDbName() + "' created.")));
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

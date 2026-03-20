package com.indolyn.rill.core.executor.ddl;

import com.indolyn.rill.core.DatabaseManager;
import com.indolyn.rill.core.common.model.Schema;
import com.indolyn.rill.core.common.model.Tuple;
import com.indolyn.rill.core.common.model.Value;
import com.indolyn.rill.core.compiler.planner.plan.ddl.DropDatabasePlanNode;
import com.indolyn.rill.core.executor.TupleIterator;

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
            return new Tuple(Collections.singletonList(new Value("Database '" + plan.getDbName() + "' dropped.")));
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


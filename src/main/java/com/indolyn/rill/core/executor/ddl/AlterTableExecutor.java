package com.indolyn.rill.core.executor.ddl;

import com.indolyn.rill.core.catalog.Catalog;
import com.indolyn.rill.core.common.model.*;
import com.indolyn.rill.core.compiler.planner.plan.ddl.AlterTablePlanNode;
import com.indolyn.rill.core.executor.TupleIterator;
import com.indolyn.rill.core.transaction.Transaction;
import com.indolyn.rill.core.transaction.log.LogManager;
import com.indolyn.rill.core.transaction.log.LogRecord;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class AlterTableExecutor implements TupleIterator {

    private final AlterTablePlanNode plan;
    private final Catalog catalog;
    private boolean done = false;
    private static final Schema RESULT_SCHEMA =
        new Schema(List.of(new Column("message", DataType.VARCHAR)));
    private final Transaction txn;
    private final LogManager logManager;

    public AlterTableExecutor(
        AlterTablePlanNode plan, Catalog catalog, Transaction txn, LogManager logManager) {
        this.plan = plan;
        this.catalog = catalog;
        this.txn = txn;
        this.logManager = logManager;
    }

    @Override
    public Tuple next() throws IOException {
        if (done) {
            return null;
        }

        LogRecord logRecord =
            new LogRecord(
                txn.getTransactionId(),
                txn.getPrevLSN(),
                LogRecord.LogType.ALTER_TABLE,
                plan.getTableName(),
                plan.getNewColumn());
        long lsn = logManager.appendLogRecord(logRecord);
        txn.setPrevLSN(lsn);

        catalog.addColumn(plan.getTableName(), plan.getNewColumn());
        done = true;
        return new Tuple(
            Collections.singletonList(new Value("Table '" + plan.getTableName() + "' altered.")));
    }

    @Override
    public boolean hasNext() {
        return !done;
    }

    @Override
    public Schema getOutputSchema() {
        return RESULT_SCHEMA;
    }
}

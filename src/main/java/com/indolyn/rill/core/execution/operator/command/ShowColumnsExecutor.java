package com.indolyn.rill.core.execution.operator.command;

import com.indolyn.rill.core.catalog.Catalog;
import com.indolyn.rill.core.catalog.TableInfo;
import com.indolyn.rill.core.model.Schema;
import com.indolyn.rill.core.model.Tuple;
import com.indolyn.rill.core.model.Value;
import com.indolyn.rill.core.sql.planner.plan.command.ShowColumnsPlanNode;
import com.indolyn.rill.core.execution.operator.TupleIterator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ShowColumnsExecutor implements TupleIterator {

    private final ShowColumnsPlanNode plan;
    private final Catalog catalog;
    private Iterator<Tuple> resultIterator;

    public ShowColumnsExecutor(ShowColumnsPlanNode plan, Catalog catalog) {
        this.plan = plan;
        this.catalog = catalog;
    }

    private void generateColumnList() {
        TableInfo tableInfo = catalog.getTable(plan.getTableName());
        if (tableInfo == null) {
            throw new IllegalStateException(
                "Table '" + plan.getTableName() + "' not found during execution.");
        }

        List<Tuple> resultTuples = new ArrayList<>();
        Schema tableSchema = tableInfo.getSchema();

        for (com.indolyn.rill.core.model.Column column : tableSchema.getColumns()) {
            List<Value> values = new ArrayList<>();
            values.add(new Value(column.getName()));
            // 为了与 MySQL 类型更好地兼容，我们返回小写类型名
            values.add(new Value(column.getType().name().toLowerCase()));
            resultTuples.add(new Tuple(values));
        }
        this.resultIterator = resultTuples.iterator();
    }

    @Override
    public Tuple next() throws IOException {
        if (resultIterator == null) {
            generateColumnList();
        }
        return resultIterator.hasNext() ? resultIterator.next() : null;
    }

    @Override
    public boolean hasNext() throws IOException {
        if (resultIterator == null) {
            generateColumnList();
        }
        return resultIterator.hasNext();
    }

    @Override
    public Schema getOutputSchema() {
        return plan.getOutputSchema();
    }
}

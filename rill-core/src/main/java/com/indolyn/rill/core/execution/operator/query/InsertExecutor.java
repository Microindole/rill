package com.indolyn.rill.core.execution.operator.query;

import com.indolyn.rill.core.catalog.Catalog;
import com.indolyn.rill.core.catalog.IndexInfo;
import com.indolyn.rill.core.model.*;
import com.indolyn.rill.core.sql.planner.plan.command.InsertPlanNode;
import com.indolyn.rill.core.execution.operator.TableHeap;
import com.indolyn.rill.core.execution.operator.TupleIterator;
import com.indolyn.rill.core.storage.buffer.BufferPoolManager;
import com.indolyn.rill.core.storage.index.BPlusTree;
import com.indolyn.rill.core.transaction.Transaction;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class InsertExecutor implements TupleIterator {

    private final InsertPlanNode plan;
    private final TableHeap tableHeap;
    private final Transaction txn;
    private boolean done = false;
    private static final Schema AFFECTED_ROWS_SCHEMA =
        new Schema(List.of(new Column("inserted_rows", DataType.INT)));

    private final Catalog catalog;
    private final BufferPoolManager bufferPoolManager;

    public InsertExecutor(
        InsertPlanNode plan,
        TableHeap tableHeap,
        Transaction txn,
        Catalog catalog,
        BufferPoolManager bufferPoolManager) {
        this.plan = plan;
        this.tableHeap = tableHeap;
        this.txn = txn;
        this.catalog = catalog;
        this.bufferPoolManager = bufferPoolManager;
    }

    @Override
    public Tuple next() throws IOException {
        if (done) {
            return null;
        }

        String primaryKeyColumnName = plan.getTableInfo().getSchema().getPrimaryKeyColumnName();
        int insertCount = 0;

        for (Tuple tuple : plan.getRawTuples()) {
            if (primaryKeyColumnName != null) {
                int pkIndex = plan.getTableInfo().getSchema().getColumnIndex(primaryKeyColumnName);
                Value pkValue = tuple.getValues().get(pkIndex);

                IndexInfo pkIndexInfo =
                    catalog.getIndex(plan.getTableInfo().getTableName(), primaryKeyColumnName);

                if (pkIndexInfo != null) {
                    BPlusTree pkTree = new BPlusTree(bufferPoolManager, pkIndexInfo.getRootPageId());
                    if (pkTree.search(pkValue) != null) {
                        throw new RuntimeException(
                            "Primary key constraint violation: Duplicate key '" + pkValue + "'");
                    }
                }
            }
            if (tableHeap.insertTuple(tuple, txn)) {
                updateAllIndexesForInsert(tuple);
                insertCount++;
            }
        }

        done = true;
        return new Tuple(Collections.singletonList(new Value(insertCount)));
    }

    private void updateAllIndexesForInsert(Tuple tuple) throws IOException {
        RID rid = tuple.getRid();
        String tableName = plan.getTableInfo().getTableName();
        List<IndexInfo> indexes = catalog.getIndexesForTable(tableName);

        for (IndexInfo indexInfo : indexes) {
            BPlusTree index = new BPlusTree(bufferPoolManager, indexInfo.getRootPageId());
            int keyColumnIndex =
                plan.getTableInfo().getSchema().getColumnIndex(indexInfo.getColumnName());
            Value key = tuple.getValues().get(keyColumnIndex);
            index.insert(key, rid);
        }
    }

    @Override
    public boolean hasNext() {
        return !done;
    }

    @Override
    public Schema getOutputSchema() {
        return AFFECTED_ROWS_SCHEMA;
    }
}

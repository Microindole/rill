package com.indolyn.rill.core.execution.operator.query;

import com.indolyn.rill.core.catalog.Catalog;
import com.indolyn.rill.core.catalog.IndexInfo;
import com.indolyn.rill.core.model.*;
import com.indolyn.rill.core.sql.ast.expression.LiteralNode;
import com.indolyn.rill.core.sql.ast.expression.SetClauseNode;
import com.indolyn.rill.core.execution.operator.TableHeap;
import com.indolyn.rill.core.execution.operator.TupleIterator;
import com.indolyn.rill.core.storage.buffer.BufferPoolManager;
import com.indolyn.rill.core.storage.index.BPlusTree;
import com.indolyn.rill.core.transaction.Transaction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UpdateExecutor implements TupleIterator {
    private final TupleIterator child;
    private final TableHeap tableHeap;
    private final Schema schema;
    private final List<SetClauseNode> setClauses;
    private final Transaction txn;
    private final Catalog catalog;
    private final BufferPoolManager bufferPoolManager;
    private boolean done = false;

    private static final Schema AFFECTED_ROWS_SCHEMA =
        new Schema(List.of(new Column("updated_rows", DataType.INT)));

    public UpdateExecutor(
        TupleIterator child,
        TableHeap tableHeap,
        Schema schema,
        List<SetClauseNode> setClauses,
        Transaction txn,
        Catalog catalog,
        BufferPoolManager bufferPoolManager) {
        this.child = child;
        this.tableHeap = tableHeap;
        this.schema = schema;
        this.setClauses = setClauses;
        this.txn = txn;
        this.catalog = catalog;
        this.bufferPoolManager = bufferPoolManager;
    }

    @Override
    public Tuple next() throws IOException {
        if (done) {
            return null;
        }

        List<Tuple> tuplesToUpdate = new ArrayList<>();
        while (child.hasNext()) {
            tuplesToUpdate.add(child.next());
        }
        int updatedCount = 0;
        for (Tuple oldTuple : tuplesToUpdate) {
            List<Value> newValues = new ArrayList<>(oldTuple.getValues());
            for (SetClauseNode clause : setClauses) {
                String colName = clause.column().getName();
                int colIndex = getColumnIndex(schema, colName);
                DataType targetType = schema.getColumns().get(colIndex).getType();
                Value newValue = getLiteralValue((LiteralNode) clause.value(), targetType);
                newValues.set(colIndex, newValue);
            }
            Tuple newTuple = new Tuple(newValues);

            String pkColumnName = schema.getPrimaryKeyColumnName();
            if (pkColumnName != null) {
                int pkIndex = schema.getColumnIndex(pkColumnName);
                Value oldPkValue = oldTuple.getValues().get(pkIndex);
                Value newPkValue = newTuple.getValues().get(pkIndex);
                if (!oldPkValue.equals(newPkValue)) {
                    IndexInfo pkIndexInfo =
                        catalog.getIndex(tableHeap.getTableInfo().getTableName(), pkColumnName);
                    if (pkIndexInfo != null) {
                        BPlusTree pkTree = new BPlusTree(bufferPoolManager, pkIndexInfo.getRootPageId());
                        if (pkTree.search(newPkValue) != null) {
                            throw new RuntimeException(
                                "Primary key constraint violation: Cannot update to existing key '"
                                    + newPkValue
                                    + "'");
                        }
                    }
                }
            }

            RID newRid = tableHeap.updateTuple(newTuple, oldTuple.getRid(), txn);
            if (newRid != null) {
                updateAllIndexesForUpdate(oldTuple, newTuple, newRid);
                updatedCount++;
            }
        }
        done = true;
        return new Tuple(Collections.singletonList(new Value(updatedCount)));
    }

    private void updateAllIndexesForUpdate(Tuple oldTuple, Tuple newTuple, RID newRid)
        throws IOException {
        String tableName = tableHeap.getTableInfo().getTableName();
        List<IndexInfo> indexes = catalog.getIndexesForTable(tableName);

        for (IndexInfo indexInfo : indexes) {
            BPlusTree index = new BPlusTree(bufferPoolManager, indexInfo.getRootPageId());
            int keyColumnIndex = schema.getColumnIndex(indexInfo.getColumnName());

            Value oldKey = oldTuple.getValues().get(keyColumnIndex);
            Value newKey = newTuple.getValues().get(keyColumnIndex);

            if (!oldKey.equals(newKey)) {
                index.delete(oldKey);
                index.insert(newKey, newRid);
            }
        }
    }

    @Override
    public boolean hasNext() throws IOException {
        return !done;
    }

    private int getColumnIndex(Schema schema, String columnName) {
        for (int i = 0; i < schema.getColumns().size(); i++) {
            if (schema.getColumns().get(i).getName().equalsIgnoreCase(columnName)) {
                return i;
            }
        }
        throw new IllegalStateException("Column '" + columnName + "' not found in schema.");
    }

    private Value getLiteralValue(LiteralNode literalNode, DataType targetType) {
        String lexeme = literalNode.literal().lexeme();
        return switch (literalNode.literal().type()) {
            case INTEGER_CONST -> parseIntegerLiteral(lexeme, targetType);
            case STRING_CONST -> new Value(lexeme);
            default -> throw new IllegalStateException("Unsupported literal type.");
        };
    }

    private Value parseIntegerLiteral(String lexeme, DataType targetType) {
        return switch (targetType) {
            case SMALLINT -> new Value(Short.parseShort(lexeme));
            case BIGINT -> new Value(Long.parseLong(lexeme));
            case INT -> new Value(Integer.parseInt(lexeme));
            default -> {
                try {
                    yield new Value(Integer.parseInt(lexeme));
                } catch (NumberFormatException ignored) {
                    yield new Value(Long.parseLong(lexeme));
                }
            }
        };
    }

    @Override
    public Schema getOutputSchema() {
        return AFFECTED_ROWS_SCHEMA;
    }
}

package com.indolyn.rill.core.catalog;

import com.indolyn.rill.core.model.Column;
import com.indolyn.rill.core.model.DataType;
import com.indolyn.rill.core.model.Schema;
import com.indolyn.rill.core.model.Tuple;
import com.indolyn.rill.core.model.Value;
import com.indolyn.rill.core.storage.buffer.PageAccess;
import com.indolyn.rill.core.storage.page.Page;
import com.indolyn.rill.core.storage.page.PageId;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

final class CatalogMetadataStore implements CatalogMetadataAccess {
    private final PageAccess pageAccess;

    CatalogMetadataStore(PageAccess pageAccess) {
        this.pageAccess = pageAccess;
    }

    @Override
    public List<Column> readColumnsForTable(
        int tableId, PageId columnsTableFirstPageId, Schema columnsTableSchema)
        throws IOException {
        Page columnsPage = pageAccess.getPage(columnsTableFirstPageId);
        List<Tuple> columnMetadata = columnsPage.getAllTuples(columnsTableSchema);
        List<Column> columns = new ArrayList<>();
        for (Tuple columnTuple : columnMetadata) {
            if ((int) columnTuple.getValues().get(0).getValue() == tableId) {
                String columnName = (String) columnTuple.getValues().get(1).getValue();
                DataType columnType = DataType.valueOf((String) columnTuple.getValues().get(2).getValue());
                columns.add(new Column(columnName, columnType));
            }
        }
        return columns;
    }

    @Override
    public void writeSchemaToColumnsTable(PageId columnsTableFirstPageId, int tableId, Schema schema)
        throws IOException {
        Page columnsPage = pageAccess.getPage(columnsTableFirstPageId);
        int columnIndex = 0;
        for (Column column : schema.getColumns()) {
            columnsPage.insertTuple(
                new Tuple(
                    Arrays.asList(
                        new Value(tableId),
                        new Value(column.getName()),
                        new Value(column.getType().toString()),
                        new Value(columnIndex++))));
        }
        pageAccess.flushPage(columnsTableFirstPageId);
    }

    @Override
    public Tuple getTableTuple(
        String tableName, int tableId, PageId tablesTableFirstPageId, Schema tablesTableSchema)
        throws IOException {
        Page page = pageAccess.getPage(tablesTableFirstPageId);
        List<Tuple> tuples = page.getAllTuples(tablesTableSchema);
        for (Tuple tuple : tuples) {
            if ((int) tuple.getValues().getFirst().getValue() == tableId) {
                return tuple;
            }
        }
        return null;
    }

    @Override
    public void persistTableEntry(
        PageId tablesTableFirstPageId,
        int tableId,
        String tableName,
        PageId firstPageId,
        Schema tablesTableSchema)
        throws IOException {
        Page tablesPage = pageAccess.getPage(tablesTableFirstPageId);
        tablesPage.insertTuple(
            new Tuple(Arrays.asList(new Value(tableId), new Value(tableName), new Value(firstPageId.getPageNum()))));
        pageAccess.flushPage(tablesTableFirstPageId);
    }

    @Override
    public void persistNewTable(
        PageId tablesTableFirstPageId,
        PageId columnsTableFirstPageId,
        Schema tablesTableSchema,
        int tableId,
        String tableName,
        PageId firstPageId,
        Schema schema)
        throws IOException {
        persistTableEntry(tablesTableFirstPageId, tableId, tableName, firstPageId, tablesTableSchema);
        writeSchemaToColumnsTable(columnsTableFirstPageId, tableId, schema);
    }

    @Override
    public void persistAddedColumn(
        PageId columnsTableFirstPageId, int tableId, int columnIndex, Column newColumn)
        throws IOException {
        Page columnsPage = pageAccess.getPage(columnsTableFirstPageId);
        Tuple columnMeta =
            new Tuple(
                Arrays.asList(
                    new Value(tableId),
                    new Value(newColumn.getName()),
                    new Value(newColumn.getType().toString()),
                    new Value(columnIndex)));
        columnsPage.insertTuple(columnMeta);
        pageAccess.flushPage(columnsTableFirstPageId);
    }

    @Override
    public void deleteMatchingTuples(PageId pageId, Schema schema, int columnIndex, Value value)
        throws IOException {
        Page page = pageAccess.getPage(pageId);
        List<Tuple> tuples = page.getAllTuples(schema);
        List<Integer> slotsToDelete = new ArrayList<>();
        for (int i = 0; i < tuples.size(); i++) {
            Tuple tuple = tuples.get(i);
            if (tuple != null && tuple.getValues().get(columnIndex).getValue().equals(value.getValue())) {
                slotsToDelete.add(i);
            }
        }

        Collections.sort(slotsToDelete, Collections.reverseOrder());
        for (Integer slotIndex : slotsToDelete) {
            page.deleteTuple(slotIndex);
        }
        pageAccess.flushPage(pageId);
    }
}

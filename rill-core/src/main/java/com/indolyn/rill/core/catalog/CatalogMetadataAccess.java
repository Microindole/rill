package com.indolyn.rill.core.catalog;

import com.indolyn.rill.core.model.Column;
import com.indolyn.rill.core.model.Schema;
import com.indolyn.rill.core.model.Tuple;
import com.indolyn.rill.core.model.Value;
import com.indolyn.rill.core.storage.page.PageId;

import java.io.IOException;
import java.util.List;

interface CatalogMetadataAccess {
    List<Column> readColumnsForTable(int tableId, PageId columnsTableFirstPageId, Schema columnsTableSchema)
        throws IOException;

    void writeSchemaToColumnsTable(PageId columnsTableFirstPageId, int tableId, Schema schema)
        throws IOException;

    Tuple getTableTuple(String tableName, int tableId, PageId tablesTableFirstPageId, Schema tablesTableSchema)
        throws IOException;

    void persistTableEntry(
        PageId tablesTableFirstPageId,
        int tableId,
        String tableName,
        PageId firstPageId,
        Schema tablesTableSchema)
        throws IOException;

    void persistNewTable(
        PageId tablesTableFirstPageId,
        PageId columnsTableFirstPageId,
        Schema tablesTableSchema,
        int tableId,
        String tableName,
        PageId firstPageId,
        Schema schema)
        throws IOException;

    void persistAddedColumn(PageId columnsTableFirstPageId, int tableId, int columnIndex, Column newColumn)
        throws IOException;

    void deleteMatchingTuples(PageId pageId, Schema schema, int columnIndex, Value value)
        throws IOException;
}

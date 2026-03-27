package com.indolyn.rill.core.catalog;

import java.util.List;

interface IndexCatalogAccess {
    void createIndex(String indexName, String tableName, String columnName, int rootPageId);

    void updateRootPageId(String indexName, int newRootPageId);

    IndexInfo getIndex(String tableName, String columnName);

    IndexInfo getIndex(String indexName);

    List<IndexInfo> getIndexesForTable(String tableName);

    void dropIndexesForTable(String tableName);
}

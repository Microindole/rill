package com.indolyn.rill.core.catalog;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

final class IndexRegistry implements IndexCatalogAccess {
    private final Map<String, IndexInfo> indices = new ConcurrentHashMap<>();

    @Override
    public void createIndex(String indexName, String tableName, String columnName, int rootPageId) {
        if (indices.containsKey(indexName)) {
            throw new IllegalStateException("Index '" + indexName + "' already exists.");
        }
        indices.put(indexName, new IndexInfo(indexName, tableName, columnName, rootPageId));
    }

    @Override
    public void updateRootPageId(String indexName, int newRootPageId) {
        IndexInfo indexInfo = indices.get(indexName);
        if (indexInfo == null) {
            throw new IllegalStateException(
                "Cannot update root page for non-existent index '" + indexName + "'.");
        }
        indexInfo.setRootPageId(newRootPageId);
    }

    @Override
    public IndexInfo getIndex(String tableName, String columnName) {
        for (IndexInfo indexInfo : indices.values()) {
            if (indexInfo.getTableName().equalsIgnoreCase(tableName)
                && indexInfo.getColumnName().equalsIgnoreCase(columnName)) {
                return indexInfo;
            }
        }
        return null;
    }

    @Override
    public IndexInfo getIndex(String indexName) {
        return indices.get(indexName);
    }

    @Override
    public List<IndexInfo> getIndexesForTable(String tableName) {
        return indices.values().stream()
            .filter(indexInfo -> indexInfo.getTableName().equalsIgnoreCase(tableName))
            .collect(Collectors.toList());
    }

    @Override
    public void dropIndexesForTable(String tableName) {
        List<String> indexesToRemove =
            indices.values().stream()
                .filter(indexInfo -> indexInfo.getTableName().equalsIgnoreCase(tableName))
                .map(IndexInfo::getIndexName)
                .collect(Collectors.toList());

        for (String indexName : indexesToRemove) {
            indices.remove(indexName);
        }
    }
}

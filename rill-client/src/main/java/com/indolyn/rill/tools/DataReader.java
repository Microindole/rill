package com.indolyn.rill.tools;

import com.indolyn.rill.core.storage.database.DatabaseManager;
import com.indolyn.rill.core.catalog.Catalog;
import com.indolyn.rill.core.catalog.TableInfo;
import com.indolyn.rill.core.model.Column;
import com.indolyn.rill.core.model.Schema;
import com.indolyn.rill.core.model.Tuple;
import com.indolyn.rill.core.model.Value;
import com.indolyn.rill.core.storage.buffer.BufferPoolManager;
import com.indolyn.rill.core.storage.disk.DiskManager;
import com.indolyn.rill.core.storage.page.Page;
import com.indolyn.rill.core.storage.page.PageId;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DataReader {
    public static void exportDatabaseToFile(String dbName, File outputFile) throws IOException {
        DiskManager diskManager = new DiskManager(DatabaseManager.getDbFilePath(dbName));
        diskManager.open();
        BufferPoolManager bufferPoolManager = new BufferPoolManager(10, diskManager, "LRU");
        Catalog catalog = new Catalog(bufferPoolManager);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write("-- rill SQL Dump\n");
            writer.write("-- Database: " + dbName + "\n");
            writer.write("-- ------------------------------------------------------\n\n");

            List<String> userTables = catalog.getTableNames();
            for (String tableName : userTables) {
                TableInfo tableInfo = catalog.getTable(tableName);
                writer.write("-- Table structure for table `" + tableName + "`\n");
                writer.write(generateCreateTableSql(tableInfo) + "\n\n");
                List<Tuple> tuples = getAllTuplesForTable(tableInfo, bufferPoolManager);
                if (!tuples.isEmpty()) {
                    writer.write("-- Dumping data for table `" + tableName + "`\n");
                    for (Tuple tuple : tuples) {
                        writer.write(generateInsertSql(tableInfo, tuple) + "\n");
                    }
                    writer.write("\n");
                }
            }
        } finally {
            diskManager.close();
        }
    }

    public static String renderDatabaseReport(String dbName) throws IOException {
        DiskManager diskManager = new DiskManager(DatabaseManager.getDbFilePath(dbName));
        diskManager.open();
        BufferPoolManager bufferPoolManager = new BufferPoolManager(10, diskManager, "LRU");
        Catalog catalog = new Catalog(bufferPoolManager);
        StringBuilder report = new StringBuilder();

        List<String> userTables = catalog.getTableNames();
        report
            .append("\n--- 正在读取数据库 '")
            .append(dbName)
            .append("' 的数据 ---\n")
            .append("在数据库 '")
            .append(dbName)
            .append("' 中找到 ")
            .append(userTables.size())
            .append(" 个用户表: ")
            .append(userTables)
            .append('\n');

        for (String tableName : userTables) {
            TableInfo tableInfo = catalog.getTable(tableName);
            List<Tuple> allTuplesInTable = getAllTuplesForTable(tableInfo, bufferPoolManager);
            report
                .append("\n--- 表 '")
                .append(tableName)
                .append("' 的数据 ---\n")
                .append(QueryResultFormatter.format(tableInfo.getSchema(), allTuplesInTable))
                .append('\n');
        }
        diskManager.close();
        return report.toString();
    }

    private static String generateCreateTableSql(TableInfo tableInfo) {
        StringBuilder sb = new StringBuilder();
        Schema schema = tableInfo.getSchema();
        sb.append("CREATE TABLE ").append(tableInfo.getTableName()).append(" (\n");
        List<String> columnDefs = new ArrayList<>();
        for (Column column : schema.getColumns()) {
            String colDef = "  " + column.getName() + " " + column.formatTypeDeclaration();
            if (column.getName().equalsIgnoreCase(schema.getPrimaryKeyColumnName())) {
                colDef += " PRIMARY KEY";
            }
            columnDefs.add(colDef);
        }
        sb.append(String.join(",\n", columnDefs));
        sb.append("\n);");
        return sb.toString();
    }

    private static String generateInsertSql(TableInfo tableInfo, Tuple tuple) {
        Schema schema = tableInfo.getSchema();
        String columns = schema.getColumnNames().stream().collect(Collectors.joining(", "));
        String values =
            tuple.getValues().stream()
                .map(DataReader::formatValueForSql)
                .collect(Collectors.joining(", "));
        return String.format(
            "INSERT INTO %s (%s) VALUES (%s);", tableInfo.getTableName(), columns, values);
    }

    private static String formatValueForSql(Value value) {
        if (value.getValue() == null) {
            return "NULL";
        }
        switch (value.getType()) {
            case VARCHAR:
            case DATE:
            case TIMESTAMP:
                return "'" + value.getValue().toString().replace("'", "''") + "'";
            case SMALLINT:
            case INT:
            case BIGINT:
            case DECIMAL:
            case BOOLEAN:
                return value.getValue().toString();
            default:
                return "'" + value.getValue().toString() + "'";
        }
    }

    private static List<Tuple> getAllTuplesForTable(
        TableInfo tableInfo, BufferPoolManager bufferPoolManager) throws IOException {
        List<Tuple> allTuples = new ArrayList<>();
        if (tableInfo == null) return allTuples;
        PageId currentPageId = tableInfo.getFirstPageId();
        while (currentPageId != null && currentPageId.getPageNum() != -1) {
            Page page = bufferPoolManager.getPage(currentPageId);
            allTuples.addAll(page.getAllTuples(tableInfo.getSchema()));
            int nextPageNum = page.getNextPageId();
            currentPageId = nextPageNum != -1 ? new PageId(nextPageNum) : null;
        }
        return allTuples;
    }
}

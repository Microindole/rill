package com.indolyn.rill.tools;

import com.indolyn.rill.core.catalog.Catalog;
import com.indolyn.rill.core.model.Schema;
import com.indolyn.rill.core.model.Tuple;
import com.indolyn.rill.core.storage.buffer.BufferPoolManager;
import com.indolyn.rill.core.storage.database.DatabaseManager;
import com.indolyn.rill.core.storage.disk.DiskManager;
import com.indolyn.rill.core.transaction.log.LogRecord;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public final class LogInspectionService {

    public List<LogRecord> readAllLogRecords(String dbName) throws IOException {
        String logFilePath = DatabaseManager.getDbFilePath(dbName) + ".log";
        File logFile = new File(logFilePath);
        if (!logFile.exists() || logFile.length() == 0) {
            return List.of();
        }

        List<LogRecord> records = new ArrayList<>();
        try (RandomAccessFile file = new RandomAccessFile(logFilePath, "r")) {
            long fileLength = file.length();
            long currentPosition = 0;

            while (currentPosition < fileLength) {
                file.seek(currentPosition);
                if (fileLength - currentPosition < 4) {
                    break;
                }
                int recordSize = file.readInt();
                if (recordSize <= 0 || recordSize > fileLength - currentPosition) {
                    System.err.println(
                        "⚠️ 警告: 在偏移量 " + currentPosition + " 发现无效的日志记录大小(" + recordSize + ")。停止解析。");
                    break;
                }
                byte[] recordBytes = new byte[recordSize];
                file.seek(currentPosition);
                int bytesRead = file.read(recordBytes);
                if (bytesRead != recordSize) {
                    System.err.println(
                        "⚠️ 警告: 尝试读取 " + recordSize + " 字节但只读取到 " + bytesRead + " 字节。日志文件可能已损坏。");
                    break;
                }
                ByteBuffer buffer = ByteBuffer.wrap(recordBytes);
                records.add(LogRecord.fromBytes(buffer, null));
                currentPosition += recordSize;
            }
        }
        return records;
    }

    public String formatLogDetails(String dbName, LogRecord record) throws IOException {
        if (record == null) {
            return "";
        }

        DiskManager diskManager = new DiskManager(DatabaseManager.getDbFilePath(dbName));
        diskManager.open();
        try {
            BufferPoolManager bufferPoolManager = new BufferPoolManager(10, diskManager, "LRU");
            Catalog catalog = new Catalog(bufferPoolManager);
            return formatLogDetails(record, catalog);
        } finally {
            diskManager.close();
        }
    }

    private static String formatLogDetails(LogRecord record, Catalog catalog) {
        StringBuilder sb = new StringBuilder();
        sb.append("--- Log Record Details ---\n");
        sb.append(String.format("LSN: %d\n", record.getLsn()));
        sb.append(String.format("Transaction ID: %d\n", record.getTransactionId()));
        sb.append(
            String.format(
                "Previous LSN: %s\n", record.getPrevLSN() == -1 ? "NULL" : record.getPrevLSN()));
        sb.append(String.format("Log Type: %s\n", record.getLogType()));
        sb.append("--------------------------\n\n");

        Schema schema = null;
        if (record.getTableName() != null && catalog.getTable(record.getTableName()) != null) {
            schema = catalog.getTable(record.getTableName()).getSchema();
        }

        switch (record.getLogType()) {
            case INSERT, DELETE:
                sb.append(
                    String.format(
                        "Table: %s\nRID: %s\nTuple: %s",
                        record.getTableName(),
                        record.getRid(),
                        schema != null
                            ? Tuple.fromBytes(record.getTupleBytes(), schema)
                            : "[Schema not found]"));
                break;
            case UPDATE:
                if (schema != null) {
                    Tuple oldTuple = Tuple.fromBytes(record.getOldTupleBytes(), schema);
                    Tuple newTuple = Tuple.fromBytes(record.getNewTupleBytes(), schema);
                    sb.append(
                        String.format(
                            "Table: %s\nRID: %s\nOld Tuple: %s\nNew Tuple: %s",
                            record.getTableName(), record.getRid(), oldTuple, newTuple));
                } else {
                    sb.append("Table: ").append(record.getTableName()).append(" [Schema not found]");
                }
                break;
            case CREATE_TABLE:
                sb.append(
                    String.format(
                        "Table: %s\nSchema: %s",
                        record.getTableName(), record.getSchema().getColumnNames()));
                break;
            case DROP_TABLE:
                sb.append("Table: ").append(record.getTableName());
                break;
            case ALTER_TABLE:
                sb.append(
                    String.format(
                        "Table: %s\nNew Column: %s %s",
                        record.getTableName(),
                        record.getNewColumn().getName(),
                        record.getNewColumn().getType()));
                break;
            case CLR:
                sb.append("UndoNextLSN: ").append(record.getUndoNextLSN());
                break;
        }
        return sb.toString();
    }
}

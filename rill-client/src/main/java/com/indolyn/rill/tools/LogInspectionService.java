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
        return readLogRecords(dbName).records();
    }

    public LogReadResult readLogRecords(String dbName) throws IOException {
        String logFilePath = DatabaseManager.getDbFilePath(dbName) + ".log";
        File logFile = new File(logFilePath);
        if (!logFile.exists()) {
            return new LogReadResult(dbName, logFilePath, List.of(), List.of("日志文件不存在。"));
        }
        if (logFile.length() == 0) {
            return new LogReadResult(dbName, logFilePath, List.of(), List.of("日志文件为空。"));
        }

        List<LogRecord> records = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        try (RandomAccessFile file = new RandomAccessFile(logFilePath, "r")) {
            long fileLength = file.length();
            long currentPosition = 0;

            while (currentPosition < fileLength) {
                file.seek(currentPosition);
                if (fileLength - currentPosition < 4) {
                    warnings.add("日志尾部不足 4 字节，已停止解析。偏移=" + currentPosition);
                    break;
                }
                int recordSize = file.readInt();
                if (recordSize <= 0 || recordSize > fileLength - currentPosition) {
                    warnings.add(
                        "在偏移量 " + currentPosition + " 发现无效的日志记录大小(" + recordSize + ")，已停止解析。");
                    break;
                }
                byte[] recordBytes = new byte[recordSize];
                file.seek(currentPosition);
                int bytesRead = file.read(recordBytes);
                if (bytesRead != recordSize) {
                    warnings.add(
                        "尝试读取 " + recordSize + " 字节但只读取到 " + bytesRead + " 字节，日志文件可能已损坏。");
                    break;
                }
                try {
                    ByteBuffer buffer = ByteBuffer.wrap(recordBytes);
                    records.add(LogRecord.fromBytes(buffer, null));
                } catch (RuntimeException ex) {
                    warnings.add(
                        "在偏移量 " + currentPosition + " 解析日志失败: " + ex.getMessage() + "。已停止解析。");
                    break;
                }
                currentPosition += recordSize;
            }
        }
        return new LogReadResult(dbName, logFilePath, List.copyOf(records), List.copyOf(warnings));
    }

    public String renderConsoleReport(String dbName, boolean includeDetails) throws IOException {
        LogReadResult result = readLogRecords(dbName);
        StringBuilder sb = new StringBuilder();
        sb.append("--- rill 日志检查 ---\n");
        sb.append("数据库: ").append(result.dbName()).append('\n');
        sb.append("日志文件: ").append(result.logFilePath()).append('\n');
        sb.append("记录数: ").append(result.records().size()).append("\n");

        if (!result.warnings().isEmpty()) {
            sb.append("警告:\n");
            for (String warning : result.warnings()) {
                sb.append("- ").append(warning).append('\n');
            }
        }

        if (result.records().isEmpty()) {
            return sb.toString();
        }

        sb.append("\n摘要:\n");
        sb.append(String.format("%-8s %-8s %-10s %-14s %s%n", "LSN", "TxnID", "PrevLSN", "LogType", "Table"));
        for (LogRecord record : result.records()) {
            sb.append(
                String.format(
                    "%-8d %-8d %-10s %-14s %s%n",
                    record.getLsn(),
                    record.getTransactionId(),
                    record.getPrevLSN() == -1 ? "NULL" : record.getPrevLSN(),
                    record.getLogType(),
                    record.getTableName() == null ? "-" : record.getTableName()));
        }

        if (includeDetails) {
            for (LogRecord record : result.records()) {
                sb.append("\n").append(formatLogDetails(dbName, record)).append("\n");
            }
        }
        return sb.toString();
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
        sb.append(String.format("Previous LSN: %s\n", record.getPrevLSN() == -1 ? "NULL" : record.getPrevLSN()));
        sb.append(String.format("Log Type: %s\n", record.getLogType()));
        sb.append("--------------------------\n\n");

        Schema schema = null;
        if (record.getTableName() != null && catalog.getTable(record.getTableName()) != null) {
            schema = catalog.getTable(record.getTableName()).getSchema();
        }

        switch (record.getLogType()) {
            case INSERT, DELETE -> sb.append(
                String.format(
                    "Table: %s\nRID: %s\nTuple: %s",
                    record.getTableName(),
                    record.getRid(),
                    schema != null ? Tuple.fromBytes(record.getTupleBytes(), schema) : "[Schema not found]"));
            case UPDATE -> {
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
            }
            case CREATE_TABLE -> sb.append(
                String.format("Table: %s\nSchema: %s", record.getTableName(), record.getSchema().getColumnNames()));
            case DROP_TABLE -> sb.append("Table: ").append(record.getTableName());
            case ALTER_TABLE -> sb.append(
                String.format(
                    "Table: %s\nNew Column: %s %s",
                    record.getTableName(),
                    record.getNewColumn().getName(),
                    record.getNewColumn().getType()));
            case CLR -> sb.append("UndoNextLSN: ").append(record.getUndoNextLSN());
            case INVALID, COMMIT, ABORT, BEGIN -> sb.append("无额外详情。");
        }
        return sb.toString();
    }

    public record LogReadResult(String dbName, String logFilePath, List<LogRecord> records, List<String> warnings) {
    }
}

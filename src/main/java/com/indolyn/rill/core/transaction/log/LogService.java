package com.indolyn.rill.core.transaction.log;

import java.io.IOException;
import java.util.List;

public interface LogService {
    long appendLogRecord(LogRecord logRecord) throws IOException;

    void flush() throws IOException;

    void close() throws IOException;

    List<LogRecord> readAllLogRecords() throws IOException;

    LogRecord readLogRecord(long lsn) throws IOException;
}

package com.indolyn.rill.core.infrastructure.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.indolyn.rill.core.model.RID;
import com.indolyn.rill.core.model.Tuple;
import com.indolyn.rill.core.model.Value;
import com.indolyn.rill.core.transaction.log.LogManager;
import com.indolyn.rill.core.transaction.log.LogRecord;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LogManagerBaselineTest {

    @TempDir
    Path tempDir;

    @Test
    void logManagerShouldAppendAndReadBackRecords() throws Exception {
        Path logPath = tempDir.resolve("rill.data.log");
        LogManager logManager = new LogManager(logPath.toString());
        try {
            LogRecord begin = new LogRecord(1, -1L, LogRecord.LogType.BEGIN);
            long beginLsn = logManager.appendLogRecord(begin);

            LogRecord insert =
                new LogRecord(
                    1,
                    beginLsn,
                    LogRecord.LogType.INSERT,
                    "users",
                    new RID(2, 3),
                    new Tuple(List.of(new Value(1), new Value("alice"))));
            long insertLsn = logManager.appendLogRecord(insert);

            List<LogRecord> records = logManager.readAllLogRecords();
            LogRecord insertReadBack = logManager.readLogRecord(insertLsn);

            assertEquals(2, records.size());
            assertEquals(LogRecord.LogType.BEGIN, records.get(0).getLogType());
            assertEquals(LogRecord.LogType.INSERT, records.get(1).getLogType());
            assertEquals(beginLsn, records.get(0).getLsn());
            assertNotNull(insertReadBack);
            assertEquals("users", insertReadBack.getTableName());
            assertTrue(insertReadBack.getTupleBytes().length > 0);
        } finally {
            logManager.close();
        }
    }
}

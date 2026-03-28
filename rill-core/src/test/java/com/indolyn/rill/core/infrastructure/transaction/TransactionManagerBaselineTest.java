package com.indolyn.rill.core.infrastructure.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.indolyn.rill.core.storage.page.PageId;
import com.indolyn.rill.core.transaction.LockService;
import com.indolyn.rill.core.transaction.Transaction;
import com.indolyn.rill.core.transaction.TransactionManager;
import com.indolyn.rill.core.transaction.log.LogRecord;
import com.indolyn.rill.core.transaction.log.LogService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class TransactionManagerBaselineTest {

    @Test
    void beginShouldCreateActiveTransactionAndAppendBeginLog() throws Exception {
        RecordingLockService lockService = new RecordingLockService();
        RecordingLogService logService = new RecordingLogService();
        TransactionManager transactionManager = new TransactionManager(lockService, logService);

        Transaction transaction = transactionManager.begin();

        assertEquals(Transaction.State.ACTIVE, transaction.getState());
        assertEquals(1, logService.records.size());
        assertEquals(LogRecord.LogType.BEGIN, logService.records.get(0).getLogType());
        assertTrue(transaction.getPrevLSN() >= 0);
    }

    @Test
    void commitShouldFlushLogsReleaseLocksAndChangeState() throws Exception {
        RecordingLockService lockService = new RecordingLockService();
        RecordingLogService logService = new RecordingLogService();
        TransactionManager transactionManager = new TransactionManager(lockService, logService);
        Transaction transaction = transactionManager.begin();
        transaction.getLockedPageIds().add(11);
        transaction.getLockedPageIds().add(12);

        transactionManager.commit(transaction);

        assertEquals(Transaction.State.COMMITTED, transaction.getState());
        assertEquals(List.of(LogRecord.LogType.BEGIN, LogRecord.LogType.COMMIT), logService.logTypes());
        assertTrue(logService.flushCalled);
        assertTrue(lockService.unlockedPages.contains(11));
        assertTrue(lockService.unlockedPages.contains(12));
        assertTrue(transaction.getLockedPageIds().isEmpty());
    }

    @Test
    void abortShouldAppendAbortLogReleaseLocksAndChangeState() throws Exception {
        RecordingLockService lockService = new RecordingLockService();
        RecordingLogService logService = new RecordingLogService();
        TransactionManager transactionManager = new TransactionManager(lockService, logService);
        Transaction transaction = transactionManager.begin();
        transaction.getLockedPageIds().add(21);

        transactionManager.abort(transaction);

        assertEquals(Transaction.State.ABORTED, transaction.getState());
        assertEquals(List.of(LogRecord.LogType.BEGIN, LogRecord.LogType.ABORT), logService.logTypes());
        assertFalse(logService.flushCalled);
        assertEquals(List.of(21), lockService.unlockedPages);
        assertTrue(transaction.getLockedPageIds().isEmpty());
    }

    private static final class RecordingLockService implements LockService {
        private final List<Integer> unlockedPages = new ArrayList<>();

        @Override
        public void lockShared(Transaction txn, PageId pageId) {
        }

        @Override
        public void lockExclusive(Transaction txn, PageId pageId) {
        }

        @Override
        public void unlock(Transaction txn, PageId pageId) {
            unlockedPages.add(pageId.getPageNum());
            txn.getLockedPageIds().remove(pageId.getPageNum());
        }
    }

    private static final class RecordingLogService implements LogService {
        private final List<LogRecord> records = new ArrayList<>();
        private boolean flushCalled;
        private long nextLsn;

        @Override
        public long appendLogRecord(LogRecord logRecord) {
            logRecord.setLsn(nextLsn);
            records.add(logRecord);
            nextLsn += 100;
            return logRecord.getLsn();
        }

        @Override
        public void flush() {
            flushCalled = true;
        }

        @Override
        public void close() {
        }

        @Override
        public List<LogRecord> readAllLogRecords() {
            return List.copyOf(records);
        }

        @Override
        public LogRecord readLogRecord(long lsn) throws IOException {
            return records.stream().filter(record -> record.getLsn() == lsn).findFirst().orElse(null);
        }

        private List<LogRecord.LogType> logTypes() {
            return records.stream().map(LogRecord::getLogType).toList();
        }
    }
}

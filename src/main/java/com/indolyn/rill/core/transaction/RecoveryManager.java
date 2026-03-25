package com.indolyn.rill.core.transaction;

import com.indolyn.rill.core.catalog.Catalog;
import com.indolyn.rill.core.storage.buffer.BufferPoolManager;
import com.indolyn.rill.core.storage.page.PageId;
import com.indolyn.rill.core.transaction.log.LogManager;
import com.indolyn.rill.core.transaction.log.LogRecord;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecoveryManager {
    private final LogManager logManager;
    private final RecoveryApplier recoveryApplier;

    public RecoveryManager(
        LogManager logManager,
        BufferPoolManager bufferPoolManager,
        Catalog catalog,
        LockManager lockManager) {
        this.logManager = logManager;
        this.recoveryApplier =
            new RecoveryApplier(bufferPoolManager, catalog, logManager, lockManager);
    }

    public void recover() throws IOException {
        System.out.println("[RecoveryManager] Starting recovery process...");
        List<LogRecord> allLogs = logManager.readAllLogRecords();

        if (allLogs.isEmpty()) {
            System.out.println("[RecoveryManager] Log file is empty. No recovery needed.");
            return;
        }

        Map<Integer, TransactionMetadata> activeTxnTable = analyze(allLogs);
        redo(allLogs);
        undo(activeTxnTable);
        System.out.println("[RecoveryManager] Recovery process completed.");
    }

    private Map<Integer, TransactionMetadata> analyze(List<LogRecord> allLogs) {
        System.out.println("[RecoveryManager] --- Analysis Phase ---");
        Map<Integer, TransactionMetadata> activeTxnTable = new HashMap<>();
        for (LogRecord log : allLogs) {
            activeTxnTable
                .computeIfAbsent(log.getTransactionId(), txnId -> new TransactionMetadata(log.getLsn()))
                .lastLSN = log.getLsn();

            if (log.getLogType() == LogRecord.LogType.COMMIT
                || log.getLogType() == LogRecord.LogType.ABORT) {
                activeTxnTable.remove(log.getTransactionId());
            }
        }
        System.out.println(
            "[Analysis] Active transactions to be rolled back: " + activeTxnTable.keySet());
        return activeTxnTable;
    }

    private void redo(List<LogRecord> allLogs) throws IOException {
        System.out.println("[RecoveryManager] --- Redo Phase ---");
        for (LogRecord log : allLogs) {
            applyLog(log, false);
        }
        System.out.println("[Redo] All logged operations have been re-applied.");
    }

    private void undo(Map<Integer, TransactionMetadata> activeTxnTable) throws IOException {
        System.out.println("[RecoveryManager] --- Undo Phase ---");
        for (Integer txnId : activeTxnTable.keySet()) {
            undoTransaction(txnId, activeTxnTable.get(txnId).lastLSN);
        }
    }

    private void undoTransaction(int txnId, long startLsn) throws IOException {
        System.out.println("[Undo] Rolling back transaction " + txnId);
        long lsnToUndo = startLsn;

        while (lsnToUndo != -1) {
            LogRecord logToUndo = logManager.readLogRecord(lsnToUndo);
            if (logToUndo == null) {
                break;
            }

            if (logToUndo.getLogType() == LogRecord.LogType.CLR) {
                lsnToUndo = logToUndo.getUndoNextLSN();
                continue;
            }

            logManager.appendLogRecord(generateCompensationLog(logToUndo));
            applyUndo(logToUndo);
            lsnToUndo = logToUndo.getPrevLSN();
        }

        Transaction fakeTxn = new Transaction(txnId);
        LogRecord abortLog =
            new LogRecord(fakeTxn.getTransactionId(), lsnToUndo, LogRecord.LogType.ABORT);
        logManager.appendLogRecord(abortLog);
        System.out.println("Transaction " + fakeTxn.getTransactionId() + " aborted.");
    }

    /**
     * 根据日志记录，重做或撤销物理操作。
     */
    private void applyLog(LogRecord log, boolean isUndo) throws IOException {
        Transaction fakeTxn = new Transaction(log.getTransactionId());
        try {
            switch (log.getLogType()) {
                case BEGIN:
                case COMMIT:
                case ABORT:
                case CLR:
                    return;
                case CREATE_TABLE, DROP_TABLE, ALTER_TABLE:
                    recoveryApplier.applyDdlLog(log, isUndo);
                    return;
                case INSERT, DELETE, UPDATE:
                    recoveryApplier.applyDmlLog(log, isUndo, fakeTxn);
            }
        } finally {
            releaseRecoveryLocks(fakeTxn);
        }
    }

    private void applyUndo(LogRecord log) throws IOException {
        System.out.println(
            "[Undo] Applying undo for LSN=" + log.getLsn() + ", Type=" + log.getLogType());
        applyLog(log, true); // applyLog 传入 isUndo=true 即可执行逆操作
    }

    private LogRecord generateCompensationLog(LogRecord logToUndo) {
        return new LogRecord(
            logToUndo.getTransactionId(),
            logToUndo.getPrevLSN(),
            LogRecord.LogType.CLR,
            logToUndo.getPrevLSN()
        );
    }

    private void releaseRecoveryLocks(Transaction transaction) {
        for (Integer pageIdNum : new ArrayList<>(transaction.getLockedPageIds())) {
            recoveryApplier.getLockManager().unlock(transaction, new PageId(pageIdNum));
        }
    }

    private static class TransactionMetadata {
        public long lastLSN;

        TransactionMetadata(long lsn) {
            this.lastLSN = lsn;
        }
    }
}

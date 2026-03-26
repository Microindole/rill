package com.indolyn.rill.core.transaction;

import com.indolyn.rill.core.catalog.Catalog;
import com.indolyn.rill.core.catalog.TableInfo;
import com.indolyn.rill.core.execution.operator.TableHeap;
import com.indolyn.rill.core.model.RID;
import com.indolyn.rill.core.model.Schema;
import com.indolyn.rill.core.model.Tuple;
import com.indolyn.rill.core.storage.buffer.PageAccess;
import com.indolyn.rill.core.storage.page.Page;
import com.indolyn.rill.core.storage.page.PageId;
import com.indolyn.rill.core.transaction.log.LogService;
import com.indolyn.rill.core.transaction.log.LogRecord;

import java.io.IOException;

import lombok.Getter;

final class RecoveryApplier {
    private final PageAccess pageAccess;
    private final Catalog catalog;
    private final LogService logManager;
    @Getter
    private final LockService lockManager;

    RecoveryApplier(
        PageAccess pageAccess,
        Catalog catalog,
        LogService logManager,
        LockService lockManager) {
        this.pageAccess = pageAccess;
        this.catalog = catalog;
        this.logManager = logManager;
        this.lockManager = lockManager;
    }

    void applyDdlLog(LogRecord log, boolean isUndo) throws IOException {
        switch (log.getLogType()) {
            case CREATE_TABLE -> applyCreateTableLog(log, isUndo);
            case DROP_TABLE -> applyDropTableLog(log, isUndo);
            case ALTER_TABLE -> applyAlterTableLog(log, isUndo);
            default -> throw new IllegalStateException("Unexpected DDL log type: " + log.getLogType());
        }
    }

    void applyDmlLog(LogRecord log, boolean isUndo, Transaction transaction) throws IOException {
        TableInfo tableInfo = catalog.getTable(log.getTableName());
        if (tableInfo == null) {
            System.err.println(
                "WARN: Table '" + log.getTableName() + "' not found, skipping LSN=" + log.getLsn());
            return;
        }

        Schema schema = tableInfo.getSchema();
        TableHeap tableHeap = new TableHeap(pageAccess, tableInfo, logManager, lockManager);

        switch (log.getLogType()) {
            case INSERT -> applyInsertLog(log, isUndo, transaction, schema, tableHeap);
            case DELETE -> applyDeleteLog(log, isUndo, transaction, schema, tableHeap);
            case UPDATE -> applyUpdateLog(log, isUndo, transaction, schema, tableHeap);
            default -> throw new IllegalStateException("Unexpected DML log type: " + log.getLogType());
        }
    }

    private void applyCreateTableLog(LogRecord log, boolean isUndo) throws IOException {
        if (!isUndo && catalog.getTable(log.getTableName()) == null) {
            catalog.createTable(log.getTableName(), log.getSchema());
            return;
        }
        if (isUndo && catalog.getTable(log.getTableName()) != null) {
            catalog.dropTable(log.getTableName());
        }
    }

    private void applyDropTableLog(LogRecord log, boolean isUndo) throws IOException {
        if (!isUndo && catalog.getTable(log.getTableName()) != null) {
            catalog.dropTable(log.getTableName());
            return;
        }
        if (isUndo && catalog.getTable(log.getTableName()) == null) {
            catalog.createTable(log.getTableName(), log.getSchema());
        }
    }

    private void applyAlterTableLog(LogRecord log, boolean isUndo) throws IOException {
        if (!isUndo) {
            catalog.addColumn(log.getTableName(), log.getNewColumn());
        }
    }

    private void applyInsertLog(
        LogRecord log, boolean isUndo, Transaction transaction, Schema schema, TableHeap tableHeap)
        throws IOException {
        Tuple tupleToInsert = Tuple.fromBytes(log.getTupleBytes(), schema);
        tupleToInsert.setRid(log.getRid());

        if (isUndo) {
            tableHeap.deleteTuple(log.getRid(), transaction, false);
            return;
        }

        PageId pageId = new PageId(log.getRid().pageNum());
        Page page = pageAccess.getPage(pageId);
        if (log.getRid().slotIndex() >= page.getNumTuples()) {
            tableHeap.insertTuple(tupleToInsert, transaction, false, false);
        }
    }

    private void applyDeleteLog(
        LogRecord log, boolean isUndo, Transaction transaction, Schema schema, TableHeap tableHeap)
        throws IOException {
        if (isUndo) {
            Tuple tupleToRestore = Tuple.fromBytes(log.getTupleBytes(), schema);
            tupleToRestore.setRid(log.getRid());
            Tuple existingTuple = tableHeap.getTuple(log.getRid(), transaction);
            if (existingTuple == null) {
                tableHeap.insertTuple(tupleToRestore, transaction, false, false);
            }
            return;
        }

        tableHeap.deleteTuple(log.getRid(), transaction, false);
    }

    private void applyUpdateLog(
        LogRecord log, boolean isUndo, Transaction transaction, Schema schema, TableHeap tableHeap)
        throws IOException {
        Tuple oldTuple = Tuple.fromBytes(log.getOldTupleBytes(), schema);
        Tuple newTuple = Tuple.fromBytes(log.getNewTupleBytes(), schema);
        oldTuple.setRid(log.getRid());

        if (isUndo) {
            restoreDeletedTuple(log.getRid());
            deleteMatchingTuple(tableHeap, transaction, newTuple, log.getRid());
            return;
        }

        PageId pageId = new PageId(log.getRid().pageNum());
        Page page = pageAccess.getPage(pageId);
        page.markTupleAsDeleted(log.getRid().slotIndex());
        pageAccess.flushPage(pageId);

        if (!containsTuple(tableHeap, transaction, newTuple)) {
            tableHeap.insertTuple(newTuple, transaction, false, false);
        }
    }

    private boolean containsTuple(TableHeap tableHeap, Transaction transaction, Tuple expectedTuple)
        throws IOException {
        tableHeap.initIterator(transaction);
        while (tableHeap.hasNext()) {
            Tuple currentTuple = tableHeap.next();
            if (currentTuple != null && currentTuple.getValues().equals(expectedTuple.getValues())) {
                return true;
            }
        }
        return false;
    }

    private void restoreDeletedTuple(RID rid) throws IOException {
        PageId pageId = new PageId(rid.pageNum());
        Page page = pageAccess.getPage(pageId);
        if (page.undoMarkTupleAsDeleted(rid.slotIndex())) {
            pageAccess.flushPage(pageId);
        }
    }

    private void deleteMatchingTuple(
        TableHeap tableHeap, Transaction transaction, Tuple expectedTuple, RID originalRid)
        throws IOException {
        RID matchingRid = findMatchingRid(tableHeap, transaction, expectedTuple, originalRid);
        if (matchingRid != null) {
            tableHeap.deleteTuple(matchingRid, transaction, false);
        }
    }

    private RID findMatchingRid(
        TableHeap tableHeap, Transaction transaction, Tuple expectedTuple, RID originalRid)
        throws IOException {
        tableHeap.initIterator(transaction);
        while (tableHeap.hasNext()) {
            Tuple currentTuple = tableHeap.next();
            if (currentTuple == null || currentTuple.getRid() == null) {
                continue;
            }
            if (currentTuple.getRid().equals(originalRid)) {
                continue;
            }
            if (currentTuple.getValues().equals(expectedTuple.getValues())) {
                return currentTuple.getRid();
            }
        }
        return null;
    }
}

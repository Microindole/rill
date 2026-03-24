package com.indolyn.rill.core.execution.operator;

import com.indolyn.rill.core.catalog.TableInfo;
import com.indolyn.rill.core.model.RID;
import com.indolyn.rill.core.model.Schema;
import com.indolyn.rill.core.model.Tuple;
import com.indolyn.rill.core.storage.buffer.BufferPoolManager;
import com.indolyn.rill.core.storage.page.Page;
import com.indolyn.rill.core.storage.page.PageId;
import com.indolyn.rill.core.transaction.LockManager;
import com.indolyn.rill.core.transaction.Transaction;
import com.indolyn.rill.core.transaction.log.LogManager;
import com.indolyn.rill.core.transaction.log.LogRecord;

import java.io.IOException;

import lombok.Getter;

public class TableHeap implements TupleIterator {

    private final BufferPoolManager bufferPoolManager;
    private final Schema schema;
    @Getter
    private PageId firstPageId;
    private final LogManager logManager;
    @Getter
    private final LockManager lockManager;
    @Getter
    private final TableInfo tableInfo;

    private PageId currentPageId;
    private Page currentPage;
    private int currentSlotIndex;
    private Transaction iteratorTxn;

    public TableHeap(
        BufferPoolManager bufferPoolManager,
        TableInfo tableInfo,
        LogManager logManager,
        LockManager lockManager) {
        this.bufferPoolManager = bufferPoolManager;
        this.tableInfo = tableInfo;
        this.schema = tableInfo.getSchema();
        this.firstPageId = tableInfo.getFirstPageId();
        this.logManager = logManager;
        this.lockManager = lockManager;
    }

    public void initIterator(Transaction txn) throws IOException {
        this.iteratorTxn = txn;
        this.currentPageId = this.firstPageId;
        this.currentSlotIndex = 0;
        try {
            if (this.currentPageId != null && this.currentPageId.getPageNum() != -1) {
                lockManager.lockShared(iteratorTxn, currentPageId);
                this.currentPage = bufferPoolManager.getPage(this.currentPageId);
            } else {
                this.currentPage = null;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Thread interrupted while acquiring lock", e);
        }
    }

    @Override
    public Schema getOutputSchema() {
        return this.schema;
    }

    @Override
    public Tuple next() throws IOException {
        if (!hasNext()) return null;
        Tuple tuple = currentPage.getTuple(currentSlotIndex, schema);
        if (tuple != null) {
            tuple.setRid(new RID(currentPageId.getPageNum(), currentSlotIndex));
        }
        currentSlotIndex++;
        return tuple;
    }

    @Override
    public boolean hasNext() throws IOException {
        if (currentPage == null) return false;
        while (true) {
            if (currentSlotIndex < currentPage.getNumTuples()) {
                if (currentPage.getTuple(currentSlotIndex, schema) != null) {
                    return true;
                }
                currentSlotIndex++;
            } else {
                int nextPageNum = currentPage.getNextPageId();
                if (nextPageNum != -1) {
                    try {
                        PageId nextPid = new PageId(nextPageNum);
                        lockManager.lockShared(iteratorTxn, nextPid);
                        currentPageId = nextPid;
                        currentPage = bufferPoolManager.getPage(currentPageId);
                        currentSlotIndex = 0;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Thread interrupted while acquiring lock", e);
                    }
                } else {
                    return false;
                }
            }
        }
    }

    public boolean insertTuple(Tuple tuple, Transaction txn) throws IOException {
        return insertTuple(tuple, txn, true, true);
    }

    public boolean insertTuple(Tuple tuple, Transaction txn, boolean acquireLock, boolean writeLog)
        throws IOException {
        try {
            Page targetPage = findFreePageForInsert(tuple, txn, acquireLock);
            if (targetPage == null) return false;

            int slotIndexOfNewTuple = targetPage.getNumTuples();
            if (!targetPage.insertTuple(tuple)) {
                return false;
            }
            RID rid = new RID(targetPage.getPageId().getPageNum(), slotIndexOfNewTuple);
            tuple.setRid(rid);

            if (writeLog) {
                LogRecord logRecord =
                    new LogRecord(
                        txn.getTransactionId(),
                        txn.getPrevLSN(),
                        LogRecord.LogType.INSERT,
                        this.tableInfo.getTableName(),
                        rid,
                        tuple);
                long lsn = logManager.appendLogRecord(logRecord);
                txn.setPrevLSN(lsn);
            }

            bufferPoolManager.flushPage(targetPage.getPageId());
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Thread interrupted while acquiring lock", e);
        }
    }

    private Page findFreePageForInsert(Tuple tuple, Transaction txn, boolean acquireLock)
        throws IOException, InterruptedException {
        byte[] tupleBytes = tuple.toBytes();
        int requiredSpace = tupleBytes.length + 8;
        PageId pid = this.firstPageId;
        Page lastPage = null;
        while (pid != null && pid.getPageNum() != -1) {
            if (acquireLock) {
                lockManager.lockExclusive(txn, pid);
            }
            Page page = bufferPoolManager.getPage(pid);
            lastPage = page;
            if (page.getFreeSpace() >= requiredSpace) {
                return page;
            }
            int nextPageNum = page.getNextPageId();
            pid = (nextPageNum != -1) ? new PageId(nextPageNum) : null;
        }
        Page newPage = bufferPoolManager.newPage();
        if (newPage == null) return null;
        newPage.init();
        if (acquireLock) {
            lockManager.lockExclusive(txn, newPage.getPageId());
        }
        if (lastPage != null) {
            lastPage.setNextPageId(newPage.getPageId().getPageNum());
            bufferPoolManager.flushPage(lastPage.getPageId());
        } else {
            this.firstPageId = newPage.getPageId();
        }
        return newPage;
    }

    public boolean deleteTuple(RID rid, Transaction txn) throws IOException {
        return deleteTuple(rid, txn, true, true);
    }

    public boolean deleteTuple(RID rid, Transaction txn, boolean acquireLock) throws IOException {
        return deleteTuple(rid, txn, acquireLock, false);
    }

    private boolean deleteTuple(RID rid, Transaction txn, boolean acquireLock, boolean writeLog)
        throws IOException {
        try {
            PageId pageId = new PageId(rid.pageNum());
            if (acquireLock) {
                lockManager.lockExclusive(txn, pageId);
            }
            Page page = bufferPoolManager.getPage(pageId);
            Tuple oldTuple = page.getTuple(rid.slotIndex(), schema);
            if (oldTuple == null) return false;

            if (writeLog) {
                LogRecord logRecord =
                    new LogRecord(
                        txn.getTransactionId(),
                        txn.getPrevLSN(),
                        LogRecord.LogType.DELETE,
                        this.tableInfo.getTableName(),
                        rid,
                        oldTuple);
                long lsn = logManager.appendLogRecord(logRecord);
                txn.setPrevLSN(lsn);
            }

            boolean success = page.deleteTuple(rid.slotIndex());
            if (success) {
                bufferPoolManager.flushPage(page.getPageId());
            }
            return success;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Thread interrupted while acquiring lock", e);
        }
    }

    public RID updateTuple(Tuple newTuple, RID rid, Transaction txn) throws IOException {
        return updateTuple(newTuple, rid, txn, true, true);
    }

    public RID updateTuple(Tuple newTuple, RID rid, Transaction txn, boolean acquireLock)
        throws IOException {
        return updateTuple(newTuple, rid, txn, acquireLock, false);
    }

    private RID updateTuple(
        Tuple newTuple, RID rid, Transaction txn, boolean acquireLock, boolean writeLog)
        throws IOException {
        try {
            PageId pageId = new PageId(rid.pageNum());
            if (acquireLock) {
                lockManager.lockExclusive(txn, pageId);
            }
            Page page = bufferPoolManager.getPage(pageId);
            Tuple oldTuple = page.getTuple(rid.slotIndex(), schema);
            if (oldTuple == null) {
                return null;
            }

            if (writeLog) {
                LogRecord logRecord =
                    new LogRecord(
                        txn.getTransactionId(),
                        txn.getPrevLSN(),
                        LogRecord.LogType.UPDATE,
                        this.tableInfo.getTableName(),
                        rid,
                        oldTuple,
                        newTuple);
                long lsn = logManager.appendLogRecord(logRecord);
                txn.setPrevLSN(lsn);
            }

            boolean markSuccess = page.markTupleAsDeleted(rid.slotIndex());

            if (markSuccess) {
                if (insertTuple(newTuple, txn, false, false)) {
                    bufferPoolManager.flushPage(page.getPageId());
                    return newTuple.getRid();
                } else {
                    page.undoMarkTupleAsDeleted(rid.slotIndex());
                    bufferPoolManager.flushPage(page.getPageId());
                    return null;
                }
            }
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Thread interrupted while acquiring lock", e);
        }
    }

    public Tuple getTuple(RID rid, Transaction txn) throws IOException {
        try {
            PageId pageId = new PageId(rid.pageNum());
            lockManager.lockShared(txn, pageId);
            Page page = bufferPoolManager.getPage(pageId);
            Tuple tuple = page.getTuple(rid.slotIndex(), schema);
            if (tuple != null) {
                tuple.setRid(rid);
            }
            return tuple;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Thread interrupted while acquiring lock for getTuple", e);
        }
    }
}

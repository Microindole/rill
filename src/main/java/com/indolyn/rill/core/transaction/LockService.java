package com.indolyn.rill.core.transaction;

import com.indolyn.rill.core.storage.page.PageId;

public interface LockService {
    void lockShared(Transaction txn, PageId pageId) throws InterruptedException;

    void lockExclusive(Transaction txn, PageId pageId) throws InterruptedException;

    void unlock(Transaction txn, PageId pageId);
}

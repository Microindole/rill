package com.indolyn.rill.core.infrastructure.transaction;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.indolyn.rill.core.storage.page.PageId;
import com.indolyn.rill.core.transaction.LockManager;
import com.indolyn.rill.core.transaction.Transaction;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

class LockManagerBaselineTest {

    @Test
    void sharedLockShouldBeRecordedAndReleased() throws Exception {
        LockManager lockManager = new LockManager();
        Transaction transaction = new Transaction(100);
        PageId pageId = new PageId(7);

        lockManager.lockShared(transaction, pageId);

        assertTrue(transaction.getLockedPageIds().contains(7));

        lockManager.unlock(transaction, pageId);

        assertFalse(transaction.getLockedPageIds().contains(7));
    }

    @Test
    void exclusiveLockShouldWaitUntilSharedLockIsReleased() throws Exception {
        LockManager lockManager = new LockManager();
        Transaction firstTransaction = new Transaction(1);
        Transaction secondTransaction = new Transaction(2);
        PageId pageId = new PageId(9);

        lockManager.lockShared(firstTransaction, pageId);

        CountDownLatch acquiredSignal = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future =
            executor.submit(
                () -> {
                    try {
                        lockManager.lockExclusive(secondTransaction, pageId);
                        acquiredSignal.countDown();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });

        try {
            assertFalse(acquiredSignal.await(200, TimeUnit.MILLISECONDS));

            lockManager.unlock(firstTransaction, pageId);

            assertTrue(acquiredSignal.await(2, TimeUnit.SECONDS));
            assertTrue(secondTransaction.getLockedPageIds().contains(9));
        } finally {
            lockManager.unlock(secondTransaction, pageId);
            future.cancel(true);
            executor.shutdownNow();
        }
    }
}

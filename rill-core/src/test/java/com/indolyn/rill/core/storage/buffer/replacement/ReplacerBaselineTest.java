package com.indolyn.rill.core.storage.buffer.replacement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.indolyn.rill.core.storage.page.PageId;

import org.junit.jupiter.api.Test;

class ReplacerBaselineTest {

    @Test
    void lruReplacerShouldEvictLeastRecentlyUsedPage() {
        LRUReplacer replacer = new LRUReplacer();

        replacer.pin(new PageId(1));
        replacer.pin(new PageId(2));
        replacer.pin(new PageId(1));

        assertEquals(new PageId(2), replacer.unpin());
        assertEquals(new PageId(1), replacer.unpin());
        assertNull(replacer.unpin());
    }

    @Test
    void fifoReplacerShouldEvictInInsertionOrder() {
        FIFOReplacer replacer = new FIFOReplacer();

        replacer.pin(new PageId(1));
        replacer.pin(new PageId(2));
        replacer.pin(new PageId(1));

        assertEquals(new PageId(1), replacer.unpin());
        assertEquals(new PageId(2), replacer.unpin());
        assertNull(replacer.unpin());
    }

    @Test
    void clockReplacerShouldGivePinnedPagesASecondChance() {
        ClockReplacer replacer = new ClockReplacer();

        replacer.pin(new PageId(1));
        replacer.pin(new PageId(2));

        assertEquals(new PageId(1), replacer.unpin());
        assertEquals(new PageId(2), replacer.unpin());
        assertNull(replacer.unpin());
    }
}

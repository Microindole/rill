package com.indolyn.rill.core.storage.buffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.indolyn.rill.core.storage.disk.DiskManager;
import com.indolyn.rill.core.storage.page.Page;
import com.indolyn.rill.core.storage.page.PageId;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BufferPoolManagerBaselineTest {

    @TempDir
    Path tempDir;

    @Test
    void bufferPoolManagerShouldTrackHitsMissesAndReloadEvictedPages() throws Exception {
        Path dbPath = tempDir.resolve("rill.data");
        DiskManager diskManager = new DiskManager(dbPath.toString());
        diskManager.open();
        try {
            BufferPoolManager bufferPoolManager = new BufferPoolManager(1, diskManager, "LRU");

            Page firstPage = bufferPoolManager.newPage();
            firstPage.getData().putInt(64, 11);
            bufferPoolManager.flushPage(firstPage.getPageId());

            bufferPoolManager.resetStats();
            PageId firstPageId = firstPage.getPageId();

            Page hitRead = bufferPoolManager.getPage(firstPageId);

            assertNotNull(hitRead);
            assertEquals(1, bufferPoolManager.getHitCount());

            Page secondPage = bufferPoolManager.newPage();
            secondPage.getData().putInt(64, 22);
            bufferPoolManager.flushPage(secondPage.getPageId());
            assertTrue(bufferPoolManager.getPageTable().containsKey(secondPage.getPageId()));

            Page reloadedFirstPage = bufferPoolManager.getPage(firstPageId);
            assertEquals(1, bufferPoolManager.getMissCount());
            assertEquals(11, reloadedFirstPage.getData().getInt(64));
        } finally {
            diskManager.close();
        }
    }
}

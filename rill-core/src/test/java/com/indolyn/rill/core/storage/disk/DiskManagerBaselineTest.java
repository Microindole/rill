package com.indolyn.rill.core.storage.disk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.indolyn.rill.core.storage.page.Page;
import com.indolyn.rill.core.storage.page.PageId;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DiskManagerBaselineTest {

    @TempDir
    Path tempDir;

    @Test
    void diskManagerShouldPersistPageContentAndReuseDeallocatedPageIds() throws Exception {
        Path dbPath = tempDir.resolve("rill.data");
        DiskManager diskManager = new DiskManager(dbPath.toString());
        diskManager.open();
        try {
            PageId firstPageId = diskManager.allocatePage();
            assertEquals(0, firstPageId.getPageNum());

            Page page = new Page(firstPageId);
            page.getData().putInt(128, 2026);
            diskManager.writePage(page);

            Page readBack = diskManager.readPage(firstPageId);
            assertEquals(2026, readBack.getData().getInt(128));

            diskManager.deallocatePage(firstPageId);
        } finally {
            diskManager.close();
        }

        DiskManager reopened = new DiskManager(dbPath.toString());
        reopened.open();
        try {
            PageId reusedPageId = reopened.allocatePage();
            assertEquals(0, reusedPageId.getPageNum());
            assertTrue(reopened.getFileLength() >= Page.PAGE_SIZE);
        } finally {
            reopened.close();
        }
    }
}

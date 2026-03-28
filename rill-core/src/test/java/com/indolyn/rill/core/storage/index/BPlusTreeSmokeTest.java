package com.indolyn.rill.core.storage.index;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.indolyn.rill.core.model.RID;
import com.indolyn.rill.core.model.Value;
import com.indolyn.rill.core.storage.buffer.BufferPoolManager;
import com.indolyn.rill.core.storage.disk.DiskManager;
import com.indolyn.rill.core.storage.page.Page;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BPlusTreeSmokeTest {

    @TempDir
    Path tempDir;

    @Test
    void bPlusTreeShouldInsertSearchAndDeleteKeys() throws Exception {
        Path dbPath = tempDir.resolve("rill.data");
        DiskManager diskManager = new DiskManager(dbPath.toString());
        diskManager.open();
        try {
            BufferPoolManager bufferPoolManager = new BufferPoolManager(8, diskManager, "LRU");
            Page rootPage = bufferPoolManager.newPage();
            BPlusTree tree = new BPlusTree(bufferPoolManager, rootPage.getPageId().getPageNum());

            tree.insert(new Value(5), new RID(1, 1));
            tree.insert(new Value(10), new RID(1, 2));
            tree.insert(new Value(20), new RID(1, 3));

            assertFalse(tree.isEmpty());
            assertEquals(new RID(1, 1), tree.search(new Value(5)));
            assertEquals(new RID(1, 2), tree.search(new Value(10)));
            assertEquals(new RID(1, 3), tree.search(new Value(20)));

            assertTrue(tree.delete(new Value(10)));
            assertNull(tree.search(new Value(10)));
        } finally {
            diskManager.close();
        }
    }
}

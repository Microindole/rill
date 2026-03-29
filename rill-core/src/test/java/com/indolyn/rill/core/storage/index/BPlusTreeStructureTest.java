package com.indolyn.rill.core.storage.index;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.indolyn.rill.core.model.RID;
import com.indolyn.rill.core.model.Value;
import com.indolyn.rill.core.storage.buffer.BufferPoolManager;
import com.indolyn.rill.core.storage.disk.DiskManager;
import com.indolyn.rill.core.storage.page.Page;
import com.indolyn.rill.core.storage.page.PageId;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BPlusTreeStructureTest {

    @TempDir
    Path tempDir;

    @Test
    void insertsShouldSplitIntoInternalRootAndKeepSearchableLeaves() throws Exception {
        Path dbPath = tempDir.resolve("split-rill.data");
        DiskManager diskManager = new DiskManager(dbPath.toString());
        diskManager.open();
        try {
            BufferPoolManager bufferPoolManager = new BufferPoolManager(32, diskManager, "LRU");
            Page rootPage = bufferPoolManager.newPage();
            BPlusTree tree = new BPlusTree(bufferPoolManager, rootPage.getPageId().getPageNum());

            for (int i = 1; i <= 500; i++) {
                tree.insert(new Value(i), new RID(7, i));
            }

            BPlusTreeNodePage rootNode =
                loadNode(bufferPoolManager, tree.getRootPageId());
            assertTrue(rootNode instanceof BPlusTreeInternalPage);

            for (int i = 1; i <= 500; i++) {
                assertEquals(new RID(7, i), tree.search(new Value(i)));
            }
        } finally {
            diskManager.close();
        }
    }

    @Test
    void deletesShouldTriggerRebalanceAndPreserveRemainingKeys() throws Exception {
        Path dbPath = tempDir.resolve("merge-rill.data");
        DiskManager diskManager = new DiskManager(dbPath.toString());
        diskManager.open();
        try {
            BufferPoolManager bufferPoolManager = new BufferPoolManager(32, diskManager, "LRU");
            Page rootPage = bufferPoolManager.newPage();
            BPlusTree tree = new BPlusTree(bufferPoolManager, rootPage.getPageId().getPageNum());

            for (int i = 1; i <= 500; i++) {
                tree.insert(new Value(i), new RID(9, i));
            }
            for (int i = 1; i <= 350; i++) {
                assertTrue(tree.delete(new Value(i)));
            }

            for (int i = 1; i <= 350; i++) {
                assertNull(tree.search(new Value(i)));
            }
            for (int i = 351; i <= 500; i++) {
                assertEquals(new RID(9, i), tree.search(new Value(i)));
            }
        } finally {
            diskManager.close();
        }
    }

    private BPlusTreeNodePage loadNode(BufferPoolManager bufferPoolManager, int pageId)
        throws Exception {
        Page page = bufferPoolManager.getPage(new PageId(pageId));
        BPlusTreeLeafPage probe = new BPlusTreeLeafPage(page);
        if (probe.getNodeType() == BPlusTreeNodePage.NodeType.LEAF) {
            return probe;
        }
        return new BPlusTreeInternalPage(page);
    }
}

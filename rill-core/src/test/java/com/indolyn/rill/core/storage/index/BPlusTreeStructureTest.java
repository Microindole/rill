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
import java.util.ArrayList;
import java.util.List;

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

    @Test
    void leafChainShouldRemainSortedAfterMultipleSplits() throws Exception {
        Path dbPath = tempDir.resolve("leaf-chain-rill.data");
        DiskManager diskManager = new DiskManager(dbPath.toString());
        diskManager.open();
        try {
            BufferPoolManager bufferPoolManager = new BufferPoolManager(32, diskManager, "LRU");
            Page rootPage = bufferPoolManager.newPage();
            BPlusTree tree = new BPlusTree(bufferPoolManager, rootPage.getPageId().getPageNum());

            for (int i = 300; i >= 1; i--) {
                tree.insert(new Value(i), new RID(11, i));
            }

            List<Integer> keysInLeafOrder = readKeysInLeafOrder(bufferPoolManager, tree.getRootPageId());

            assertEquals(300, keysInLeafOrder.size());
            for (int i = 1; i <= 300; i++) {
                assertEquals(i, keysInLeafOrder.get(i - 1));
            }
        } finally {
            diskManager.close();
        }
    }

    @Test
    void rootShouldShrinkBackToLeafAfterHeavyDeletion() throws Exception {
        Path dbPath = tempDir.resolve("root-shrink-rill.data");
        DiskManager diskManager = new DiskManager(dbPath.toString());
        diskManager.open();
        try {
            BufferPoolManager bufferPoolManager = new BufferPoolManager(32, diskManager, "LRU");
            Page rootPage = bufferPoolManager.newPage();
            BPlusTree tree = new BPlusTree(bufferPoolManager, rootPage.getPageId().getPageNum());

            for (int i = 1; i <= 300; i++) {
                tree.insert(new Value(i), new RID(13, i));
            }
            for (int i = 1; i <= 298; i++) {
                assertTrue(tree.delete(new Value(i)));
            }

            BPlusTreeNodePage rootNode = loadNode(bufferPoolManager, tree.getRootPageId());
            assertTrue(rootNode instanceof BPlusTreeLeafPage);
            assertEquals(new RID(13, 299), tree.search(new Value(299)));
            assertEquals(new RID(13, 300), tree.search(new Value(300)));
        } finally {
            diskManager.close();
        }
    }

    @Test
    void deleteOfMissingKeyShouldLeaveExistingEntriesUntouched() throws Exception {
        Path dbPath = tempDir.resolve("delete-missing-rill.data");
        DiskManager diskManager = new DiskManager(dbPath.toString());
        diskManager.open();
        try {
            BufferPoolManager bufferPoolManager = new BufferPoolManager(16, diskManager, "LRU");
            Page rootPage = bufferPoolManager.newPage();
            BPlusTree tree = new BPlusTree(bufferPoolManager, rootPage.getPageId().getPageNum());

            for (int i = 1; i <= 120; i++) {
                tree.insert(new Value(i), new RID(15, i));
            }

            assertNull(tree.search(new Value(999)));
            assertTrue(!tree.delete(new Value(999)));
            for (int i = 1; i <= 120; i++) {
                assertEquals(new RID(15, i), tree.search(new Value(i)));
            }
        } finally {
            diskManager.close();
        }
    }

    @Test
    void treeShouldRemainSearchableAfterShrinkAndReinsert() throws Exception {
        Path dbPath = tempDir.resolve("shrink-reinsert-rill.data");
        DiskManager diskManager = new DiskManager(dbPath.toString());
        diskManager.open();
        try {
            BufferPoolManager bufferPoolManager = new BufferPoolManager(32, diskManager, "LRU");
            Page rootPage = bufferPoolManager.newPage();
            BPlusTree tree = new BPlusTree(bufferPoolManager, rootPage.getPageId().getPageNum());

            for (int i = 1; i <= 240; i++) {
                tree.insert(new Value(i), new RID(17, i));
            }
            for (int i = 1; i <= 230; i++) {
                assertTrue(tree.delete(new Value(i)));
            }
            for (int i = 241; i <= 280; i++) {
                tree.insert(new Value(i), new RID(17, i));
            }

            List<Integer> keysInLeafOrder = readKeysInLeafOrder(bufferPoolManager, tree.getRootPageId());

            assertEquals(50, keysInLeafOrder.size());
            for (int i = 231; i <= 280; i++) {
                assertEquals(new RID(17, i), tree.search(new Value(i)));
            }
            assertEquals(231, keysInLeafOrder.getFirst());
            assertEquals(280, keysInLeafOrder.getLast());
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

    private List<Integer> readKeysInLeafOrder(BufferPoolManager bufferPoolManager, int rootPageId)
        throws Exception {
        BPlusTreeNodePage currentNode = loadNode(bufferPoolManager, rootPageId);
        while (currentNode instanceof BPlusTreeInternalPage internalPage) {
            currentNode = loadNode(bufferPoolManager, internalPage.getChildPageId(0));
        }

        List<Integer> orderedKeys = new ArrayList<>();
        BPlusTreeLeafPage leafPage = (BPlusTreeLeafPage) currentNode;
        while (leafPage != null) {
            for (int i = 0; i < leafPage.getKeyCount(); i++) {
                orderedKeys.add((Integer) leafPage.getKey(i).getValue());
            }
            int nextLeafPageId = leafPage.getNextLeafPageId();
            if (nextLeafPageId == -1) {
                leafPage = null;
            } else {
                leafPage = (BPlusTreeLeafPage) loadNode(bufferPoolManager, nextLeafPageId);
            }
        }
        return orderedKeys;
    }
}

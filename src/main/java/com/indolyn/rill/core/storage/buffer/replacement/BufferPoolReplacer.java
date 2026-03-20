package com.indolyn.rill.core.storage.buffer.replacement;


import com.indolyn.rill.core.storage.page.PageId;

/**
 * 缓存替换策略接口。
 */
public interface BufferPoolReplacer {
    /**
     * 将一个页标记为已使用（pin），不参与淘汰。
     * @param pageId
     */
    void pin(PageId pageId);

    /**
     * 找到一个可以淘汰的页，并将其标记为可淘汰（unpin）。
     * @return 可淘汰的页ID，如果没有则返回null。
     */
    PageId unpin();

    void remove(PageId pageId);
}


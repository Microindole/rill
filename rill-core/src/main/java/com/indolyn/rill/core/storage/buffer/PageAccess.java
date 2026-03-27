package com.indolyn.rill.core.storage.buffer;

import com.indolyn.rill.core.storage.page.Page;
import com.indolyn.rill.core.storage.page.PageId;

import java.io.IOException;

public interface PageAccess {
    Page getPage(PageId pageId) throws IOException;

    Page newPage() throws IOException;

    void flushPage(PageId pageId) throws IOException;

    void flushAllPages() throws IOException;

    boolean deletePage(PageId pageId) throws IOException;
}

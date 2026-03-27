package com.indolyn.rill.core.catalog;

import com.indolyn.rill.core.model.Schema;
import com.indolyn.rill.core.model.Tuple;
import com.indolyn.rill.core.storage.buffer.BufferPoolManager;
import com.indolyn.rill.core.storage.buffer.PageAccess;
import com.indolyn.rill.core.storage.database.DatabasePathResolver;
import com.indolyn.rill.core.storage.disk.DiskManager;
import com.indolyn.rill.core.storage.page.Page;
import com.indolyn.rill.core.storage.page.PageId;

import java.io.IOException;
import java.util.List;

final class DefaultPermissionReloadAccess implements PermissionReloadAccess {
    private static final String DEFAULT_AUTH_DATABASE = "default";
    private static final int DEFAULT_BUFFER_POOL_SIZE = 10;
    private static final String DEFAULT_REPLACEMENT_POLICY = "LRU";
    private final DatabasePathResolver pathResolver;

    DefaultPermissionReloadAccess(DatabasePathResolver pathResolver) {
        this.pathResolver = pathResolver;
    }

    @Override
    public void reload(
        PermissionRegistry permissionRegistry,
        Schema tablesTableSchema,
        Schema usersTableSchema,
        Schema privilegesTableSchema)
        throws IOException {
        permissionRegistry.clear();

        DiskManager diskManager = null;
        try {
            String defaultDbPath = pathResolver.resolveDatabaseFilePath(DEFAULT_AUTH_DATABASE);
            diskManager = new DiskManager(defaultDbPath);
            diskManager.open();

            BufferPoolManager bufferPoolManager =
                new BufferPoolManager(
                    DEFAULT_BUFFER_POOL_SIZE, diskManager, DEFAULT_REPLACEMENT_POLICY);
            PageAccess pageAccess = bufferPoolManager;
            Page tablesPage = pageAccess.getPage(new PageId(0));
            List<Tuple> catalogTuples = tablesPage.getAllTuples(tablesTableSchema);

            PageId usersPageId = null;
            PageId privilegesPageId = null;
            for (Tuple tuple : catalogTuples) {
                String tableName = (String) tuple.getValues().get(1).getValue();
                int pageId = (int) tuple.getValues().get(2).getValue();
                if (Catalog.CATALOG_USERS_TABLE_NAME.equals(tableName)) {
                    usersPageId = new PageId(pageId);
                }
                if (Catalog.CATALOG_PRIVILEGES_TABLE_NAME.equals(tableName)) {
                    privilegesPageId = new PageId(pageId);
                }
            }

            if (usersPageId == null || privilegesPageId == null) {
                System.err.println("[Catalog.forceReload] 警告: 在 'default' 数据库中找不到用户或权限系统表。");
                return;
            }

            UserDirectoryStore reloadStore =
                new UserDirectoryStore(pageAccess, permissionRegistry);
            reloadStore.loadUsers(usersPageId, usersTableSchema);
            reloadStore.loadPrivileges(
                usersPageId, usersTableSchema, privilegesPageId, privilegesTableSchema);
        } finally {
            if (diskManager != null) {
                diskManager.close();
            }
        }
    }
}

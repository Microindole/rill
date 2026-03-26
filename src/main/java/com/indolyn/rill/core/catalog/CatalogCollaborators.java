package com.indolyn.rill.core.catalog;

import com.indolyn.rill.core.storage.buffer.PageAccess;
import com.indolyn.rill.core.storage.database.DatabasePathResolver;

final class CatalogCollaborators {
    private final PermissionRegistry permissionRegistry;
    private final PermissionReloadAccess permissionReloadAccess;
    private final CatalogMetadataAccess metadataAccess;
    private final IndexCatalogAccess indexAccess;
    private final UserDirectoryAccess userDirectoryAccess;

    private CatalogCollaborators(
        PermissionRegistry permissionRegistry,
        PermissionReloadAccess permissionReloadAccess,
        CatalogMetadataAccess metadataAccess,
        IndexCatalogAccess indexAccess,
        UserDirectoryAccess userDirectoryAccess) {
        this.permissionRegistry = permissionRegistry;
        this.permissionReloadAccess = permissionReloadAccess;
        this.metadataAccess = metadataAccess;
        this.indexAccess = indexAccess;
        this.userDirectoryAccess = userDirectoryAccess;
    }

    static CatalogCollaborators createDefault(PageAccess pageAccess, DatabasePathResolver pathResolver) {
        PermissionRegistry permissionRegistry = new PermissionRegistry();
        return new CatalogCollaborators(
            permissionRegistry,
            new DefaultPermissionReloadAccess(pathResolver),
            new CatalogMetadataStore(pageAccess),
            new IndexRegistry(),
            new UserDirectoryStore(pageAccess, permissionRegistry));
    }

    PermissionRegistry permissionRegistry() {
        return permissionRegistry;
    }

    PermissionReloadAccess permissionReloadAccess() {
        return permissionReloadAccess;
    }

    CatalogMetadataAccess metadataAccess() {
        return metadataAccess;
    }

    IndexCatalogAccess indexAccess() {
        return indexAccess;
    }

    UserDirectoryAccess userDirectoryAccess() {
        return userDirectoryAccess;
    }
}

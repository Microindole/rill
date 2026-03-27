package com.indolyn.rill.core.catalog;

import com.indolyn.rill.core.model.Schema;

import java.io.IOException;

interface PermissionReloadAccess {
    void reload(
        PermissionRegistry permissionRegistry,
        Schema tablesTableSchema,
        Schema usersTableSchema,
        Schema privilegesTableSchema)
        throws IOException;
}

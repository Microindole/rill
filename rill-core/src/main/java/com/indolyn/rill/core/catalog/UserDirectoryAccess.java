package com.indolyn.rill.core.catalog;

import com.indolyn.rill.core.model.Schema;
import com.indolyn.rill.core.storage.page.PageId;

import java.io.IOException;

interface UserDirectoryAccess {
    void loadUsers(PageId usersTableFirstPageId, Schema usersTableSchema) throws IOException;

    void loadPrivileges(
        PageId usersTableFirstPageId,
        Schema usersTableSchema,
        PageId privilegesTableFirstPageId,
        Schema privilegesTableSchema)
        throws IOException;

    void bootstrapDefaultUsers(PageId usersTableFirstPageId, PageId privilegesTableFirstPageId)
        throws IOException;

    void createUser(PageId usersTableFirstPageId, String username, String password)
        throws IOException;

    void grantPrivilege(
        PageId privilegesTableFirstPageId, String username, String tableName, String privilegeType)
        throws IOException;
}

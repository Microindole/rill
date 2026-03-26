package com.indolyn.rill.core.catalog;

import com.indolyn.rill.core.model.Schema;
import com.indolyn.rill.core.model.Tuple;
import com.indolyn.rill.core.model.Value;
import com.indolyn.rill.core.storage.buffer.PageAccess;
import com.indolyn.rill.core.storage.page.Page;
import com.indolyn.rill.core.storage.page.PageId;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

final class UserDirectoryStore implements UserDirectoryAccess {
    private final PageAccess pageAccess;
    private final PermissionRegistry permissionRegistry;

    UserDirectoryStore(PageAccess pageAccess, PermissionRegistry permissionRegistry) {
        this.pageAccess = pageAccess;
        this.permissionRegistry = permissionRegistry;
    }

    @Override
    public void loadUsers(PageId usersTableFirstPageId, Schema usersTableSchema) throws IOException {
        Page usersPage = pageAccess.getPage(usersTableFirstPageId);
        List<Tuple> userTuples = usersPage.getAllTuples(usersTableSchema);
        for (Tuple userTuple : userTuples) {
            int userId = (int) userTuple.getValues().get(0).getValue();
            String username = (String) userTuple.getValues().get(1).getValue();
            String passwordHash = (String) userTuple.getValues().get(2).getValue();
            permissionRegistry.registerUser(username, userId, passwordHash);
        }
    }

    @Override
    public void loadPrivileges(
        PageId usersTableFirstPageId,
        Schema usersTableSchema,
        PageId privilegesTableFirstPageId,
        Schema privilegesTableSchema)
        throws IOException {
        Page usersPage = pageAccess.getPage(usersTableFirstPageId);
        List<Tuple> userTuples = usersPage.getAllTuples(usersTableSchema);
        Page privilegesPage = pageAccess.getPage(privilegesTableFirstPageId);
        List<Tuple> privilegeTuples = privilegesPage.getAllTuples(privilegesTableSchema);

        for (Tuple privilegeTuple : privilegeTuples) {
            int userId = (int) privilegeTuple.getValues().get(1).getValue();
            String tableName = (String) privilegeTuple.getValues().get(2).getValue();
            String privilegeType = (String) privilegeTuple.getValues().get(3).getValue();
            String username = findUsernameById(userId, userTuples);
            if (username != null) {
                permissionRegistry.registerPrivilege(username, tableName, privilegeType);
            }
        }
    }

    @Override
    public void bootstrapDefaultUsers(PageId usersTableFirstPageId, PageId privilegesTableFirstPageId)
        throws IOException {
        Page usersPage = pageAccess.getPage(usersTableFirstPageId);
        Page privilegesPage = pageAccess.getPage(privilegesTableFirstPageId);

        String rootPasswordHash = permissionRegistry.hashPassword("root_password");
        usersPage.insertTuple(
            new Tuple(Arrays.asList(new Value(0), new Value("root"), new Value(rootPasswordHash))));

        privilegesPage.insertTuple(
            new Tuple(Arrays.asList(new Value(0), new Value(0), new Value("*"), new Value("ALL"))));

        String testPasswordHash = permissionRegistry.hashPassword("123");
        usersPage.insertTuple(
            new Tuple(Arrays.asList(new Value(1), new Value("testuser"), new Value(testPasswordHash))));

        pageAccess.flushPage(usersTableFirstPageId);
        pageAccess.flushPage(privilegesTableFirstPageId);

        permissionRegistry.registerUser("root", 0, rootPasswordHash);
        permissionRegistry.registerPrivilege("root", "*", "ALL");
        permissionRegistry.registerUser("testuser", 1, testPasswordHash);
    }

    @Override
    public void createUser(PageId usersTableFirstPageId, String username, String password)
        throws IOException {
        int newUserId = permissionRegistry.nextUserId();
        String passwordHash = permissionRegistry.hashPassword(password);
        Tuple userTuple =
            new Tuple(Arrays.asList(new Value(newUserId), new Value(username), new Value(passwordHash)));

        Page usersPage = pageAccess.getPage(usersTableFirstPageId);
        if (!usersPage.insertTuple(userTuple)) {
            throw new IOException(
                "Failed to insert new user into users catalog page. Page might be full.");
        }
        pageAccess.flushPage(usersTableFirstPageId);
        permissionRegistry.registerUser(username, newUserId, passwordHash);
    }

    @Override
    public void grantPrivilege(
        PageId privilegesTableFirstPageId, String username, String tableName, String privilegeType)
        throws IOException {
        Integer userId = permissionRegistry.getUserId(username);
        if (userId == null) {
            throw new IllegalStateException("User '" + username + "' not found in memory cache.");
        }

        int newPrivilegeId = permissionRegistry.nextPrivilegeId();
        Tuple privilegeTuple =
            new Tuple(
                Arrays.asList(
                    new Value(newPrivilegeId),
                    new Value(userId),
                    new Value(tableName),
                    new Value(privilegeType.toUpperCase())));

        Page privilegesPage = pageAccess.getPage(privilegesTableFirstPageId);
        if (!privilegesPage.insertTuple(privilegeTuple)) {
            throw new IOException(
                "Failed to insert new privilege into privileges catalog page. Page might be full.");
        }
        pageAccess.flushPage(privilegesTableFirstPageId);
        permissionRegistry.registerPrivilege(username, tableName, privilegeType);
    }

    private String findUsernameById(int userId, List<Tuple> userTuples) {
        for (Tuple userTuple : userTuples) {
            if ((int) userTuple.getValues().get(0).getValue() == userId) {
                return (String) userTuple.getValues().get(1).getValue();
            }
        }
        return null;
    }
}

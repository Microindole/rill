package com.indolyn.rill.core.catalog;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

final class PermissionRegistry {
    private final Map<String, byte[]> users = new ConcurrentHashMap<>();
    private final Map<String, List<PrivilegeInfo>> userPrivileges = new ConcurrentHashMap<>();
    private final Map<String, Integer> userIds = new ConcurrentHashMap<>();
    private final AtomicInteger nextPrivilegeId = new AtomicInteger(0);

    byte[] getPasswordHash(String username) {
        return users.get(username);
    }

    boolean containsUser(String username) {
        return users.containsKey(username);
    }

    int nextUserId() {
        return userIds.size();
    }

    Integer getUserId(String username) {
        return userIds.get(username);
    }

    int nextPrivilegeId() {
        return nextPrivilegeId.getAndIncrement();
    }

    void registerUser(String username, int userId, String passwordHash) {
        users.put(username, passwordHash.getBytes(StandardCharsets.UTF_8));
        userIds.put(username, userId);
    }

    void registerPrivilege(String username, String tableName, String privilegeType) {
        userPrivileges
            .computeIfAbsent(username, key -> new ArrayList<>())
            .add(new PrivilegeInfo(tableName, privilegeType.toUpperCase()));
    }

    void clear() {
        users.clear();
        userIds.clear();
        userPrivileges.clear();
    }

    boolean hasPermission(String username, String tableName, String privilegeType) {
        if (username == null) {
            return false;
        }

        List<PrivilegeInfo> privileges = userPrivileges.get(username);
        if (privileges == null) {
            return false;
        }

        return privileges.stream().anyMatch(p -> p.matches(tableName, privilegeType));
    }

    String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encodedHash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    static final class PrivilegeInfo {
        private final String tableName;
        private final String privilegeType;

        private PrivilegeInfo(String tableName, String privilegeType) {
            this.tableName = tableName;
            this.privilegeType = privilegeType;
        }

        private boolean matches(String requestedTableName, String requestedPrivilegeType) {
            if ("*".equals(tableName) && "ALL".equalsIgnoreCase(privilegeType)) {
                return true;
            }
            if (tableName.equalsIgnoreCase(requestedTableName)
                && "ALL".equalsIgnoreCase(privilegeType)) {
                return true;
            }
            return tableName.equalsIgnoreCase(requestedTableName)
                && privilegeType.equalsIgnoreCase(requestedPrivilegeType);
        }
    }
}

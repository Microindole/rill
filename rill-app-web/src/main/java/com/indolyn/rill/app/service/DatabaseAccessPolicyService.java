package com.indolyn.rill.app.service;

import com.indolyn.rill.app.persistence.entity.AppUserEntity;

import java.util.List;

public interface DatabaseAccessPolicyService {

    List<String> accessibleDatabases(AppUserEntity user, List<String> loadedDatabases);

    String defaultDatabase(AppUserEntity user);

    void assertCanUseDatabase(AppUserEntity user, String databaseName, List<String> loadedDatabases);

    void assertCanExecute(AppUserEntity user, String currentDatabase, String sql, List<String> loadedDatabases);

    boolean isAdmin(AppUserEntity user);

    boolean isGuest(AppUserEntity user);
}

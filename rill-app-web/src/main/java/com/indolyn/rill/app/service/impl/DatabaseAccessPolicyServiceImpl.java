package com.indolyn.rill.app.service.impl;

import com.indolyn.rill.app.persistence.entity.AppUserEntity;
import com.indolyn.rill.app.service.DatabaseAccessPolicyService;
import com.indolyn.rill.core.sql.ast.StatementNode;
import com.indolyn.rill.core.sql.ast.statement.CreateDatabaseStatementNode;
import com.indolyn.rill.core.sql.ast.statement.DropDatabaseStatementNode;
import com.indolyn.rill.core.sql.ast.statement.UseDatabaseStatementNode;
import com.indolyn.rill.core.sql.lexer.Lexer;
import com.indolyn.rill.core.sql.parser.Parser;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DatabaseAccessPolicyServiceImpl implements DatabaseAccessPolicyService {

    @Override
    public List<String> accessibleDatabases(AppUserEntity user, List<String> loadedDatabases) {
        Set<String> normalizedLoaded = normalizeLoadedDatabases(loadedDatabases);
        normalizedLoaded.add("default");

        if (isAdmin(user)) {
            if (user != null && user.getKernelDbName() != null && !user.getKernelDbName().isBlank()) {
                normalizedLoaded.add(normalizeDbName(user.getKernelDbName()));
            }
            return new ArrayList<>(normalizedLoaded);
        }

        if (isGuest(user)) {
            return List.of("default");
        }

        LinkedHashSet<String> accessible = new LinkedHashSet<>();
        accessible.add("default");
        if (user != null) {
            if (user.getKernelDbName() != null && !user.getKernelDbName().isBlank()) {
                accessible.add(normalizeDbName(user.getKernelDbName()));
            }
            if (user.getUsername() != null && !user.getUsername().isBlank()) {
                accessible.add(normalizeDbName(user.getUsername()));
            }
        }
        return new ArrayList<>(accessible);
    }

    @Override
    public String defaultDatabase(AppUserEntity user) {
        if (isGuest(user)) {
            return "default";
        }
        if (user.getKernelDbName() != null && !user.getKernelDbName().isBlank()) {
            return user.getKernelDbName();
        }
        return "default";
    }

    @Override
    public void assertCanUseDatabase(AppUserEntity user, String databaseName, List<String> loadedDatabases) {
        String normalized = normalizeDbName(databaseName);
        if (!accessibleDatabases(user, loadedDatabases).contains(normalized)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Current user cannot access database '" + normalized + "'");
        }
    }

    @Override
    public void assertCanExecute(AppUserEntity user, String currentDatabase, String sql, List<String> loadedDatabases) {
        assertCanUseDatabase(user, currentDatabase, loadedDatabases);
        if (sql == null || sql.isBlank()) {
            return;
        }
        StatementNode statement = new Parser(new Lexer(sql.trim()).tokenize()).parse();
        if (statement instanceof CreateDatabaseStatementNode createDatabaseStatementNode) {
            if (!canCreateDatabase(user, createDatabaseStatementNode.databaseName().getName())) {
                throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Current user can only create own database");
            }
            return;
        }
        if (statement instanceof DropDatabaseStatementNode dropDatabaseStatementNode) {
            if (!canDropDatabase(user, dropDatabaseStatementNode.databaseName().getName())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only root can drop databases");
            }
            return;
        }
        if (statement instanceof UseDatabaseStatementNode useDatabaseStatementNode) {
            assertCanUseDatabase(user, useDatabaseStatementNode.databaseName().getName(), loadedDatabases);
        }
    }

    @Override
    public boolean isAdmin(AppUserEntity user) {
        return user != null && "ADMIN".equalsIgnoreCase(user.getRole());
    }

    @Override
    public boolean isGuest(AppUserEntity user) {
        return user != null && "GUEST".equalsIgnoreCase(user.getRole());
    }

    private boolean canCreateDatabase(AppUserEntity user, String databaseName) {
        if (user == null) {
            return false;
        }
        if ("root".equalsIgnoreCase(user.getUsername())) {
            return true;
        }
        String normalized = normalizeDbName(databaseName);
        return normalized.equals(normalizeDbName(user.getUsername()))
            || normalized.equals(normalizeDbName(user.getKernelDbName()));
    }

    private boolean canDropDatabase(AppUserEntity user, String databaseName) {
        return "root".equalsIgnoreCase(user == null ? null : user.getUsername());
    }

    private String normalizeDbName(String dbName) {
        if (dbName == null || dbName.isBlank()) {
            return "default";
        }
        return dbName.trim().toLowerCase(Locale.ROOT);
    }

    private Set<String> normalizeLoadedDatabases(List<String> loadedDatabases) {
        Set<String> normalized = new LinkedHashSet<>();
        if (loadedDatabases == null) {
            return normalized;
        }
        for (String db : loadedDatabases) {
            if (db == null || db.isBlank()) {
                continue;
            }
            normalized.add(normalizeDbName(db));
        }
        return normalized;
    }
}

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
        if (isAdmin(user)) {
            return loadedDatabases;
        }
        Set<String> databases = new LinkedHashSet<>();
        databases.add("default");
        if (!isGuest(user) && user.getKernelDbName() != null && !user.getKernelDbName().isBlank()) {
            databases.add(user.getKernelDbName());
        }
        return new ArrayList<>(databases);
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
        if (statement instanceof CreateDatabaseStatementNode || statement instanceof DropDatabaseStatementNode) {
            if (!isAdmin(user)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admin can create or drop databases");
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

    private String normalizeDbName(String dbName) {
        if (dbName == null || dbName.isBlank()) {
            return "default";
        }
        return dbName.trim().toLowerCase(Locale.ROOT);
    }
}

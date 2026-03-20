package com.indolyn.rill.app.service;

import com.indolyn.rill.core.engine.QueryProcessor;
import com.indolyn.rill.core.session.Session;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Minimal application service layer that adapts Spring-facing use cases to the core engine.
 */
@Service
public class RillQueryService {

    private final QueryProcessorRegistry registry;

    public RillQueryService(QueryProcessorRegistry registry) {
        this.registry = registry;
    }

    public String execute(String dbName, String sql, Session session) {
        QueryProcessor processor = registry.getOrCreate(normalizeDbName(dbName));
        return processor.executeAndGetResult(sql, session);
    }

    public List<String> getLoadedDatabases() {
        return registry.getLoadedDatabases();
    }

    public Session createRootSession() {
        return Session.createAuthenticatedSession(-1, "root");
    }

    private String normalizeDbName(String dbName) {
        if (dbName == null || dbName.isBlank()) {
            return "default";
        }
        return dbName.trim();
    }
}

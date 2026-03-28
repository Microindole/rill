package com.indolyn.rill.app.service;

import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Spring-facing query use cases built on top of the application database boundary.
 */
@Service
public class RillQueryService {

    private final DatabaseService databaseService;

    public RillQueryService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    public DatabaseExecution execute(String dbName, String sql) {
        return databaseService.execute(dbName, sql);
    }

    public List<String> getLoadedDatabases() {
        return databaseService.getLoadedDatabases();
    }
}

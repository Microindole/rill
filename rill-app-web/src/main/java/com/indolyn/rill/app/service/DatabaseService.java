package com.indolyn.rill.app.service;

import java.util.List;

/**
 * Stable application-facing boundary for database access.
 */
public interface DatabaseService {

    DatabaseExecution execute(String dbName, String sql);

    List<String> getLoadedDatabases();
}

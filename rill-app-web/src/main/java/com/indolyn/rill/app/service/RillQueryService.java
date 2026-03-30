package com.indolyn.rill.app.service;

import java.util.List;

public interface RillQueryService {

    DatabaseExecution execute(String dbName, String sql);

    List<String> getLoadedDatabases();
}

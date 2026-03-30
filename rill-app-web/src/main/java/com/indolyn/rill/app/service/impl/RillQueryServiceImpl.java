package com.indolyn.rill.app.service.impl;

import com.indolyn.rill.app.service.DatabaseExecution;
import com.indolyn.rill.app.service.DatabaseService;
import com.indolyn.rill.app.service.RillQueryService;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class RillQueryServiceImpl implements RillQueryService {

    private final DatabaseService databaseService;

    public RillQueryServiceImpl(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    @Override
    public DatabaseExecution execute(String dbName, String sql) {
        return databaseService.execute(dbName, sql);
    }

    @Override
    public List<String> getLoadedDatabases() {
        return databaseService.getLoadedDatabases();
    }
}

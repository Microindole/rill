package com.indolyn.rill.app.service;

import com.indolyn.rill.core.execution.QueryProcessor;

import java.util.List;

public interface QueryProcessorRegistry {

    QueryProcessor getOrCreate(String dbName);

    QueryProcessor getDefault();

    List<String> getLoadedDatabases();

    void shutdown();
}

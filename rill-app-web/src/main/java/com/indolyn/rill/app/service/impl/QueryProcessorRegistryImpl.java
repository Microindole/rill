package com.indolyn.rill.app.service.impl;

import com.indolyn.rill.app.service.QueryProcessorRegistry;
import com.indolyn.rill.core.execution.QueryProcessor;
import jakarta.annotation.PreDestroy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class QueryProcessorRegistryImpl implements QueryProcessorRegistry {

    private final Map<String, QueryProcessor> processors = new ConcurrentHashMap<>();

    @Override
    public QueryProcessor getOrCreate(String dbName) {
        return processors.computeIfAbsent(dbName, QueryProcessor::new);
    }

    @Override
    public QueryProcessor getDefault() {
        return getOrCreate("default");
    }

    @Override
    public List<String> getLoadedDatabases() {
        try {
            // Use default processor's database manager as the source of truth:
            // returns real existing databases, not just currently loaded processors.
            List<String> databases = new ArrayList<>(getDefault().getDbManager().listDatabases());
            if (!databases.contains("default")) {
                databases.add("default");
            }
            Collections.sort(databases);
            return databases;
        } catch (Exception ignored) {
            // Keep service resilient if database manager is temporarily unavailable.
            return new ArrayList<>(processors.keySet());
        }
    }

    @PreDestroy
    @Override
    public void shutdown() {
        for (QueryProcessor processor : processors.values()) {
            try {
                processor.close();
            } catch (IOException e) {
                throw new RuntimeException("Failed to close query processor cleanly", e);
            }
        }
        processors.clear();
    }
}

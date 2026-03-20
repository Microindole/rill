package com.indolyn.rill.app.service;

import com.indolyn.rill.core.engine.QueryProcessor;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spring-facing registry for core query processor instances.
 */
@Component
public class QueryProcessorRegistry {

    private final Map<String, QueryProcessor> processors = new ConcurrentHashMap<>();

    public QueryProcessor getOrCreate(String dbName) {
        return processors.computeIfAbsent(dbName, QueryProcessor::new);
    }

    public QueryProcessor getDefault() {
        return getOrCreate("default");
    }

    public List<String> getLoadedDatabases() {
        return new ArrayList<>(processors.keySet());
    }

    @PreDestroy
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

package com.indolyn.rill.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.indolyn.rill.core.execution.QueryProcessor;
import com.indolyn.rill.core.execution.QueryResult;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class EmbeddedDatabaseServiceTest {

    @Test
    void executeNormalizesInputsAndDelegatesToEmbeddedProcessor() {
        QueryProcessorRegistry registry = Mockito.mock(QueryProcessorRegistry.class);
        QueryProcessor processor = Mockito.mock(QueryProcessor.class);
        QueryResult queryResult = QueryResult.newSuccessResult("Query OK.");
        EmbeddedDatabaseService service = new EmbeddedDatabaseService(registry);

        when(registry.getOrCreate("default")).thenReturn(processor);
        when(processor.executeStructured(eq("select 1"), any())).thenReturn(queryResult);
        when(processor.render(queryResult)).thenReturn("Query OK.");

        DatabaseExecution execution = service.execute("   ", "  select 1  ");

        assertEquals("default", execution.dbName());
        assertEquals("select 1", execution.sql());
        assertSame(queryResult, execution.queryResult());
        assertEquals("Query OK.", execution.rawResult());
        assertFalse(execution.sql().startsWith(" "));
        verify(registry).getOrCreate("default");
        verify(processor).executeStructured(eq("select 1"), any());
        verify(processor).render(queryResult);
    }

    @Test
    void executeUseDatabaseReturnsSwitchedDatabaseNameOnSuccess() {
        QueryProcessorRegistry registry = Mockito.mock(QueryProcessorRegistry.class);
        QueryProcessor processor = Mockito.mock(QueryProcessor.class);
        QueryResult queryResult = QueryResult.newSuccessResult("Database changed to 'demo'.");
        EmbeddedDatabaseService service = new EmbeddedDatabaseService(registry);

        when(registry.getOrCreate("default")).thenReturn(processor);
        when(processor.executeStructured(eq("USE demo;"), any())).thenReturn(queryResult);
        when(processor.render(queryResult)).thenReturn("Database changed to 'demo'.");

        DatabaseExecution execution = service.execute("default", "USE demo;");

        assertEquals("demo", execution.dbName());
        assertTrue(execution.rawResult().contains("demo"));
        verify(registry).getOrCreate("default");
    }
}

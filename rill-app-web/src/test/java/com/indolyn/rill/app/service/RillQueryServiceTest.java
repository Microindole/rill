package com.indolyn.rill.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.indolyn.rill.core.execution.QueryResult;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RillQueryServiceTest {

    @Test
    void executeDelegatesToDatabaseService() {
        DatabaseService databaseService = Mockito.mock(DatabaseService.class);
        RillQueryService service = new RillQueryService(databaseService);
        DatabaseExecution execution =
            new DatabaseExecution("default", "select 1", QueryResult.newSuccessResult("OK"), "OK");

        when(databaseService.execute("default", "select 1")).thenReturn(execution);

        DatabaseExecution result = service.execute("default", "select 1");

        assertSame(execution, result);
        verify(databaseService).execute("default", "select 1");
    }

    @Test
    void getLoadedDatabasesDelegatesToDatabaseService() {
        DatabaseService databaseService = Mockito.mock(DatabaseService.class);
        RillQueryService service = new RillQueryService(databaseService);
        List<String> expected = List.of("default", "analytics");

        when(databaseService.getLoadedDatabases()).thenReturn(expected);

        List<String> databases = service.getLoadedDatabases();

        assertEquals(expected, databases);
        verify(databaseService).getLoadedDatabases();
    }
}

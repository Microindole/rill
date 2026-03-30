package com.indolyn.rill.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.indolyn.rill.app.dto.DemoScenarioRequest;
import com.indolyn.rill.app.dto.DemoScenarioRunResponse;
import com.indolyn.rill.app.dto.QueryExecuteResponse;
import com.indolyn.rill.app.dto.WorkspaceSessionResponse;
import com.indolyn.rill.app.persistence.entity.DemoScenarioEntity;
import com.indolyn.rill.app.persistence.mapper.DemoScenarioMapper;
import com.indolyn.rill.app.service.impl.DemoScenarioServiceImpl;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.server.ResponseStatusException;

class DemoScenarioServiceTest {

    @Test
    void createScenarioShouldPersistEntity() {
        DemoScenarioMapper demoScenarioMapper = Mockito.mock(DemoScenarioMapper.class);
        WorkspaceService workspaceService = Mockito.mock(WorkspaceService.class);
        CurrentUserProvider currentUserProvider = Mockito.mock(CurrentUserProvider.class);
        when(currentUserProvider.requireCurrentUserId()).thenReturn(1L);
        DemoScenarioService service =
            new DemoScenarioServiceImpl(demoScenarioMapper, workspaceService, currentUserProvider);

        var response =
            service.createScenario(
                new DemoScenarioRequest("Bootstrap", "Create demo data", "create table users (id int);"));

        assertEquals("Bootstrap", response.title());
        verify(demoScenarioMapper).insert(any(DemoScenarioEntity.class));
    }

    @Test
    void runScenarioShouldExecuteEveryStatementInOrder() {
        DemoScenarioMapper demoScenarioMapper = Mockito.mock(DemoScenarioMapper.class);
        WorkspaceService workspaceService = Mockito.mock(WorkspaceService.class);
        CurrentUserProvider currentUserProvider = Mockito.mock(CurrentUserProvider.class);
        when(currentUserProvider.requireCurrentUserId()).thenReturn(1L);
        DemoScenarioService service =
            new DemoScenarioServiceImpl(demoScenarioMapper, workspaceService, currentUserProvider);
        DemoScenarioEntity entity = scenario(1L, "Bootstrap", "use demo; create table users (id int);");
        when(demoScenarioMapper.selectOne(any())).thenReturn(entity);
        when(workspaceService.execute("session-1", "use demo;"))
            .thenReturn(response("trace-1", "demo", "use demo;"));
        when(workspaceService.execute("session-1", "create table users (id int);"))
            .thenReturn(response("trace-2", "demo", "create table users (id int);"));
        when(workspaceService.getSession("session-1"))
            .thenReturn(
                new WorkspaceSessionResponse(
                    "session-1",
                    "demo",
                    Instant.parse("2026-03-29T00:00:00Z"),
                    Instant.parse("2026-03-29T00:05:00Z"),
                    List.of("default", "demo"),
                    List.of()));

        DemoScenarioRunResponse run = service.runScenario("session-1", 1L);

        assertEquals(2, run.statementsExecuted());
        assertEquals("demo", run.finalDatabase());
        assertEquals(2, run.executions().size());
        assertEquals("trace-1", run.executions().get(0).traceId());
        assertEquals("trace-2", run.executions().get(1).traceId());
    }

    @Test
    void missingScenarioShouldReturnNotFound() {
        DemoScenarioMapper demoScenarioMapper = Mockito.mock(DemoScenarioMapper.class);
        WorkspaceService workspaceService = Mockito.mock(WorkspaceService.class);
        CurrentUserProvider currentUserProvider = Mockito.mock(CurrentUserProvider.class);
        when(currentUserProvider.requireCurrentUserId()).thenReturn(1L);
        DemoScenarioService service =
            new DemoScenarioServiceImpl(demoScenarioMapper, workspaceService, currentUserProvider);

        ResponseStatusException exception =
            assertThrows(ResponseStatusException.class, () -> service.getScenario(9L));

        assertEquals(404, exception.getStatusCode().value());
    }

    private DemoScenarioEntity scenario(Long id, String title, String sqlScript) {
        DemoScenarioEntity entity = new DemoScenarioEntity();
        entity.setId(id);
        entity.setOwnerId(1L);
        entity.setTitle(title);
        entity.setDescription("demo");
        entity.setSqlScript(sqlScript);
        entity.setCreatedAt(Instant.parse("2026-03-29T00:00:00Z"));
        entity.setUpdatedAt(Instant.parse("2026-03-29T00:00:00Z"));
        return entity;
    }

    private QueryExecuteResponse response(String traceId, String dbName, String sql) {
        return new QueryExecuteResponse(
            traceId,
            dbName,
            sql,
            true,
            5L,
            Instant.parse("2026-03-29T00:00:00Z"),
            "OK",
            List.of(),
            List.of(),
            List.of());
    }
}

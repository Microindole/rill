package com.indolyn.rill.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.indolyn.rill.app.dto.QueryExecuteResponse;
import com.indolyn.rill.app.dto.WorkspaceSessionResponse;
import com.indolyn.rill.app.dto.WorkspaceSessionSummaryResponse;
import com.indolyn.rill.app.persistence.entity.QueryHistoryEntity;
import com.indolyn.rill.app.persistence.entity.WorkspaceSessionEntity;
import com.indolyn.rill.app.persistence.mapper.QueryHistoryMapper;
import com.indolyn.rill.app.persistence.mapper.WorkspaceSessionMapper;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.server.ResponseStatusException;

class WorkspaceServiceTest {

    @Test
    void createSessionShouldPersistDefaultWorkspaceSnapshot() {
        QueryTraceService queryTraceService = Mockito.mock(QueryTraceService.class);
        RillQueryService rillQueryService = Mockito.mock(RillQueryService.class);
        WorkspaceSessionMapper workspaceSessionMapper = Mockito.mock(WorkspaceSessionMapper.class);
        QueryHistoryMapper queryHistoryMapper = Mockito.mock(QueryHistoryMapper.class);
        CurrentUserProvider currentUserProvider = Mockito.mock(CurrentUserProvider.class);
        when(currentUserProvider.requireCurrentUserId()).thenReturn(1L);
        when(rillQueryService.getLoadedDatabases()).thenReturn(List.of("default", "demo"));
        when(queryHistoryMapper.selectList(any())).thenReturn(List.of());
        WorkspaceService workspaceService =
            new WorkspaceService(
                queryTraceService, rillQueryService, workspaceSessionMapper, queryHistoryMapper, currentUserProvider);

        WorkspaceSessionResponse response = workspaceService.createSession();

        assertEquals("default", response.currentDatabase());
        assertEquals(List.of("default", "demo"), response.loadedDatabases());
        assertTrue(response.recentQueries().isEmpty());
        verify(workspaceSessionMapper).insert(any(WorkspaceSessionEntity.class));
    }

    @Test
    void executeShouldPersistHistoryAndAdvanceCurrentDatabase() {
        QueryTraceService queryTraceService = Mockito.mock(QueryTraceService.class);
        RillQueryService rillQueryService = Mockito.mock(RillQueryService.class);
        WorkspaceSessionMapper workspaceSessionMapper = Mockito.mock(WorkspaceSessionMapper.class);
        QueryHistoryMapper queryHistoryMapper = Mockito.mock(QueryHistoryMapper.class);
        CurrentUserProvider currentUserProvider = Mockito.mock(CurrentUserProvider.class);
        when(currentUserProvider.requireCurrentUserId()).thenReturn(1L);
        when(rillQueryService.getLoadedDatabases()).thenReturn(List.of("default", "demo"));
        when(queryHistoryMapper.selectList(any())).thenReturn(
            List.of(history("trace-1", "demo", "use demo;", true, 5L, Instant.parse("2026-03-29T00:00:00Z"))));
        WorkspaceService workspaceService =
            new WorkspaceService(
                queryTraceService, rillQueryService, workspaceSessionMapper, queryHistoryMapper, currentUserProvider);
        WorkspaceSessionResponse session = workspaceService.createSession();
        WorkspaceSessionEntity stored = new WorkspaceSessionEntity();
        stored.setSessionId(session.sessionId());
        stored.setOwnerId(1L);
        stored.setCurrentDatabase("default");
        stored.setCreatedAt(session.createdAt());
        stored.setLastUsedAt(session.lastUsedAt());
        when(workspaceSessionMapper.selectOne(any())).thenReturn(stored);
        QueryExecuteResponse execution =
            new QueryExecuteResponse(
                "trace-1",
                "demo",
                "use demo;",
                true,
                5L,
                Instant.parse("2026-03-29T00:00:00Z"),
                "Database changed to 'demo'.",
                List.of(),
                List.of(),
                List.of());
        when(queryTraceService.execute("default", "use demo;")).thenReturn(execution);

        QueryExecuteResponse response = workspaceService.execute(session.sessionId(), "use demo;");
        WorkspaceSessionResponse updated = workspaceService.getSession(session.sessionId());

        assertEquals("trace-1", response.traceId());
        assertEquals("demo", updated.currentDatabase());
        assertEquals(1, updated.recentQueries().size());
        assertEquals("use demo;", updated.recentQueries().get(0).sql());
        verify(queryHistoryMapper).insert(any(QueryHistoryEntity.class));
        verify(workspaceSessionMapper).updateById(any(WorkspaceSessionEntity.class));
    }

    @Test
    void missingWorkspaceSessionShouldReturnNotFound() {
        QueryTraceService queryTraceService = Mockito.mock(QueryTraceService.class);
        RillQueryService rillQueryService = Mockito.mock(RillQueryService.class);
        WorkspaceSessionMapper workspaceSessionMapper = Mockito.mock(WorkspaceSessionMapper.class);
        QueryHistoryMapper queryHistoryMapper = Mockito.mock(QueryHistoryMapper.class);
        CurrentUserProvider currentUserProvider = Mockito.mock(CurrentUserProvider.class);
        when(currentUserProvider.requireCurrentUserId()).thenReturn(1L);
        WorkspaceService workspaceService =
            new WorkspaceService(
                queryTraceService, rillQueryService, workspaceSessionMapper, queryHistoryMapper, currentUserProvider);

        ResponseStatusException exception =
            assertThrows(ResponseStatusException.class, () -> workspaceService.getSession("missing"));

        assertEquals(404, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("Workspace session"));
    }

    @Test
    void listSessionsShouldReturnSummariesOrderedByRecentUse() {
        QueryTraceService queryTraceService = Mockito.mock(QueryTraceService.class);
        RillQueryService rillQueryService = Mockito.mock(RillQueryService.class);
        WorkspaceSessionMapper workspaceSessionMapper = Mockito.mock(WorkspaceSessionMapper.class);
        QueryHistoryMapper queryHistoryMapper = Mockito.mock(QueryHistoryMapper.class);
        CurrentUserProvider currentUserProvider = Mockito.mock(CurrentUserProvider.class);
        when(currentUserProvider.requireCurrentUserId()).thenReturn(1L);
        WorkspaceService workspaceService =
            new WorkspaceService(
                queryTraceService, rillQueryService, workspaceSessionMapper, queryHistoryMapper, currentUserProvider);
        WorkspaceSessionEntity session = new WorkspaceSessionEntity();
        session.setSessionId("session-1");
        session.setOwnerId(1L);
        session.setCurrentDatabase("demo");
        session.setCreatedAt(Instant.parse("2026-03-29T00:00:00Z"));
        session.setLastUsedAt(Instant.parse("2026-03-29T00:05:00Z"));
        when(workspaceSessionMapper.selectList(any())).thenReturn(List.of(session));
        when(queryHistoryMapper.selectCount(any())).thenReturn(3L);

        List<WorkspaceSessionSummaryResponse> summaries = workspaceService.listSessions();

        assertEquals(1, summaries.size());
        assertEquals("session-1", summaries.get(0).sessionId());
        assertEquals(3, summaries.get(0).recentQueryCount());
    }

    private QueryHistoryEntity history(
        String traceId, String dbName, String sql, boolean success, long elapsedMs, Instant executedAt) {
        QueryHistoryEntity entity = new QueryHistoryEntity();
        entity.setOwnerId(1L);
        entity.setTraceId(traceId);
        entity.setDbName(dbName);
        entity.setSqlText(sql);
        entity.setSuccess(success);
        entity.setElapsedMs(elapsedMs);
        entity.setExecutedAt(executedAt);
        return entity;
    }
}

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
import com.indolyn.rill.app.service.impl.WorkspaceServiceImpl;

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
        DatabaseAccessPolicyService databaseAccessPolicyService = Mockito.mock(DatabaseAccessPolicyService.class);
        when(currentUserProvider.requireCurrentUserId()).thenReturn(1L);
        when(currentUserProvider.requireCurrentUser()).thenReturn(user(1L, "demo", "Demo", "USER", "demo"));
        when(rillQueryService.getLoadedDatabases()).thenReturn(List.of("default", "demo"));
        when(databaseAccessPolicyService.defaultDatabase(any())).thenReturn("demo");
        when(databaseAccessPolicyService.accessibleDatabases(any(), any())).thenReturn(List.of("default", "demo"));
        when(queryHistoryMapper.selectList(any())).thenReturn(List.of());
        WorkspaceService workspaceService =
            new WorkspaceServiceImpl(
                queryTraceService,
                rillQueryService,
                workspaceSessionMapper,
                queryHistoryMapper,
                currentUserProvider,
                databaseAccessPolicyService);

        WorkspaceSessionResponse response = workspaceService.createSession();

        assertEquals("demo", response.currentDatabase());
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
        DatabaseAccessPolicyService databaseAccessPolicyService = Mockito.mock(DatabaseAccessPolicyService.class);
        when(currentUserProvider.requireCurrentUserId()).thenReturn(1L);
        when(currentUserProvider.requireCurrentUser()).thenReturn(user(1L, "demo", "Demo", "USER", "demo"));
        when(rillQueryService.getLoadedDatabases()).thenReturn(List.of("default", "demo"));
        when(databaseAccessPolicyService.defaultDatabase(any())).thenReturn("demo");
        when(databaseAccessPolicyService.accessibleDatabases(any(), any())).thenReturn(List.of("default", "demo"));
        when(queryHistoryMapper.selectList(any())).thenReturn(
            List.of(history("trace-1", "demo", "use demo;", true, 5L, Instant.parse("2026-03-29T00:00:00Z"))));
        WorkspaceService workspaceService =
            new WorkspaceServiceImpl(
                queryTraceService,
                rillQueryService,
                workspaceSessionMapper,
                queryHistoryMapper,
                currentUserProvider,
                databaseAccessPolicyService);
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
        DatabaseAccessPolicyService databaseAccessPolicyService = Mockito.mock(DatabaseAccessPolicyService.class);
        when(currentUserProvider.requireCurrentUserId()).thenReturn(1L);
        when(currentUserProvider.requireCurrentUser()).thenReturn(user(1L, "demo", "Demo", "USER", "demo"));
        WorkspaceService workspaceService =
            new WorkspaceServiceImpl(
                queryTraceService,
                rillQueryService,
                workspaceSessionMapper,
                queryHistoryMapper,
                currentUserProvider,
                databaseAccessPolicyService);

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
        DatabaseAccessPolicyService databaseAccessPolicyService = Mockito.mock(DatabaseAccessPolicyService.class);
        when(currentUserProvider.requireCurrentUserId()).thenReturn(1L);
        when(currentUserProvider.requireCurrentUser()).thenReturn(user(1L, "demo", "Demo", "USER", "demo"));
        when(queryHistoryMapper.selectCount(any())).thenReturn(3L);
        WorkspaceService workspaceService =
            new WorkspaceServiceImpl(
                queryTraceService,
                rillQueryService,
                workspaceSessionMapper,
                queryHistoryMapper,
                currentUserProvider,
                databaseAccessPolicyService);
        WorkspaceSessionEntity session = new WorkspaceSessionEntity();
        session.setSessionId("session-1");
        session.setOwnerId(1L);
        session.setCurrentDatabase("demo");
        session.setCreatedAt(Instant.parse("2026-03-29T00:00:00Z"));
        session.setLastUsedAt(Instant.parse("2026-03-29T00:05:00Z"));
        when(workspaceSessionMapper.selectList(any())).thenReturn(List.of(session));
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

    private com.indolyn.rill.app.persistence.entity.AppUserEntity user(
        Long id, String username, String displayName, String role, String kernelDbName) {
        var user = new com.indolyn.rill.app.persistence.entity.AppUserEntity();
        user.setId(id);
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setRole(role);
        user.setKernelDbName(kernelDbName);
        return user;
    }
}

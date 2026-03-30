package com.indolyn.rill.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.indolyn.rill.app.dto.WorkspaceDashboardResponse;
import com.indolyn.rill.app.persistence.entity.DemoScenarioEntity;
import com.indolyn.rill.app.persistence.entity.QueryHistoryEntity;
import com.indolyn.rill.app.persistence.entity.SqlSnippetEntity;
import com.indolyn.rill.app.persistence.entity.WorkspaceSessionEntity;
import com.indolyn.rill.app.persistence.mapper.DemoScenarioMapper;
import com.indolyn.rill.app.persistence.mapper.QueryHistoryMapper;
import com.indolyn.rill.app.persistence.mapper.SqlSnippetMapper;
import com.indolyn.rill.app.persistence.mapper.WorkspaceSessionMapper;
import com.indolyn.rill.app.service.impl.WorkspaceDashboardServiceImpl;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class WorkspaceDashboardServiceTest {

    @Test
    void dashboardShouldAggregateWorkspaceCounts() {
        WorkspaceSessionMapper workspaceSessionMapper = Mockito.mock(WorkspaceSessionMapper.class);
        QueryHistoryMapper queryHistoryMapper = Mockito.mock(QueryHistoryMapper.class);
        SqlSnippetMapper sqlSnippetMapper = Mockito.mock(SqlSnippetMapper.class);
        DemoScenarioMapper demoScenarioMapper = Mockito.mock(DemoScenarioMapper.class);
        RillQueryService rillQueryService = Mockito.mock(RillQueryService.class);
        CurrentUserProvider currentUserProvider = Mockito.mock(CurrentUserProvider.class);
        DatabaseAccessPolicyService databaseAccessPolicyService = Mockito.mock(DatabaseAccessPolicyService.class);
        when(currentUserProvider.requireCurrentUserId()).thenReturn(1L);
        when(currentUserProvider.requireCurrentUser()).thenReturn(user(1L, "demo", "Demo", "USER", "demo"));
        when(workspaceSessionMapper.selectList(any())).thenReturn(List.of(session("session-1", "demo")));
        when(queryHistoryMapper.selectList(any())).thenReturn(List.of(history("session-1", "trace-1")));
        when(sqlSnippetMapper.selectCount(any())).thenReturn(2L);
        when(demoScenarioMapper.selectCount(any())).thenReturn(1L);
        when(rillQueryService.getLoadedDatabases()).thenReturn(List.of("default", "demo"));
        when(databaseAccessPolicyService.accessibleDatabases(any(), any())).thenReturn(List.of("default", "demo"));
        WorkspaceDashboardService service =
            new WorkspaceDashboardServiceImpl(
                workspaceSessionMapper,
                queryHistoryMapper,
                sqlSnippetMapper,
                demoScenarioMapper,
                rillQueryService,
                currentUserProvider,
                databaseAccessPolicyService);

        WorkspaceDashboardResponse dashboard = service.getDashboard();

        assertEquals(1, dashboard.totalSessions());
        assertEquals(1, dashboard.totalQueryHistory());
        assertEquals(2, dashboard.totalSnippets());
        assertEquals(1, dashboard.totalScenarios());
        assertEquals("session-1", dashboard.sessions().get(0).sessionId());
        assertEquals("trace-1", dashboard.recentQueries().get(0).traceId());
    }

    private WorkspaceSessionEntity session(String sessionId, String dbName) {
        WorkspaceSessionEntity entity = new WorkspaceSessionEntity();
        entity.setSessionId(sessionId);
        entity.setOwnerId(1L);
        entity.setCurrentDatabase(dbName);
        entity.setCreatedAt(Instant.parse("2026-03-29T00:00:00Z"));
        entity.setLastUsedAt(Instant.parse("2026-03-29T00:05:00Z"));
        return entity;
    }

    private QueryHistoryEntity history(String sessionId, String traceId) {
        QueryHistoryEntity entity = new QueryHistoryEntity();
        entity.setOwnerId(1L);
        entity.setSessionId(sessionId);
        entity.setTraceId(traceId);
        entity.setDbName("demo");
        entity.setSqlText("show tables;");
        entity.setSuccess(true);
        entity.setElapsedMs(4L);
        entity.setExecutedAt(Instant.parse("2026-03-29T00:05:00Z"));
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

package com.indolyn.rill.app.service;

import com.indolyn.rill.app.dto.QueryHistoryItemResponse;
import com.indolyn.rill.app.dto.WorkspaceDashboardResponse;
import com.indolyn.rill.app.dto.WorkspaceSessionSummaryResponse;
import com.indolyn.rill.app.persistence.entity.QueryHistoryEntity;
import com.indolyn.rill.app.persistence.entity.WorkspaceSessionEntity;
import com.indolyn.rill.app.persistence.mapper.DemoScenarioMapper;
import com.indolyn.rill.app.persistence.mapper.QueryHistoryMapper;
import com.indolyn.rill.app.persistence.mapper.SqlSnippetMapper;
import com.indolyn.rill.app.persistence.mapper.WorkspaceSessionMapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
public class WorkspaceDashboardService {

    private static final int MAX_RECENT_QUERIES = 10;

    private final WorkspaceSessionMapper workspaceSessionMapper;
    private final QueryHistoryMapper queryHistoryMapper;
    private final SqlSnippetMapper sqlSnippetMapper;
    private final DemoScenarioMapper demoScenarioMapper;
    private final RillQueryService rillQueryService;

    public WorkspaceDashboardService(
        WorkspaceSessionMapper workspaceSessionMapper,
        QueryHistoryMapper queryHistoryMapper,
        SqlSnippetMapper sqlSnippetMapper,
        DemoScenarioMapper demoScenarioMapper,
        RillQueryService rillQueryService) {
        this.workspaceSessionMapper = workspaceSessionMapper;
        this.queryHistoryMapper = queryHistoryMapper;
        this.sqlSnippetMapper = sqlSnippetMapper;
        this.demoScenarioMapper = demoScenarioMapper;
        this.rillQueryService = rillQueryService;
    }

    public WorkspaceDashboardResponse getDashboard() {
        List<WorkspaceSessionEntity> sessions =
            workspaceSessionMapper.selectList(new QueryWrapper<WorkspaceSessionEntity>().orderByDesc("last_used_at"));
        List<QueryHistoryEntity> allHistory =
            queryHistoryMapper.selectList(new QueryWrapper<QueryHistoryEntity>().orderByDesc("executed_at"));
        Map<String, Long> historyCountBySession =
            allHistory.stream().collect(Collectors.groupingBy(QueryHistoryEntity::getSessionId, Collectors.counting()));

        List<WorkspaceSessionSummaryResponse> sessionSummaries =
            sessions.stream()
                .map(
                    session ->
                        new WorkspaceSessionSummaryResponse(
                            session.getSessionId(),
                            session.getCurrentDatabase(),
                            session.getCreatedAt(),
                            session.getLastUsedAt(),
                            historyCountBySession.getOrDefault(session.getSessionId(), 0L).intValue()))
                .toList();

        List<QueryHistoryItemResponse> recentQueries =
            allHistory.stream().limit(MAX_RECENT_QUERIES).map(this::toHistoryResponse).toList();

        return new WorkspaceDashboardResponse(
            sessions.size(),
            allHistory.size(),
            Math.toIntExact(sqlSnippetMapper.selectCount(null)),
            Math.toIntExact(demoScenarioMapper.selectCount(null)),
            rillQueryService.getLoadedDatabases(),
            sessionSummaries,
            recentQueries);
    }

    private QueryHistoryItemResponse toHistoryResponse(QueryHistoryEntity entity) {
        return new QueryHistoryItemResponse(
            entity.getTraceId(),
            entity.getDbName(),
            entity.getSqlText(),
            entity.isSuccess(),
            entity.getElapsedMs(),
            entity.getExecutedAt());
    }
}

package com.indolyn.rill.app.service;

import com.indolyn.rill.app.dto.QueryExecuteResponse;
import com.indolyn.rill.app.dto.QueryHistoryItemResponse;
import com.indolyn.rill.app.dto.WorkspaceSessionResponse;
import com.indolyn.rill.app.dto.WorkspaceSessionSummaryResponse;
import com.indolyn.rill.app.persistence.entity.QueryHistoryEntity;
import com.indolyn.rill.app.persistence.entity.WorkspaceSessionEntity;
import com.indolyn.rill.app.persistence.mapper.QueryHistoryMapper;
import com.indolyn.rill.app.persistence.mapper.WorkspaceSessionMapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WorkspaceService {

    private static final int MAX_SESSION_HISTORY = 20;

    private final QueryTraceService queryTraceService;
    private final RillQueryService rillQueryService;
    private final WorkspaceSessionMapper workspaceSessionMapper;
    private final QueryHistoryMapper queryHistoryMapper;

    public WorkspaceService(
        QueryTraceService queryTraceService,
        RillQueryService rillQueryService,
        WorkspaceSessionMapper workspaceSessionMapper,
        QueryHistoryMapper queryHistoryMapper) {
        this.queryTraceService = queryTraceService;
        this.rillQueryService = rillQueryService;
        this.workspaceSessionMapper = workspaceSessionMapper;
        this.queryHistoryMapper = queryHistoryMapper;
    }

    public WorkspaceSessionResponse createSession() {
        String sessionId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        WorkspaceSessionEntity session = new WorkspaceSessionEntity();
        session.setSessionId(sessionId);
        session.setCurrentDatabase("default");
        session.setCreatedAt(now);
        session.setLastUsedAt(now);
        workspaceSessionMapper.insert(session);
        return toResponse(session);
    }

    public WorkspaceSessionResponse getSession(String sessionId) {
        return toResponse(requireSession(sessionId));
    }

    public List<WorkspaceSessionSummaryResponse> listSessions() {
        return workspaceSessionMapper
            .selectList(new QueryWrapper<WorkspaceSessionEntity>().orderByDesc("last_used_at"))
            .stream()
            .map(
                session ->
                    new WorkspaceSessionSummaryResponse(
                        session.getSessionId(),
                        session.getCurrentDatabase(),
                        session.getCreatedAt(),
                        session.getLastUsedAt(),
                        countQueries(session.getSessionId())))
            .toList();
    }

    public List<QueryHistoryItemResponse> getSessionHistory(String sessionId) {
        requireSession(sessionId);
        return loadRecentQueries(sessionId);
    }

    public void deleteSession(String sessionId) {
        requireSession(sessionId);
        queryHistoryMapper.delete(new QueryWrapper<QueryHistoryEntity>().eq("session_id", sessionId));
        workspaceSessionMapper.deleteById(sessionId);
    }

    public QueryExecuteResponse execute(String sessionId, String sql) {
        WorkspaceSessionEntity session = requireSession(sessionId);
        QueryExecuteResponse response = queryTraceService.execute(session.getCurrentDatabase(), sql);
        record(session, response);
        return response;
    }

    private WorkspaceSessionEntity requireSession(String sessionId) {
        WorkspaceSessionEntity session = workspaceSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace session not found");
        }
        return session;
    }

    private void record(WorkspaceSessionEntity session, QueryExecuteResponse response) {
        String sessionId = session.getSessionId();
        queryHistoryMapper.insert(toHistoryEntity(sessionId, response));
        session.setCurrentDatabase(response.dbName());
        session.setLastUsedAt(response.executedAt());
        workspaceSessionMapper.updateById(session);
    }

    private WorkspaceSessionResponse toResponse(WorkspaceSessionEntity session) {
        return new WorkspaceSessionResponse(
            session.getSessionId(),
            session.getCurrentDatabase(),
            session.getCreatedAt(),
            session.getLastUsedAt(),
            rillQueryService.getLoadedDatabases(),
            loadRecentQueries(session.getSessionId()));
    }

    private List<QueryHistoryItemResponse> loadRecentQueries(String sessionId) {
        return queryHistoryMapper
            .selectList(
                new QueryWrapper<QueryHistoryEntity>()
                    .eq("session_id", sessionId)
                    .orderByDesc("executed_at")
                    .last("limit " + MAX_SESSION_HISTORY))
            .stream()
            .map(this::toHistoryResponse)
            .toList();
    }

    private int countQueries(String sessionId) {
        return Math.toIntExact(
            queryHistoryMapper.selectCount(new QueryWrapper<QueryHistoryEntity>().eq("session_id", sessionId)));
    }

    private QueryHistoryEntity toHistoryEntity(String sessionId, QueryExecuteResponse response) {
        QueryHistoryEntity entity = new QueryHistoryEntity();
        entity.setSessionId(sessionId);
        entity.setTraceId(response.traceId());
        entity.setDbName(response.dbName());
        entity.setSqlText(response.sql());
        entity.setSuccess(response.success());
        entity.setElapsedMs(response.elapsedMs());
        entity.setExecutedAt(response.executedAt());
        return entity;
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

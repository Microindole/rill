package com.indolyn.rill.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.indolyn.rill.app.dto.QueryExecuteResponse;
import com.indolyn.rill.app.dto.QueryHistoryItemResponse;
import com.indolyn.rill.app.dto.WorkspaceSessionResponse;
import com.indolyn.rill.app.dto.WorkspaceSessionSummaryResponse;
import com.indolyn.rill.app.persistence.entity.QueryHistoryEntity;
import com.indolyn.rill.app.persistence.entity.WorkspaceSessionEntity;
import com.indolyn.rill.app.persistence.mapper.QueryHistoryMapper;
import com.indolyn.rill.app.persistence.mapper.WorkspaceSessionMapper;
import com.indolyn.rill.app.service.CurrentUserProvider;
import com.indolyn.rill.app.service.DatabaseAccessPolicyService;
import com.indolyn.rill.app.service.QueryTraceService;
import com.indolyn.rill.app.service.RillQueryService;
import com.indolyn.rill.app.service.WorkspaceService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WorkspaceServiceImpl implements WorkspaceService {

    private static final int MAX_SESSION_HISTORY = 20;

    private final QueryTraceService queryTraceService;
    private final RillQueryService rillQueryService;
    private final WorkspaceSessionMapper workspaceSessionMapper;
    private final QueryHistoryMapper queryHistoryMapper;
    private final CurrentUserProvider currentUserProvider;
    private final DatabaseAccessPolicyService databaseAccessPolicyService;

    public WorkspaceServiceImpl(
        QueryTraceService queryTraceService,
        RillQueryService rillQueryService,
        WorkspaceSessionMapper workspaceSessionMapper,
        QueryHistoryMapper queryHistoryMapper,
        CurrentUserProvider currentUserProvider,
        DatabaseAccessPolicyService databaseAccessPolicyService) {
        this.queryTraceService = queryTraceService;
        this.rillQueryService = rillQueryService;
        this.workspaceSessionMapper = workspaceSessionMapper;
        this.queryHistoryMapper = queryHistoryMapper;
        this.currentUserProvider = currentUserProvider;
        this.databaseAccessPolicyService = databaseAccessPolicyService;
    }

    @Override
    @Transactional
    public WorkspaceSessionResponse createSession() {
        var currentUser = currentUserProvider.requireCurrentUser();
        long ownerId = currentUser.getId();
        String sessionId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        WorkspaceSessionEntity session = new WorkspaceSessionEntity();
        session.setSessionId(sessionId);
        session.setOwnerId(ownerId);
        session.setCurrentDatabase(databaseAccessPolicyService.defaultDatabase(currentUser));
        session.setCreatedAt(now);
        session.setLastUsedAt(now);
        workspaceSessionMapper.insert(session);
        return toResponse(session);
    }

    @Override
    public WorkspaceSessionResponse getSession(String sessionId) {
        return toResponse(requireSession(sessionId));
    }

    @Override
    public List<WorkspaceSessionSummaryResponse> listSessions() {
        long ownerId = currentUserProvider.requireCurrentUserId();
        return workspaceSessionMapper
            .selectList(new QueryWrapper<WorkspaceSessionEntity>().eq("owner_id", ownerId).orderByDesc("last_used_at"))
            .stream()
            .map(
                session -> new WorkspaceSessionSummaryResponse(
                    session.getSessionId(),
                    session.getCurrentDatabase(),
                    session.getCreatedAt(),
                    session.getLastUsedAt(),
                    countQueries(session.getSessionId())))
            .toList();
    }

    @Override
    public List<QueryHistoryItemResponse> getSessionHistory(String sessionId) {
        requireSession(sessionId);
        return loadRecentQueries(sessionId);
    }

    @Override
    @Transactional
    public void deleteSession(String sessionId) {
        WorkspaceSessionEntity session = requireSession(sessionId);
        queryHistoryMapper.delete(
            new QueryWrapper<QueryHistoryEntity>().eq("owner_id", session.getOwnerId()).eq("session_id", sessionId));
        workspaceSessionMapper.delete(
            new QueryWrapper<WorkspaceSessionEntity>().eq("owner_id", session.getOwnerId()).eq("session_id", sessionId));
    }

    @Override
    @Transactional
    public QueryExecuteResponse execute(String sessionId, String sql) {
        WorkspaceSessionEntity session = requireSession(sessionId);
        var currentUser = currentUserProvider.requireCurrentUser();
        databaseAccessPolicyService.assertCanExecute(
            currentUser, session.getCurrentDatabase(), sql, rillQueryService.getLoadedDatabases());
        QueryExecuteResponse response = queryTraceService.execute(session.getCurrentDatabase(), sql);
        record(session, response);
        return response;
    }

    private WorkspaceSessionEntity requireSession(String sessionId) {
        WorkspaceSessionEntity session =
            workspaceSessionMapper.selectOne(
                new QueryWrapper<WorkspaceSessionEntity>()
                    .eq("owner_id", currentUserProvider.requireCurrentUserId())
                    .eq("session_id", sessionId)
                    .last("limit 1"));
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
            databaseAccessPolicyService.accessibleDatabases(
                currentUserProvider.requireCurrentUser(), rillQueryService.getLoadedDatabases()),
            loadRecentQueries(session.getSessionId()));
    }

    private List<QueryHistoryItemResponse> loadRecentQueries(String sessionId) {
        long ownerId = currentUserProvider.requireCurrentUserId();
        return queryHistoryMapper
            .selectList(
                new QueryWrapper<QueryHistoryEntity>()
                    .eq("owner_id", ownerId)
                    .eq("session_id", sessionId)
                    .orderByDesc("executed_at")
                    .last("limit " + MAX_SESSION_HISTORY))
            .stream()
            .map(this::toHistoryResponse)
            .toList();
    }

    private int countQueries(String sessionId) {
        long ownerId = currentUserProvider.requireCurrentUserId();
        return Math.toIntExact(
            queryHistoryMapper.selectCount(
                new QueryWrapper<QueryHistoryEntity>().eq("owner_id", ownerId).eq("session_id", sessionId)));
    }

    private QueryHistoryEntity toHistoryEntity(String sessionId, QueryExecuteResponse response) {
        QueryHistoryEntity entity = new QueryHistoryEntity();
        entity.setOwnerId(currentUserProvider.requireCurrentUserId());
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

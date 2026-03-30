package com.indolyn.rill.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.indolyn.rill.app.dto.DemoScenarioRequest;
import com.indolyn.rill.app.dto.DemoScenarioResponse;
import com.indolyn.rill.app.dto.DemoScenarioRunResponse;
import com.indolyn.rill.app.dto.QueryExecuteResponse;
import com.indolyn.rill.app.dto.WorkspaceSessionResponse;
import com.indolyn.rill.app.persistence.entity.DemoScenarioEntity;
import com.indolyn.rill.app.persistence.mapper.DemoScenarioMapper;
import com.indolyn.rill.app.service.CurrentUserProvider;
import com.indolyn.rill.app.service.DemoScenarioService;
import com.indolyn.rill.app.service.WorkspaceService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DemoScenarioServiceImpl implements DemoScenarioService {

    private final DemoScenarioMapper demoScenarioMapper;
    private final WorkspaceService workspaceService;
    private final CurrentUserProvider currentUserProvider;

    public DemoScenarioServiceImpl(
        DemoScenarioMapper demoScenarioMapper,
        WorkspaceService workspaceService,
        CurrentUserProvider currentUserProvider) {
        this.demoScenarioMapper = demoScenarioMapper;
        this.workspaceService = workspaceService;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public List<DemoScenarioResponse> listScenarios() {
        long ownerId = currentUserProvider.requireCurrentUserId();
        return demoScenarioMapper
            .selectList(
                new LambdaQueryWrapper<DemoScenarioEntity>()
                    .eq(DemoScenarioEntity::getOwnerId, ownerId)
                    .orderByDesc(DemoScenarioEntity::getUpdatedAt))
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    public DemoScenarioResponse getScenario(long id) {
        return toResponse(requireScenario(id));
    }

    @Override
    @Transactional
    public DemoScenarioResponse createScenario(DemoScenarioRequest request) {
        validateRequest(request);
        Instant now = Instant.now();
        DemoScenarioEntity entity = new DemoScenarioEntity();
        entity.setOwnerId(currentUserProvider.requireCurrentUserId());
        entity.setTitle(request.title().trim());
        entity.setDescription(normalizeText(request.description()));
        entity.setSqlScript(request.sqlScript().trim());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        demoScenarioMapper.insert(entity);
        return toResponse(entity);
    }

    @Override
    @Transactional
    public DemoScenarioResponse updateScenario(long id, DemoScenarioRequest request) {
        validateRequest(request);
        DemoScenarioEntity entity = requireScenario(id);
        entity.setTitle(request.title().trim());
        entity.setDescription(normalizeText(request.description()));
        entity.setSqlScript(request.sqlScript().trim());
        entity.setUpdatedAt(Instant.now());
        demoScenarioMapper.updateById(entity);
        return toResponse(entity);
    }

    @Override
    @Transactional
    public void deleteScenario(long id) {
        requireScenario(id);
        demoScenarioMapper.deleteById(id);
    }

    @Override
    public DemoScenarioRunResponse runScenario(String sessionId, long scenarioId) {
        DemoScenarioEntity scenario = requireScenario(scenarioId);
        List<QueryExecuteResponse> executions = new ArrayList<>();
        for (String statement : splitStatements(scenario.getSqlScript())) {
            executions.add(workspaceService.execute(sessionId, statement));
        }
        WorkspaceSessionResponse session = workspaceService.getSession(sessionId);
        return new DemoScenarioRunResponse(
            scenarioId,
            sessionId,
            executions.size(),
            session.currentDatabase(),
            List.copyOf(executions));
    }

    private List<String> splitStatements(String sqlScript) {
        return sqlScript
            .lines()
            .flatMap(line -> List.of(line.split(";")).stream())
            .map(String::trim)
            .filter(statement -> !statement.isBlank())
            .map(statement -> statement.endsWith(";") ? statement : statement + ";")
            .toList();
    }

    private void validateRequest(DemoScenarioRequest request) {
        if (request == null || request.title() == null || request.title().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Scenario title cannot be empty");
        }
        if (request.sqlScript() == null || request.sqlScript().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Scenario SQL script cannot be empty");
        }
    }

    private DemoScenarioEntity requireScenario(long id) {
        DemoScenarioEntity entity =
            demoScenarioMapper.selectOne(
                new LambdaQueryWrapper<DemoScenarioEntity>()
                    .eq(DemoScenarioEntity::getId, id)
                    .eq(DemoScenarioEntity::getOwnerId, currentUserProvider.requireCurrentUserId())
                    .last("limit 1"));
        if (entity == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Demo scenario not found");
        }
        return entity;
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private DemoScenarioResponse toResponse(DemoScenarioEntity entity) {
        return new DemoScenarioResponse(
            entity.getId(),
            entity.getTitle(),
            entity.getDescription(),
            entity.getSqlScript(),
            entity.getCreatedAt(),
            entity.getUpdatedAt());
    }
}

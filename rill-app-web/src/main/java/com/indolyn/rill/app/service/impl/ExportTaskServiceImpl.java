package com.indolyn.rill.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.indolyn.rill.app.dto.ExportTaskRequest;
import com.indolyn.rill.app.dto.ExportTaskResponse;
import com.indolyn.rill.app.persistence.entity.ExportTaskEntity;
import com.indolyn.rill.app.persistence.mapper.ExportTaskMapper;
import com.indolyn.rill.app.service.CurrentUserProvider;
import com.indolyn.rill.app.service.ExportTaskJobPublisher;
import com.indolyn.rill.app.service.ExportTaskService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ExportTaskServiceImpl implements ExportTaskService {

    private final ExportTaskMapper exportTaskMapper;
    private final Path exportDir;
    private final CurrentUserProvider currentUserProvider;
    private final ExportTaskJobPublisher exportTaskJobPublisher;

    public ExportTaskServiceImpl(
        ExportTaskMapper exportTaskMapper,
        CurrentUserProvider currentUserProvider,
        ExportTaskJobPublisher exportTaskJobPublisher,
        @Value("${app.workspace.export-dir:target/exports}") String exportDir) {
        this.exportTaskMapper = exportTaskMapper;
        this.currentUserProvider = currentUserProvider;
        this.exportTaskJobPublisher = exportTaskJobPublisher;
        this.exportDir = Path.of(exportDir);
    }

    @Override
    public List<ExportTaskResponse> listTasks() {
        long ownerId = currentUserProvider.requireCurrentUserId();
        return exportTaskMapper
            .selectList(
                new LambdaQueryWrapper<ExportTaskEntity>()
                    .eq(ExportTaskEntity::getOwnerId, ownerId)
                    .orderByDesc(ExportTaskEntity::getUpdatedAt))
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    public ExportTaskResponse getTask(long id) {
        return toResponse(requireTask(id));
    }

    @Override
    @Transactional
    public ExportTaskResponse createTask(ExportTaskRequest request) {
        validateRequest(request);
        Instant now = Instant.now();
        ExportTaskEntity entity = new ExportTaskEntity();
        entity.setOwnerId(currentUserProvider.requireCurrentUserId());
        entity.setTitle(request.title().trim());
        entity.setDescription(normalizeText(request.description()));
        entity.setDbName(normalizeDbName(request.dbName()));
        entity.setSqlText(request.sql().trim());
        entity.setExportFormat(normalizeFormat(request.exportFormat()));
        entity.setStatus("PENDING");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        exportTaskMapper.insert(entity);
        return toResponse(entity);
    }

    @Override
    @Transactional
    public ExportTaskResponse updateTask(long id, ExportTaskRequest request) {
        validateRequest(request);
        ExportTaskEntity entity = requireTask(id);
        entity.setTitle(request.title().trim());
        entity.setDescription(normalizeText(request.description()));
        entity.setDbName(normalizeDbName(request.dbName()));
        entity.setSqlText(request.sql().trim());
        entity.setExportFormat(normalizeFormat(request.exportFormat()));
        entity.setUpdatedAt(Instant.now());
        exportTaskMapper.updateById(entity);
        return toResponse(entity);
    }

    @Override
    @Transactional
    public void deleteTask(long id) {
        requireTask(id);
        exportTaskMapper.deleteById(id);
    }

    @Override
    @Transactional
    public ExportTaskResponse runTask(long id) {
        ExportTaskEntity entity = requireTask(id);
        entity.setStatus("QUEUED");
        entity.setOutputPath(null);
        entity.setLastError(null);
        entity.setCompletedAt(null);
        entity.setUpdatedAt(Instant.now());
        exportTaskMapper.updateById(entity);
        exportTaskJobPublisher.publish(entity.getId());
        return toResponse(requireTask(id));
    }

    @Override
    public Path resolveDownloadPath(long id) {
        ExportTaskEntity entity = requireTask(id);
        if (!"COMPLETED".equalsIgnoreCase(entity.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Export task is not completed");
        }
        if (entity.getOutputPath() == null || entity.getOutputPath().isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Export file not found");
        }

        Path basePath = exportDir.toAbsolutePath().normalize();
        Path outputPath = Path.of(entity.getOutputPath()).toAbsolutePath().normalize();
        if (!outputPath.startsWith(basePath)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid export file path");
        }
        if (!Files.exists(outputPath) || !Files.isRegularFile(outputPath)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Export file not found");
        }
        return outputPath;
    }

    private void validateRequest(ExportTaskRequest request) {
        if (request == null || request.title() == null || request.title().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Export task title cannot be empty");
        }
        if (request.sql() == null || request.sql().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Export task SQL cannot be empty");
        }
    }

    private ExportTaskEntity requireTask(long id) {
        ExportTaskEntity entity =
            exportTaskMapper.selectOne(
                new LambdaQueryWrapper<ExportTaskEntity>()
                    .eq(ExportTaskEntity::getId, id)
                    .eq(ExportTaskEntity::getOwnerId, currentUserProvider.requireCurrentUserId())
                    .last("limit 1"));
        if (entity == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Export task not found");
        }
        return entity;
    }

    private String normalizeDbName(String dbName) {
        if (dbName == null || dbName.isBlank()) {
            return "default";
        }
        return dbName.trim();
    }

    private String normalizeFormat(String exportFormat) {
        if (exportFormat == null || exportFormat.isBlank()) {
            return "csv";
        }
        String normalized = exportFormat.trim().toLowerCase();
        if (!"csv".equals(normalized) && !"json".equals(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Export format must be csv or json");
        }
        return normalized;
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private ExportTaskResponse toResponse(ExportTaskEntity entity) {
        return new ExportTaskResponse(
            entity.getId(),
            entity.getTitle(),
            entity.getDescription(),
            entity.getDbName(),
            entity.getSqlText(),
            entity.getExportFormat(),
            entity.getStatus(),
            entity.getOutputPath(),
            entity.getLastError(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.getCompletedAt());
    }
}

package com.indolyn.rill.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.indolyn.rill.app.dto.ExportTaskRequest;
import com.indolyn.rill.app.dto.ExportTaskResponse;
import com.indolyn.rill.app.dto.QueryExecuteResponse;
import com.indolyn.rill.app.persistence.entity.ExportTaskEntity;
import com.indolyn.rill.app.persistence.mapper.ExportTaskMapper;
import com.indolyn.rill.app.service.CurrentUserProvider;
import com.indolyn.rill.app.service.ExportTaskService;
import com.indolyn.rill.app.service.QueryTraceService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
    private final QueryTraceService queryTraceService;
    private final Path exportDir;
    private final CurrentUserProvider currentUserProvider;

    public ExportTaskServiceImpl(
        ExportTaskMapper exportTaskMapper,
        QueryTraceService queryTraceService,
        CurrentUserProvider currentUserProvider,
        @Value("${app.workspace.export-dir:target/exports}") String exportDir) {
        this.exportTaskMapper = exportTaskMapper;
        this.queryTraceService = queryTraceService;
        this.currentUserProvider = currentUserProvider;
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
    public ExportTaskResponse runTask(long id) {
        ExportTaskEntity entity = requireTask(id);
        entity.setStatus("RUNNING");
        entity.setLastError(null);
        entity.setUpdatedAt(Instant.now());
        exportTaskMapper.updateById(entity);

        QueryExecuteResponse execution = queryTraceService.execute(entity.getDbName(), entity.getSqlText());
        if (!execution.success()) {
            entity.setStatus("FAILED");
            entity.setLastError(execution.rawResult());
            entity.setUpdatedAt(Instant.now());
            exportTaskMapper.updateById(entity);
            return toResponse(entity);
        }

        try {
            Files.createDirectories(exportDir);
            String extension = "csv".equalsIgnoreCase(entity.getExportFormat()) ? "csv" : "json";
            Path outputPath = exportDir.resolve("export-task-" + entity.getId() + "." + extension);
            String content = "csv".equalsIgnoreCase(entity.getExportFormat()) ? toCsv(execution) : toJson(execution);
            Files.writeString(outputPath, content, StandardCharsets.UTF_8);
            Instant completedAt = Instant.now();
            entity.setStatus("COMPLETED");
            entity.setOutputPath(outputPath.toAbsolutePath().normalize().toString());
            entity.setCompletedAt(completedAt);
            entity.setUpdatedAt(completedAt);
            exportTaskMapper.updateById(entity);
            return toResponse(entity);
        } catch (IOException e) {
            entity.setStatus("FAILED");
            entity.setLastError(e.getMessage());
            entity.setUpdatedAt(Instant.now());
            exportTaskMapper.updateById(entity);
            return toResponse(entity);
        }
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

    private String toCsv(QueryExecuteResponse execution) {
        StringBuilder builder = new StringBuilder();
        builder.append(String.join(",", execution.columns())).append(System.lineSeparator());
        for (List<String> row : execution.rows()) {
            builder.append(String.join(",", row)).append(System.lineSeparator());
        }
        return builder.toString();
    }

    private String toJson(QueryExecuteResponse execution) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"columns\":[");
        for (int i = 0; i < execution.columns().size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append('"').append(escape(execution.columns().get(i))).append('"');
        }
        builder.append("],\"rows\":[");
        for (int i = 0; i < execution.rows().size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append('[');
            List<String> row = execution.rows().get(i);
            for (int j = 0; j < row.size(); j++) {
                if (j > 0) {
                    builder.append(',');
                }
                builder.append('"').append(escape(row.get(j))).append('"');
            }
            builder.append(']');
        }
        builder.append("]}");
        return builder.toString();
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
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

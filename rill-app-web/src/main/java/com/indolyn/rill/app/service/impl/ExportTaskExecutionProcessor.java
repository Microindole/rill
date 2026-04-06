package com.indolyn.rill.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.indolyn.rill.app.dto.ExportTaskResponse;
import com.indolyn.rill.app.dto.QueryExecuteResponse;
import com.indolyn.rill.app.persistence.entity.ExportTaskEntity;
import com.indolyn.rill.app.persistence.mapper.ExportTaskMapper;
import com.indolyn.rill.app.service.QueryTraceService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ExportTaskExecutionProcessor {

    private final ExportTaskMapper exportTaskMapper;
    private final QueryTraceService queryTraceService;
    private final Path exportDir;

    public ExportTaskExecutionProcessor(
        ExportTaskMapper exportTaskMapper,
        QueryTraceService queryTraceService,
        @Value("${app.workspace.export-dir:target/exports}") String exportDir) {
        this.exportTaskMapper = exportTaskMapper;
        this.queryTraceService = queryTraceService;
        this.exportDir = Path.of(exportDir);
    }

    @Transactional
    public ExportTaskResponse process(long id) {
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

    private ExportTaskEntity requireTask(long id) {
        ExportTaskEntity entity =
            exportTaskMapper.selectOne(
                new LambdaQueryWrapper<ExportTaskEntity>()
                    .eq(ExportTaskEntity::getId, id)
                    .last("limit 1"));
        if (entity == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Export task not found");
        }
        return entity;
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

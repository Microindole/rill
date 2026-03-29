package com.indolyn.rill.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.indolyn.rill.app.dto.ExportTaskRequest;
import com.indolyn.rill.app.dto.ExportTaskResponse;
import com.indolyn.rill.app.dto.QueryExecuteResponse;
import com.indolyn.rill.app.persistence.entity.ExportTaskEntity;
import com.indolyn.rill.app.persistence.mapper.ExportTaskMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.web.server.ResponseStatusException;

class ExportTaskServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void createTaskShouldPersistPendingTask() {
        ExportTaskMapper exportTaskMapper = Mockito.mock(ExportTaskMapper.class);
        QueryTraceService queryTraceService = Mockito.mock(QueryTraceService.class);
        ExportTaskService service = new ExportTaskService(exportTaskMapper, queryTraceService, tempDir.toString());

        ExportTaskResponse response =
            service.createTask(new ExportTaskRequest("Export users", "csv export", "demo", "select * from users;", "csv"));

        assertEquals("PENDING", response.status());
        assertEquals("csv", response.exportFormat());
        verify(exportTaskMapper).insert(any(ExportTaskEntity.class));
    }

    @Test
    void runTaskShouldWriteCsvFileAndMarkCompleted() throws Exception {
        ExportTaskMapper exportTaskMapper = Mockito.mock(ExportTaskMapper.class);
        QueryTraceService queryTraceService = Mockito.mock(QueryTraceService.class);
        ExportTaskService service = new ExportTaskService(exportTaskMapper, queryTraceService, tempDir.toString());
        ExportTaskEntity entity = task(1L, "csv");
        when(exportTaskMapper.selectById(1L)).thenReturn(entity);
        when(queryTraceService.execute("demo", "select * from users;"))
            .thenReturn(
                new QueryExecuteResponse(
                    "trace-1",
                    "demo",
                    "select * from users;",
                    true,
                    5L,
                    Instant.parse("2026-03-29T00:00:00Z"),
                    "OK",
                    List.of("id", "name"),
                    List.of(List.of("1", "alice")),
                    List.of()));

        ExportTaskResponse response = service.runTask(1L);

        assertEquals("COMPLETED", response.status());
        assertNotNull(response.outputPath());
        assertEquals(true, Files.exists(Path.of(response.outputPath())));
    }

    @Test
    void runTaskShouldMarkFailedWhenQueryFails() {
        ExportTaskMapper exportTaskMapper = Mockito.mock(ExportTaskMapper.class);
        QueryTraceService queryTraceService = Mockito.mock(QueryTraceService.class);
        ExportTaskService service = new ExportTaskService(exportTaskMapper, queryTraceService, tempDir.toString());
        ExportTaskEntity entity = task(1L, "json");
        when(exportTaskMapper.selectById(1L)).thenReturn(entity);
        when(queryTraceService.execute("demo", "select * from users;"))
            .thenReturn(
                new QueryExecuteResponse(
                    "trace-1",
                    "demo",
                    "select * from users;",
                    false,
                    5L,
                    Instant.parse("2026-03-29T00:00:00Z"),
                    "ERROR: relation users does not exist",
                    List.of(),
                    List.of(),
                    List.of()));

        ExportTaskResponse response = service.runTask(1L);

        assertEquals("FAILED", response.status());
        assertEquals("ERROR: relation users does not exist", response.lastError());
    }

    @Test
    void invalidFormatShouldRejectRequest() {
        ExportTaskMapper exportTaskMapper = Mockito.mock(ExportTaskMapper.class);
        QueryTraceService queryTraceService = Mockito.mock(QueryTraceService.class);
        ExportTaskService service = new ExportTaskService(exportTaskMapper, queryTraceService, tempDir.toString());

        ResponseStatusException exception =
            assertThrows(
                ResponseStatusException.class,
                () -> service.createTask(new ExportTaskRequest("Export users", "", "demo", "select 1;", "xml")));

        assertEquals(400, exception.getStatusCode().value());
    }

    private ExportTaskEntity task(Long id, String format) {
        ExportTaskEntity entity = new ExportTaskEntity();
        entity.setId(id);
        entity.setTitle("Export users");
        entity.setDescription("export");
        entity.setDbName("demo");
        entity.setSqlText("select * from users;");
        entity.setExportFormat(format);
        entity.setStatus("PENDING");
        entity.setCreatedAt(Instant.parse("2026-03-29T00:00:00Z"));
        entity.setUpdatedAt(Instant.parse("2026-03-29T00:00:00Z"));
        return entity;
    }
}

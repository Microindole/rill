package com.indolyn.rill.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.indolyn.rill.app.dto.ExportTaskRequest;
import com.indolyn.rill.app.dto.ExportTaskResponse;
import com.indolyn.rill.app.persistence.entity.ExportTaskEntity;
import com.indolyn.rill.app.persistence.mapper.ExportTaskMapper;
import com.indolyn.rill.app.service.impl.ExportTaskServiceImpl;

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
        CurrentUserProvider currentUserProvider = Mockito.mock(CurrentUserProvider.class);
        ExportTaskJobPublisher exportTaskJobPublisher = Mockito.mock(ExportTaskJobPublisher.class);
        when(currentUserProvider.requireCurrentUserId()).thenReturn(1L);
        ExportTaskService service =
            new ExportTaskServiceImpl(exportTaskMapper, currentUserProvider, exportTaskJobPublisher, tempDir.toString());

        ExportTaskResponse response =
            service.createTask(new ExportTaskRequest("Export users", "csv export", "demo", "select * from users;", "csv"));

        assertEquals("PENDING", response.status());
        assertEquals("csv", response.exportFormat());
        verify(exportTaskMapper).insert(any(ExportTaskEntity.class));
    }

    @Test
    void runTaskShouldQueueTaskAndPublishMessage() {
        ExportTaskMapper exportTaskMapper = Mockito.mock(ExportTaskMapper.class);
        CurrentUserProvider currentUserProvider = Mockito.mock(CurrentUserProvider.class);
        ExportTaskJobPublisher exportTaskJobPublisher = Mockito.mock(ExportTaskJobPublisher.class);
        when(currentUserProvider.requireCurrentUserId()).thenReturn(1L);
        ExportTaskService service =
            new ExportTaskServiceImpl(exportTaskMapper, currentUserProvider, exportTaskJobPublisher, tempDir.toString());
        ExportTaskEntity entity = task(1L, "csv");
        ExportTaskEntity queued = task(1L, "csv");
        queued.setStatus("QUEUED");
        when(exportTaskMapper.selectOne(any())).thenReturn(entity, queued);

        ExportTaskResponse response = service.runTask(1L);

        assertEquals("QUEUED", response.status());
        verify(exportTaskMapper).updateById(any(ExportTaskEntity.class));
        verify(exportTaskJobPublisher).publish(1L);
    }

    @Test
    void invalidFormatShouldRejectRequest() {
        ExportTaskMapper exportTaskMapper = Mockito.mock(ExportTaskMapper.class);
        CurrentUserProvider currentUserProvider = Mockito.mock(CurrentUserProvider.class);
        ExportTaskJobPublisher exportTaskJobPublisher = Mockito.mock(ExportTaskJobPublisher.class);
        when(currentUserProvider.requireCurrentUserId()).thenReturn(1L);
        ExportTaskService service =
            new ExportTaskServiceImpl(exportTaskMapper, currentUserProvider, exportTaskJobPublisher, tempDir.toString());

        ResponseStatusException exception =
            assertThrows(
                ResponseStatusException.class,
                () -> service.createTask(new ExportTaskRequest("Export users", "", "demo", "select 1;", "xml")));

        assertEquals(400, exception.getStatusCode().value());
    }

    @Test
    void resolveDownloadPathShouldReturnExistingCompletedExportFile() throws Exception {
        ExportTaskMapper exportTaskMapper = Mockito.mock(ExportTaskMapper.class);
        CurrentUserProvider currentUserProvider = Mockito.mock(CurrentUserProvider.class);
        ExportTaskJobPublisher exportTaskJobPublisher = Mockito.mock(ExportTaskJobPublisher.class);
        when(currentUserProvider.requireCurrentUserId()).thenReturn(1L);
        ExportTaskService service =
            new ExportTaskServiceImpl(exportTaskMapper, currentUserProvider, exportTaskJobPublisher, tempDir.toString());

        Path filePath = tempDir.resolve("export-task-1.csv");
        java.nio.file.Files.writeString(filePath, "id,name\n1,alice\n");
        ExportTaskEntity entity = task(1L, "csv");
        entity.setStatus("COMPLETED");
        entity.setOutputPath(filePath.toAbsolutePath().toString());
        when(exportTaskMapper.selectOne(any())).thenReturn(entity);

        Path resolved = service.resolveDownloadPath(1L);
        assertEquals(filePath.toAbsolutePath().normalize(), resolved);
    }

    private ExportTaskEntity task(Long id, String format) {
        ExportTaskEntity entity = new ExportTaskEntity();
        entity.setId(id);
        entity.setOwnerId(1L);
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

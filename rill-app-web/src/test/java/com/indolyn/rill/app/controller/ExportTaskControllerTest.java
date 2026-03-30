package com.indolyn.rill.app.controller;

import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.indolyn.rill.app.dto.ExportTaskRequest;
import com.indolyn.rill.app.dto.ExportTaskResponse;
import com.indolyn.rill.app.service.ExportTaskService;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

class ExportTaskControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void listTasksShouldReturnJsonArray() throws Exception {
        ExportTaskService service = Mockito.mock(ExportTaskService.class);
        MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new ExportTaskController(service), new RestExceptionHandler()).build();
        when(service.listTasks()).thenReturn(List.of(task(1L, "PENDING", null, null)));

        mockMvc
            .perform(get("/api/workspace/export-tasks"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].title").value("Export users"));
    }

    @Test
    void createTaskShouldReturnCreated() throws Exception {
        ExportTaskService service = Mockito.mock(ExportTaskService.class);
        MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new ExportTaskController(service), new RestExceptionHandler()).build();
        when(service.createTask(new ExportTaskRequest("Export users", "csv export", "demo", "select * from users;", "csv")))
            .thenReturn(task(1L, "PENDING", null, null));

        mockMvc
            .perform(
                post("/api/workspace/export-tasks")
                    .contentType(APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            new ExportTaskRequest("Export users", "csv export", "demo", "select * from users;", "csv"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void runTaskShouldReturnCompletedTask() throws Exception {
        ExportTaskService service = Mockito.mock(ExportTaskService.class);
        MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new ExportTaskController(service), new RestExceptionHandler()).build();
        when(service.runTask(1L)).thenReturn(task(1L, "COMPLETED", "target/exports/export-task-1.csv", null));

        mockMvc
            .perform(post("/api/workspace/export-tasks/1/run"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.outputPath").value("target/exports/export-task-1.csv"));
    }

    @Test
    void updateMissingTaskShouldReturnStructuredError() throws Exception {
        ExportTaskService service = Mockito.mock(ExportTaskService.class);
        MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new ExportTaskController(service), new RestExceptionHandler()).build();
        when(service.updateTask(Mockito.eq(9L), Mockito.any()))
            .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Export task not found"));

        mockMvc
            .perform(
                put("/api/workspace/export-tasks/9")
                    .contentType(APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            new ExportTaskRequest("Export users", "", "demo", "select * from users;", "csv"))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Export task not found"));
    }

    @Test
    void deleteTaskShouldReturnNoContent() throws Exception {
        ExportTaskService service = Mockito.mock(ExportTaskService.class);
        MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new ExportTaskController(service), new RestExceptionHandler()).build();

        mockMvc.perform(delete("/api/workspace/export-tasks/1")).andExpect(status().isNoContent());
    }

    private ExportTaskResponse task(Long id, String status, String outputPath, String lastError) {
        return new ExportTaskResponse(
            id,
            "Export users",
            "csv export",
            "demo",
            "select * from users;",
            "csv",
            status,
            outputPath,
            lastError,
            Instant.parse("2026-03-29T00:00:00Z"),
            Instant.parse("2026-03-29T00:00:00Z"),
            "COMPLETED".equals(status) ? Instant.parse("2026-03-29T00:01:00Z") : null);
    }
}

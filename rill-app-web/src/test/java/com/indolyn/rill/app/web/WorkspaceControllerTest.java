package com.indolyn.rill.app.web;

import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.indolyn.rill.app.dto.QueryExecuteResponse;
import com.indolyn.rill.app.dto.QueryHistoryItemResponse;
import com.indolyn.rill.app.dto.WorkspaceExecuteRequest;
import com.indolyn.rill.app.dto.WorkspaceSessionResponse;
import com.indolyn.rill.app.service.WorkspaceService;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

class WorkspaceControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createSessionShouldReturnCreatedWorkspaceSnapshot() throws Exception {
        WorkspaceService workspaceService = Mockito.mock(WorkspaceService.class);
        MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new WorkspaceController(workspaceService), new RestExceptionHandler())
                .build();
        when(workspaceService.createSession())
            .thenReturn(
                new WorkspaceSessionResponse(
                    "session-1",
                    "default",
                    Instant.parse("2026-03-29T00:00:00Z"),
                    Instant.parse("2026-03-29T00:00:00Z"),
                    List.of("default"),
                    List.of()));

        mockMvc
            .perform(post("/api/workspace/sessions"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sessionId").value("session-1"))
            .andExpect(jsonPath("$.currentDatabase").value("default"));
    }

    @Test
    void listSessionsShouldReturnSessionSummaries() throws Exception {
        WorkspaceService workspaceService = Mockito.mock(WorkspaceService.class);
        MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new WorkspaceController(workspaceService), new RestExceptionHandler())
                .build();
        when(workspaceService.listSessions())
            .thenReturn(
                List.of(
                    new com.indolyn.rill.app.dto.WorkspaceSessionSummaryResponse(
                        "session-1",
                        "demo",
                        Instant.parse("2026-03-29T00:00:00Z"),
                        Instant.parse("2026-03-29T00:05:00Z"),
                        3)));

        mockMvc
            .perform(get("/api/workspace/sessions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].sessionId").value("session-1"))
            .andExpect(jsonPath("$[0].recentQueryCount").value(3));
    }

    @Test
    void getSessionShouldReturnSnapshot() throws Exception {
        WorkspaceService workspaceService = Mockito.mock(WorkspaceService.class);
        MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new WorkspaceController(workspaceService), new RestExceptionHandler())
                .build();
        when(workspaceService.getSession("session-1"))
            .thenReturn(
                new WorkspaceSessionResponse(
                    "session-1",
                    "demo",
                    Instant.parse("2026-03-29T00:00:00Z"),
                    Instant.parse("2026-03-29T00:01:00Z"),
                    List.of("default", "demo"),
                    List.of(
                        new QueryHistoryItemResponse(
                            "trace-1",
                            "demo",
                            "show tables;",
                            true,
                            4L,
                            Instant.parse("2026-03-29T00:01:00Z")))));

        mockMvc
            .perform(get("/api/workspace/sessions/session-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentDatabase").value("demo"))
            .andExpect(jsonPath("$.recentQueries[0].sql").value("show tables;"));
    }

    @Test
    void getSessionHistoryShouldReturnHistoryRows() throws Exception {
        WorkspaceService workspaceService = Mockito.mock(WorkspaceService.class);
        MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new WorkspaceController(workspaceService), new RestExceptionHandler())
                .build();
        when(workspaceService.getSessionHistory("session-1"))
            .thenReturn(
                List.of(
                    new QueryHistoryItemResponse(
                        "trace-2",
                        "demo",
                        "select * from users;",
                        true,
                        7L,
                        Instant.parse("2026-03-29T00:02:00Z"))));

        mockMvc
            .perform(get("/api/workspace/sessions/session-1/history"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].traceId").value("trace-2"))
            .andExpect(jsonPath("$[0].sql").value("select * from users;"));
    }

    @Test
    void executeShouldRejectBlankSql() throws Exception {
        WorkspaceService workspaceService = Mockito.mock(WorkspaceService.class);
        MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new WorkspaceController(workspaceService), new RestExceptionHandler())
                .build();

        mockMvc
            .perform(
                post("/api/workspace/sessions/session-1/execute")
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new WorkspaceExecuteRequest("   "))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("SQL cannot be empty"));
    }

    @Test
    void executeShouldReturnQueryResponse() throws Exception {
        WorkspaceService workspaceService = Mockito.mock(WorkspaceService.class);
        MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new WorkspaceController(workspaceService), new RestExceptionHandler())
                .build();
        when(workspaceService.execute("session-1", "show tables;"))
            .thenReturn(
                new QueryExecuteResponse(
                    "trace-1",
                    "demo",
                    "show tables;",
                    true,
                    6L,
                    Instant.parse("2026-03-29T00:00:00Z"),
                    "OK",
                    List.of("Tables"),
                    List.of(List.of("users")),
                    List.of()));

        mockMvc
            .perform(
                post("/api/workspace/sessions/session-1/execute")
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new WorkspaceExecuteRequest("show tables;"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.traceId").value("trace-1"))
            .andExpect(jsonPath("$.dbName").value("demo"))
            .andExpect(jsonPath("$.rows[0][0]").value("users"));
    }

    @Test
    void deleteSessionShouldReturnNoContent() throws Exception {
        WorkspaceService workspaceService = Mockito.mock(WorkspaceService.class);
        MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new WorkspaceController(workspaceService), new RestExceptionHandler())
                .build();

        mockMvc.perform(delete("/api/workspace/sessions/session-1")).andExpect(status().isNoContent());
    }

    @Test
    void missingWorkspaceSessionShouldReturnStructuredError() throws Exception {
        WorkspaceService workspaceService = Mockito.mock(WorkspaceService.class);
        MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new WorkspaceController(workspaceService), new RestExceptionHandler())
                .build();
        when(workspaceService.getSession("missing"))
            .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Workspace session not found"));

        mockMvc
            .perform(get("/api/workspace/sessions/missing"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Workspace session not found"));
    }
}

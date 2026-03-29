package com.indolyn.rill.app.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.indolyn.rill.app.dto.QueryHistoryItemResponse;
import com.indolyn.rill.app.dto.WorkspaceDashboardResponse;
import com.indolyn.rill.app.dto.WorkspaceSessionSummaryResponse;
import com.indolyn.rill.app.service.WorkspaceDashboardService;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class WorkspaceDashboardControllerTest {

    @Test
    void dashboardShouldReturnWorkspaceAggregates() throws Exception {
        WorkspaceDashboardService workspaceDashboardService = Mockito.mock(WorkspaceDashboardService.class);
        MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new WorkspaceDashboardController(workspaceDashboardService)).build();
        when(workspaceDashboardService.getDashboard())
            .thenReturn(
                new WorkspaceDashboardResponse(
                    1,
                    2,
                    3,
                    4,
                    List.of("default", "demo"),
                    List.of(
                        new WorkspaceSessionSummaryResponse(
                            "session-1",
                            "demo",
                            Instant.parse("2026-03-29T00:00:00Z"),
                            Instant.parse("2026-03-29T00:05:00Z"),
                            2)),
                    List.of(
                        new QueryHistoryItemResponse(
                            "trace-1",
                            "demo",
                            "show tables;",
                            true,
                            4L,
                            Instant.parse("2026-03-29T00:05:00Z")))));

        mockMvc
            .perform(get("/api/workspace/dashboard"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalSessions").value(1))
            .andExpect(jsonPath("$.totalSnippets").value(3))
            .andExpect(jsonPath("$.sessions[0].sessionId").value("session-1"))
            .andExpect(jsonPath("$.recentQueries[0].traceId").value("trace-1"));
    }
}

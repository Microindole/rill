package com.indolyn.rill.app.web;

import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.indolyn.rill.app.dto.DemoScenarioRequest;
import com.indolyn.rill.app.dto.DemoScenarioResponse;
import com.indolyn.rill.app.dto.DemoScenarioRunResponse;
import com.indolyn.rill.app.dto.QueryExecuteResponse;
import com.indolyn.rill.app.service.DemoScenarioService;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

class DemoScenarioControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void listScenariosShouldReturnJsonArray() throws Exception {
        DemoScenarioService service = Mockito.mock(DemoScenarioService.class);
        MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new DemoScenarioController(service), new RestExceptionHandler()).build();
        when(service.listScenarios())
            .thenReturn(
                List.of(
                    new DemoScenarioResponse(
                        1L,
                        "Bootstrap",
                        "Create demo data",
                        "create table users (id int);",
                        Instant.parse("2026-03-29T00:00:00Z"),
                        Instant.parse("2026-03-29T00:00:00Z"))));

        mockMvc
            .perform(get("/api/workspace/scenarios"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].title").value("Bootstrap"));
    }

    @Test
    void createScenarioShouldReturnCreated() throws Exception {
        DemoScenarioService service = Mockito.mock(DemoScenarioService.class);
        MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new DemoScenarioController(service), new RestExceptionHandler()).build();
        when(service.createScenario(new DemoScenarioRequest("Bootstrap", "Create demo data", "create table users (id int);")))
            .thenReturn(
                new DemoScenarioResponse(
                    1L,
                    "Bootstrap",
                    "Create demo data",
                    "create table users (id int);",
                    Instant.parse("2026-03-29T00:00:00Z"),
                    Instant.parse("2026-03-29T00:00:00Z")));

        mockMvc
            .perform(
                post("/api/workspace/scenarios")
                    .contentType(APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            new DemoScenarioRequest("Bootstrap", "Create demo data", "create table users (id int);"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void runScenarioShouldReturnExecutionSummary() throws Exception {
        DemoScenarioService service = Mockito.mock(DemoScenarioService.class);
        MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new DemoScenarioController(service), new RestExceptionHandler()).build();
        when(service.runScenario("session-1", 1L))
            .thenReturn(
                new DemoScenarioRunResponse(
                    1L,
                    "session-1",
                    2,
                    "demo",
                    List.of(
                        new QueryExecuteResponse(
                            "trace-1",
                            "demo",
                            "use demo;",
                            true,
                            5L,
                            Instant.parse("2026-03-29T00:00:00Z"),
                            "OK",
                            List.of(),
                            List.of(),
                            List.of()))));

        mockMvc
            .perform(post("/api/workspace/scenarios/1/run/session-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statementsExecuted").value(2))
            .andExpect(jsonPath("$.finalDatabase").value("demo"));
    }

    @Test
    void updateMissingScenarioShouldReturnStructuredError() throws Exception {
        DemoScenarioService service = Mockito.mock(DemoScenarioService.class);
        MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new DemoScenarioController(service), new RestExceptionHandler()).build();
        when(service.updateScenario(Mockito.eq(9L), Mockito.any()))
            .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Demo scenario not found"));

        mockMvc
            .perform(
                put("/api/workspace/scenarios/9")
                    .contentType(APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            new DemoScenarioRequest("Bootstrap", "", "create table users (id int);"))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Demo scenario not found"));
    }

    @Test
    void deleteScenarioShouldReturnNoContent() throws Exception {
        DemoScenarioService service = Mockito.mock(DemoScenarioService.class);
        MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new DemoScenarioController(service), new RestExceptionHandler()).build();

        mockMvc.perform(delete("/api/workspace/scenarios/1")).andExpect(status().isNoContent());
    }
}

package com.indolyn.rill.app.controller;

import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.indolyn.rill.app.dto.QueryExecuteRequest;
import com.indolyn.rill.app.dto.QueryExecuteResponse;
import com.indolyn.rill.app.dto.QueryHistoryItemResponse;
import com.indolyn.rill.app.dto.QueryTraceStepResponse;
import com.indolyn.rill.app.service.QueryTraceService;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class QueryControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void executeShouldRejectBlankSql() throws Exception {
        QueryTraceService queryTraceService = Mockito.mock(QueryTraceService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new QueryController(queryTraceService)).build();

        mockMvc
            .perform(
                post("/api/query/execute")
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new QueryExecuteRequest("default", "   "))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void executeShouldReturnTracePayload() throws Exception {
        QueryTraceService queryTraceService = Mockito.mock(QueryTraceService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new QueryController(queryTraceService)).build();
        QueryExecuteResponse response =
            new QueryExecuteResponse(
                "trace-1",
                "default",
                "select 1",
                true,
                8L,
                Instant.parse("2026-03-28T00:00:00Z"),
                "OK",
                List.of("value"),
                List.of(List.of("1")),
                List.of(
                    new QueryTraceStepResponse(
                        "lexer-0",
                        "lexer",
                        "词法分析",
                        "Lexer",
                        "completed",
                        1L,
                        "Lexer.java",
                        "Lexer",
                        "tokenize",
                        "生成 1 个 token。")));

        when(queryTraceService.execute("default", "select 1")).thenReturn(response);

        mockMvc
            .perform(
                post("/api/query/execute")
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new QueryExecuteRequest("default", "select 1"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.traceId").value("trace-1"))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.rows[0][0]").value("1"));
    }

    @Test
    void historyShouldReturnLatestItems() throws Exception {
        QueryTraceService queryTraceService = Mockito.mock(QueryTraceService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new QueryController(queryTraceService)).build();

        when(queryTraceService.getHistory())
            .thenReturn(
                List.of(
                    new QueryHistoryItemResponse(
                        "trace-1",
                        "default",
                        "select 1",
                        true,
                        4L,
                        Instant.parse("2026-03-28T00:00:00Z"))));

        mockMvc
            .perform(get("/api/query/history"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].traceId").value("trace-1"))
            .andExpect(jsonPath("$[0].sql").value("select 1"));
    }

    @Test
    void traceShouldReturnNotFoundWhenMissing() throws Exception {
        QueryTraceService queryTraceService = Mockito.mock(QueryTraceService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new QueryController(queryTraceService)).build();

        when(queryTraceService.getTrace("missing")).thenReturn(null);

        mockMvc.perform(get("/api/query/trace/missing")).andExpect(status().isNotFound());
    }
}

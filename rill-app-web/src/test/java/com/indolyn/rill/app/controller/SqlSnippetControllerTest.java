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
import com.indolyn.rill.app.dto.SqlSnippetRequest;
import com.indolyn.rill.app.dto.SqlSnippetResponse;
import com.indolyn.rill.app.service.SqlSnippetService;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

class SqlSnippetControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void listSnippetsShouldReturnJsonArray() throws Exception {
        SqlSnippetService service = Mockito.mock(SqlSnippetService.class);
        MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new SqlSnippetController(service), new RestExceptionHandler()).build();
        when(service.listSnippets())
            .thenReturn(
                List.of(
                    new SqlSnippetResponse(
                        1L,
                        "Users",
                        "List users",
                        "select * from users;",
                        Instant.parse("2026-03-29T00:00:00Z"),
                        Instant.parse("2026-03-29T00:00:00Z"))));

        mockMvc
            .perform(get("/api/workspace/snippets"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].title").value("Users"));
    }

    @Test
    void createSnippetShouldReturnCreated() throws Exception {
        SqlSnippetService service = Mockito.mock(SqlSnippetService.class);
        MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new SqlSnippetController(service), new RestExceptionHandler()).build();
        when(service.createSnippet(new SqlSnippetRequest("Users", "List users", "select * from users;")))
            .thenReturn(
                new SqlSnippetResponse(
                    1L,
                    "Users",
                    "List users",
                    "select * from users;",
                    Instant.parse("2026-03-29T00:00:00Z"),
                    Instant.parse("2026-03-29T00:00:00Z")));

        mockMvc
            .perform(
                post("/api/workspace/snippets")
                    .contentType(APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            new SqlSnippetRequest("Users", "List users", "select * from users;"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void updateMissingSnippetShouldReturnStructuredError() throws Exception {
        SqlSnippetService service = Mockito.mock(SqlSnippetService.class);
        MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new SqlSnippetController(service), new RestExceptionHandler()).build();
        when(service.updateSnippet(Mockito.eq(9L), Mockito.any()))
            .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "SQL snippet not found"));

        mockMvc
            .perform(
                put("/api/workspace/snippets/9")
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new SqlSnippetRequest("Users", "", "select 1;"))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("SQL snippet not found"));
    }

    @Test
    void deleteSnippetShouldReturnNoContent() throws Exception {
        SqlSnippetService service = Mockito.mock(SqlSnippetService.class);
        MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new SqlSnippetController(service), new RestExceptionHandler()).build();

        mockMvc.perform(delete("/api/workspace/snippets/1")).andExpect(status().isNoContent());
    }
}

package com.indolyn.rill.app.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.indolyn.rill.app.dto.SystemOverviewResponse;
import com.indolyn.rill.app.service.OverviewService;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class OverviewControllerTest {

    @Test
    void overviewShouldReturnOverviewPayload() throws Exception {
        OverviewService overviewService = Mockito.mock(OverviewService.class);
        MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new OverviewController(overviewService)).build();

        when(overviewService.getOverview())
            .thenReturn(new SystemOverviewResponse("Rill", "演示台", "desc", List.of(), List.of(), List.of(), List.of()));

        mockMvc
            .perform(get("/api/overview"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.appName").value("Rill"))
            .andExpect(jsonPath("$.stage").value("演示台"));
    }
}

package com.indolyn.rill.app.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.indolyn.rill.app.dto.AdminUserResponse;
import com.indolyn.rill.app.service.AdminUserService;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminUserControllerTest {

    @Test
    void listUsersShouldReturnPayload() throws Exception {
        AdminUserService adminUserService = Mockito.mock(AdminUserService.class);
        MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new AdminUserController(adminUserService), new RestExceptionHandler()).build();
        when(adminUserService.listUsers())
            .thenReturn(List.of(new AdminUserResponse(1L, "demo", "demo@example.com", true, "Demo", "ADMIN", "demo", true)));

        mockMvc
            .perform(get("/api/admin/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].username").value("demo"));
    }

    @Test
    void provisionShouldReturnUpdatedUser() throws Exception {
        AdminUserService adminUserService = Mockito.mock(AdminUserService.class);
        MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new AdminUserController(adminUserService), new RestExceptionHandler()).build();
        when(adminUserService.provisionUserDatabase(2L))
            .thenReturn(new AdminUserResponse(2L, "alice", "alice@example.com", true, "Alice", "USER", "alice", true));

        mockMvc
            .perform(post("/api/admin/users/2/database/provision"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.kernelDbProvisioned").value(true));
    }

    @Test
    void deleteShouldReturnOk() throws Exception {
        AdminUserService adminUserService = Mockito.mock(AdminUserService.class);
        MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new AdminUserController(adminUserService), new RestExceptionHandler()).build();

        mockMvc.perform(delete("/api/admin/users/2/database")).andExpect(status().isOk());
    }
}

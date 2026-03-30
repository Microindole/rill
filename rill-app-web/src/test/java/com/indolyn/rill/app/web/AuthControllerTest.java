package com.indolyn.rill.app.web;

import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.indolyn.rill.app.dto.LoginRequest;
import com.indolyn.rill.app.persistence.entity.AppUserEntity;
import com.indolyn.rill.app.service.CurrentUserProvider;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

class AuthControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void loginShouldReturnCurrentUserPayload() throws Exception {
        CurrentUserProvider currentUserProvider = Mockito.mock(CurrentUserProvider.class);
        MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new AuthController(currentUserProvider), new RestExceptionHandler())
                .build();
        when(currentUserProvider.login("demo", "demo123")).thenReturn(user(1L, "demo", "Demo Admin", "ADMIN"));

        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new LoginRequest("demo", "demo123"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(1L))
            .andExpect(jsonPath("$.username").value("demo"))
            .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void currentUserShouldReturnResolvedUser() throws Exception {
        CurrentUserProvider currentUserProvider = Mockito.mock(CurrentUserProvider.class);
        MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new AuthController(currentUserProvider), new RestExceptionHandler())
                .build();
        when(currentUserProvider.requireCurrentUser()).thenReturn(user(1L, "demo", "Demo Admin", "ADMIN"));

        mockMvc
            .perform(get("/api/auth/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.displayName").value("Demo Admin"));
    }

    @Test
    void invalidLoginShouldReturnStructuredError() throws Exception {
        CurrentUserProvider currentUserProvider = Mockito.mock(CurrentUserProvider.class);
        MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new AuthController(currentUserProvider), new RestExceptionHandler())
                .build();
        when(currentUserProvider.login("demo", "wrong"))
            .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Invalid username or password"));

        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new LoginRequest("demo", "wrong"))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    private AppUserEntity user(Long id, String username, String displayName, String role) {
        AppUserEntity user = new AppUserEntity();
        user.setId(id);
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setRole(role);
        return user;
    }
}

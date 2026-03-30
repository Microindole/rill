package com.indolyn.rill.app.controller;

import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.indolyn.rill.app.dto.LoginRequest;
import com.indolyn.rill.app.dto.RegisterRequest;
import com.indolyn.rill.app.service.AuthService;
import com.indolyn.rill.app.service.AuthenticatedUser;
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
        AuthService authService = Mockito.mock(AuthService.class);
        CurrentUserProvider currentUserProvider = Mockito.mock(CurrentUserProvider.class);
        MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new AuthController(authService, currentUserProvider), new RestExceptionHandler())
                .build();
        when(authService.login("demo", "demo123"))
            .thenReturn(new AuthenticatedUser(user(1L, "demo", "Demo Admin", "ADMIN", "demo"), "token-demo"));

        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new LoginRequest("demo", "demo123"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(1L))
            .andExpect(jsonPath("$.username").value("demo"))
            .andExpect(jsonPath("$.role").value("ADMIN"))
            .andExpect(jsonPath("$.kernelDbName").value("demo"))
            .andExpect(jsonPath("$.token").value("token-demo"))
            .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void registerShouldCreateSessionUserPayload() throws Exception {
        AuthService authService = Mockito.mock(AuthService.class);
        CurrentUserProvider currentUserProvider = Mockito.mock(CurrentUserProvider.class);
        MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new AuthController(authService, currentUserProvider), new RestExceptionHandler())
                .build();
        when(authService.register("newuser", "New User", "secret"))
            .thenReturn(new AuthenticatedUser(user(2L, "newuser", "New User", "USER", "newuser"), "token-new"));

        mockMvc
            .perform(
                post("/api/auth/register")
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new RegisterRequest("newuser", "New User", "secret"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(2L))
            .andExpect(jsonPath("$.username").value("newuser"))
            .andExpect(jsonPath("$.role").value("USER"))
            .andExpect(jsonPath("$.kernelDbName").value("newuser"))
            .andExpect(jsonPath("$.token").value("token-new"));
    }

    @Test
    void currentUserShouldReturnResolvedUser() throws Exception {
        AuthService authService = Mockito.mock(AuthService.class);
        CurrentUserProvider currentUserProvider = Mockito.mock(CurrentUserProvider.class);
        MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new AuthController(authService, currentUserProvider), new RestExceptionHandler())
                .build();
        when(currentUserProvider.requireCurrentUser()).thenReturn(user(1L, "demo", "Demo Admin", "ADMIN", "demo"));

        mockMvc
            .perform(get("/api/auth/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.displayName").value("Demo Admin"))
            .andExpect(jsonPath("$.kernelDbName").value("demo"));
    }

    @Test
    void invalidLoginShouldReturnStructuredError() throws Exception {
        AuthService authService = Mockito.mock(AuthService.class);
        CurrentUserProvider currentUserProvider = Mockito.mock(CurrentUserProvider.class);
        MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new AuthController(authService, currentUserProvider), new RestExceptionHandler())
                .build();
        when(authService.login("demo", "wrong"))
            .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Invalid username or password"));

        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new LoginRequest("demo", "wrong"))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void logoutShouldReturnNoContent() throws Exception {
        AuthService authService = Mockito.mock(AuthService.class);
        CurrentUserProvider currentUserProvider = Mockito.mock(CurrentUserProvider.class);
        MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new AuthController(authService, currentUserProvider), new RestExceptionHandler())
                .build();

        mockMvc.perform(delete("/api/auth/logout")).andExpect(status().isOk());
    }

    private AppUserEntity user(Long id, String username, String displayName, String role, String kernelDbName) {
        AppUserEntity user = new AppUserEntity();
        user.setId(id);
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setRole(role);
        user.setKernelDbName(kernelDbName);
        return user;
    }
}

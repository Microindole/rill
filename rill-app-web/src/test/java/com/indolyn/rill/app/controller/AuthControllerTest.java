package com.indolyn.rill.app.controller;

import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.indolyn.rill.app.dto.EmailVerificationConfirmRequest;
import com.indolyn.rill.app.dto.LoginRequest;
import com.indolyn.rill.app.dto.PasswordChangeRequest;
import com.indolyn.rill.app.dto.PasswordResetConfirmRequest;
import com.indolyn.rill.app.dto.PasswordResetRequest;
import com.indolyn.rill.app.dto.RegisterRequest;
import com.indolyn.rill.app.persistence.entity.AppUserEntity;
import com.indolyn.rill.app.service.AuthService;
import com.indolyn.rill.app.service.AuthenticatedUser;
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
        MockMvc mockMvc = mockMvc(authService, currentUserProvider);
        AppUserEntity user = user(1L, "demo", "demo@example.com", "Demo Admin", "ADMIN", "demo");
        user.setEmailVerified(true);
        when(authService.login("demo", "demo123", "captcha-ok")).thenReturn(new AuthenticatedUser(user, "token-demo"));

        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new LoginRequest("demo", "demo123", "captcha-ok"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(1L))
            .andExpect(jsonPath("$.username").value("demo"))
            .andExpect(jsonPath("$.email").value("demo@example.com"))
            .andExpect(jsonPath("$.emailVerified").value(true))
            .andExpect(jsonPath("$.token").value("token-demo"));
    }

    @Test
    void registerShouldReturnMessage() throws Exception {
        AuthService authService = Mockito.mock(AuthService.class);
        CurrentUserProvider currentUserProvider = Mockito.mock(CurrentUserProvider.class);
        MockMvc mockMvc = mockMvc(authService, currentUserProvider);

        mockMvc
            .perform(
                post("/api/auth/register")
                    .contentType(APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            new RegisterRequest("newuser", "newuser@example.com", "New User", "secret"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Verification email sent"));
    }

    @Test
    void confirmRegisterShouldIssueToken() throws Exception {
        AuthService authService = Mockito.mock(AuthService.class);
        CurrentUserProvider currentUserProvider = Mockito.mock(CurrentUserProvider.class);
        MockMvc mockMvc = mockMvc(authService, currentUserProvider);
        AppUserEntity user = user(2L, "newuser", "newuser@example.com", "New User", "USER", "newuser");
        user.setEmailVerified(true);
        when(authService.confirmEmail("verify-token")).thenReturn(new AuthenticatedUser(user, "token-new"));

        mockMvc
            .perform(
                post("/api/auth/register/confirm")
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new EmailVerificationConfirmRequest("verify-token"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("newuser"))
            .andExpect(jsonPath("$.token").value("token-new"));
    }

    @Test
    void currentUserShouldReturnResolvedUser() throws Exception {
        AuthService authService = Mockito.mock(AuthService.class);
        CurrentUserProvider currentUserProvider = Mockito.mock(CurrentUserProvider.class);
        MockMvc mockMvc = mockMvc(authService, currentUserProvider);
        AppUserEntity current = user(1L, "demo", "demo@example.com", "Demo Admin", "ADMIN", "demo");
        current.setEmailVerified(true);
        when(currentUserProvider.requireCurrentUser()).thenReturn(current);

        mockMvc
            .perform(get("/api/auth/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("demo@example.com"))
            .andExpect(jsonPath("$.emailVerified").value(true))
            .andExpect(jsonPath("$.kernelDbName").value("demo"));
    }

    @Test
    void invalidLoginShouldReturnStructuredError() throws Exception {
        AuthService authService = Mockito.mock(AuthService.class);
        CurrentUserProvider currentUserProvider = Mockito.mock(CurrentUserProvider.class);
        MockMvc mockMvc = mockMvc(authService, currentUserProvider);
        when(authService.login("demo", "wrong", "captcha-ok"))
            .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Invalid username or password"));

        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new LoginRequest("demo", "wrong", "captcha-ok"))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void requestPasswordResetShouldReturnMessage() throws Exception {
        AuthService authService = Mockito.mock(AuthService.class);
        CurrentUserProvider currentUserProvider = Mockito.mock(CurrentUserProvider.class);
        MockMvc mockMvc = mockMvc(authService, currentUserProvider);

        mockMvc
            .perform(
                post("/api/auth/password/reset/request")
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new PasswordResetRequest("demo@example.com"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Password reset email sent if the account exists"));
    }

    @Test
    void confirmPasswordResetShouldReturnMessage() throws Exception {
        AuthService authService = Mockito.mock(AuthService.class);
        CurrentUserProvider currentUserProvider = Mockito.mock(CurrentUserProvider.class);
        MockMvc mockMvc = mockMvc(authService, currentUserProvider);

        mockMvc
            .perform(
                post("/api/auth/password/reset/confirm")
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new PasswordResetConfirmRequest("token-reset", "newpass"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Password reset successfully"));
    }

    @Test
    void requestPasswordChangeShouldUseCurrentUser() throws Exception {
        AuthService authService = Mockito.mock(AuthService.class);
        CurrentUserProvider currentUserProvider = Mockito.mock(CurrentUserProvider.class);
        MockMvc mockMvc = mockMvc(authService, currentUserProvider);
        when(currentUserProvider.requireAuthenticatedUser())
            .thenReturn(new AuthenticatedUser(user(9L, "demo", "demo@example.com", "Demo Admin", "ADMIN", "demo"), "token"));

        mockMvc
            .perform(
                post("/api/auth/password/change/request")
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new PasswordChangeRequest("old", "new"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Password change verification email sent"));
    }

    @Test
    void authConfigShouldExposeCaptchaState() throws Exception {
        AuthService authService = Mockito.mock(AuthService.class);
        CurrentUserProvider currentUserProvider = Mockito.mock(CurrentUserProvider.class);
        MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new AuthController(authService, currentUserProvider, true, "turnstile", "site-key"), new RestExceptionHandler())
                .build();

        mockMvc
            .perform(get("/api/auth/config"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.captchaEnabled").value(true))
            .andExpect(jsonPath("$.captchaProvider").value("turnstile"))
            .andExpect(jsonPath("$.captchaSiteKey").value("site-key"));
    }

    @Test
    void logoutShouldReturnOk() throws Exception {
        AuthService authService = Mockito.mock(AuthService.class);
        CurrentUserProvider currentUserProvider = Mockito.mock(CurrentUserProvider.class);
        MockMvc mockMvc = mockMvc(authService, currentUserProvider);

        mockMvc.perform(delete("/api/auth/logout")).andExpect(status().isOk());
    }

    private MockMvc mockMvc(AuthService authService, CurrentUserProvider currentUserProvider) {
        return MockMvcBuilders.standaloneSetup(new AuthController(authService, currentUserProvider, false, "turnstile", ""), new RestExceptionHandler())
            .build();
    }

    private AppUserEntity user(Long id, String username, String email, String displayName, String role, String kernelDbName) {
        AppUserEntity user = new AppUserEntity();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setDisplayName(displayName);
        user.setRole(role);
        user.setKernelDbName(kernelDbName);
        return user;
    }
}

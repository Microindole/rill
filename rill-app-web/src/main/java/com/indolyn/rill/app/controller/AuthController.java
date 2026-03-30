package com.indolyn.rill.app.controller;

import com.indolyn.rill.app.dto.CurrentUserResponse;
import com.indolyn.rill.app.dto.LoginRequest;
import com.indolyn.rill.app.dto.LoginResponse;
import com.indolyn.rill.app.dto.RegisterRequest;
import com.indolyn.rill.app.service.AuthService;
import com.indolyn.rill.app.service.AuthenticatedUser;
import com.indolyn.rill.app.service.CurrentUserProvider;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final CurrentUserProvider currentUserProvider;

    public AuthController(AuthService authService, CurrentUserProvider currentUserProvider) {
        this.authService = authService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/register")
    public LoginResponse register(@RequestBody RegisterRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Register request cannot be empty");
        }
        AuthenticatedUser authenticatedUser = authService.register(request.username(), request.displayName(), request.password());
        return toLoginResponse(authenticatedUser);
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        if (request == null || request.username() == null || request.username().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username cannot be empty");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password cannot be empty");
        }
        return toLoginResponse(authService.login(request.username().trim(), request.password()));
    }

    @GetMapping("/me")
    public CurrentUserResponse currentUser() {
        var user = currentUserProvider.requireCurrentUser();
        return new CurrentUserResponse(
            user.getId(), user.getUsername(), user.getDisplayName(), user.getRole(), user.getKernelDbName());
    }

    @DeleteMapping("/logout")
    public void logout() {
        authService.logout();
    }

    private LoginResponse toLoginResponse(AuthenticatedUser authenticatedUser) {
        return new LoginResponse(
            authenticatedUser.user().getId(),
            authenticatedUser.user().getUsername(),
            authenticatedUser.user().getDisplayName(),
            authenticatedUser.user().getRole(),
            authenticatedUser.user().getKernelDbName(),
            authenticatedUser.token(),
            "Bearer");
    }
}

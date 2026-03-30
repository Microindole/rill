package com.indolyn.rill.app.web;

import com.indolyn.rill.app.dto.CurrentUserResponse;
import com.indolyn.rill.app.dto.LoginRequest;
import com.indolyn.rill.app.dto.LoginResponse;
import com.indolyn.rill.app.persistence.entity.AppUserEntity;
import com.indolyn.rill.app.service.CurrentUserProvider;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final CurrentUserProvider currentUserProvider;

    public AuthController(CurrentUserProvider currentUserProvider) {
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        if (request == null || request.username() == null || request.username().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username cannot be empty");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password cannot be empty");
        }
        AppUserEntity user = currentUserProvider.login(request.username().trim(), request.password());
        return new LoginResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getRole());
    }

    @GetMapping("/me")
    public CurrentUserResponse currentUser() {
        AppUserEntity user = currentUserProvider.requireCurrentUser();
        return new CurrentUserResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getRole());
    }
}

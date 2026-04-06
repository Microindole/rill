package com.indolyn.rill.app.controller;

import com.indolyn.rill.app.dto.ActionMessageResponse;
import com.indolyn.rill.app.dto.AuthConfigResponse;
import com.indolyn.rill.app.dto.CurrentUserResponse;
import com.indolyn.rill.app.dto.EmailVerificationConfirmRequest;
import com.indolyn.rill.app.dto.LoginRequest;
import com.indolyn.rill.app.dto.LoginResponse;
import com.indolyn.rill.app.dto.OauthBindAccountRequest;
import com.indolyn.rill.app.dto.OauthCreateAccountRequest;
import com.indolyn.rill.app.dto.OauthPendingStateResponse;
import com.indolyn.rill.app.dto.PasswordChangeRequest;
import com.indolyn.rill.app.dto.PasswordResetConfirmRequest;
import com.indolyn.rill.app.dto.PasswordResetRequest;
import com.indolyn.rill.app.dto.RegisterRequest;
import com.indolyn.rill.app.service.AuthService;
import com.indolyn.rill.app.service.AuthenticatedUser;
import com.indolyn.rill.app.service.CurrentUserProvider;
import com.indolyn.rill.app.service.OauthLoginService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final CurrentUserProvider currentUserProvider;
    private final OauthLoginService oauthLoginService;
    private final boolean captchaEnabled;
    private final String captchaProvider;
    private final String captchaSiteKey;
    private final boolean githubLoginEnabled;

    public AuthController(
        AuthService authService,
        CurrentUserProvider currentUserProvider,
        OauthLoginService oauthLoginService,
        @Value("${app.auth.captcha.enabled:false}") boolean captchaEnabled,
        @Value("${app.auth.captcha.provider:turnstile}") String captchaProvider,
        @Value("${app.auth.captcha.turnstile.site-key:}") String captchaSiteKey,
        @Value("${app.auth.oauth2.github.enabled:false}") boolean githubLoginEnabled) {
        this.authService = authService;
        this.currentUserProvider = currentUserProvider;
        this.oauthLoginService = oauthLoginService;
        this.captchaEnabled = captchaEnabled;
        this.captchaProvider = captchaProvider;
        this.captchaSiteKey = captchaSiteKey;
        this.githubLoginEnabled = githubLoginEnabled;
    }

    @GetMapping("/config")
    public AuthConfigResponse config() {
        return new AuthConfigResponse(captchaEnabled, captchaProvider, captchaSiteKey, githubLoginEnabled);
    }

    @PostMapping("/register")
    public ActionMessageResponse register(@RequestBody RegisterRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Register request cannot be empty");
        }
        authService.register(request.username(), request.email(), request.displayName(), request.password());
        return new ActionMessageResponse("Verification email sent");
    }

    @PostMapping("/register/confirm")
    public LoginResponse confirmRegister(@RequestBody EmailVerificationConfirmRequest request) {
        if (request == null || request.token() == null || request.token().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification token cannot be empty");
        }
        return toLoginResponse(authService.confirmEmail(request.token().trim()));
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        if (request == null || request.username() == null || request.username().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username cannot be empty");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password cannot be empty");
        }
        return toLoginResponse(authService.login(request.username().trim(), request.password(), request.captchaToken()));
    }

    @GetMapping("/oauth2/pending/{state}")
    public OauthPendingStateResponse getOauthPendingState(@PathVariable String state) {
        var pendingState = oauthLoginService.requirePendingState(state);
        return new OauthPendingStateResponse(
            pendingState.state(),
            pendingState.provider(),
            pendingState.providerLogin(),
            pendingState.providerEmail(),
            pendingState.providerDisplayName(),
            pendingState.providerLogin());
    }

    @PostMapping("/oauth2/create")
    public LoginResponse createOauthAccount(@RequestBody OauthCreateAccountRequest request) {
        if (request == null || request.state() == null || request.state().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OAuth2 state cannot be empty");
        }
        return toLoginResponse(
            oauthLoginService.createAccountFromGithub(request.state().trim(), request.username(), request.displayName()));
    }

    @PostMapping("/oauth2/bind")
    public LoginResponse bindOauthAccount(@RequestBody OauthBindAccountRequest request) {
        if (request == null || request.state() == null || request.state().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OAuth2 state cannot be empty");
        }
        if (request.username() == null || request.username().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username cannot be empty");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password cannot be empty");
        }
        return toLoginResponse(
            oauthLoginService.bindGithubToExistingAccount(
                request.state().trim(),
                request.username().trim(),
                request.password()));
    }

    @GetMapping("/me")
    public CurrentUserResponse currentUser() {
        var user = currentUserProvider.requireCurrentUser();
        return new CurrentUserResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.isEmailVerified(),
            user.getDisplayName(),
            user.getRole(),
            user.getKernelDbName());
    }

    @PostMapping("/password/change/request")
    public ActionMessageResponse requestPasswordChange(@RequestBody PasswordChangeRequest request) {
        if (request == null || request.currentPassword() == null || request.currentPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password cannot be empty");
        }
        if (request.newPassword() == null || request.newPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password cannot be empty");
        }
        authService.requestPasswordChange(currentUserProvider.requireAuthenticatedUser().user().getId(), request.currentPassword(), request.newPassword());
        return new ActionMessageResponse("Password change verification email sent");
    }

    @PostMapping("/password/change/confirm")
    public ActionMessageResponse confirmPasswordChange(@RequestBody PasswordResetConfirmRequest request) {
        if (request == null || request.token() == null || request.token().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification token cannot be empty");
        }
        if (request.newPassword() == null || request.newPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password cannot be empty");
        }
        authService.confirmPasswordChange(request.token(), request.newPassword());
        return new ActionMessageResponse("Password changed successfully");
    }

    @PostMapping("/password/reset/request")
    public ActionMessageResponse requestPasswordReset(@RequestBody PasswordResetRequest request) {
        if (request == null || request.email() == null || request.email().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email cannot be empty");
        }
        authService.requestPasswordReset(request.email());
        return new ActionMessageResponse("Password reset email sent if the account exists");
    }

    @PostMapping("/password/reset/confirm")
    public ActionMessageResponse confirmPasswordReset(@RequestBody PasswordResetConfirmRequest request) {
        if (request == null || request.token() == null || request.token().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification token cannot be empty");
        }
        if (request.newPassword() == null || request.newPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password cannot be empty");
        }
        authService.confirmPasswordReset(request.token(), request.newPassword());
        return new ActionMessageResponse("Password reset successfully");
    }

    @DeleteMapping("/logout")
    public void logout() {
        authService.logout();
    }

    private LoginResponse toLoginResponse(AuthenticatedUser authenticatedUser) {
        return new LoginResponse(
            authenticatedUser.user().getId(),
            authenticatedUser.user().getUsername(),
            authenticatedUser.user().getEmail(),
            authenticatedUser.user().isEmailVerified(),
            authenticatedUser.user().getDisplayName(),
            authenticatedUser.user().getRole(),
            authenticatedUser.user().getKernelDbName(),
            authenticatedUser.token(),
            "Bearer");
    }
}

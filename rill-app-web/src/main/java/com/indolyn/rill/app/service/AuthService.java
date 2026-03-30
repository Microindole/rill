package com.indolyn.rill.app.service;

public interface AuthService {

    void register(String username, String email, String displayName, String password);

    AuthenticatedUser confirmEmail(String token);

    AuthenticatedUser login(String username, String password, String captchaToken);

    void requestPasswordChange(Long userId, String currentPassword, String newPassword);

    void confirmPasswordChange(String token, String newPassword);

    void requestPasswordReset(String email);

    void confirmPasswordReset(String token, String newPassword);

    void logout();
}

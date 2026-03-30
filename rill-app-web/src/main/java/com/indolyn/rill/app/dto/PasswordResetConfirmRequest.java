package com.indolyn.rill.app.dto;

public record PasswordResetConfirmRequest(String token, String newPassword) {
}

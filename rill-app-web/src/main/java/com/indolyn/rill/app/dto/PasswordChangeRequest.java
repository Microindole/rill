package com.indolyn.rill.app.dto;

public record PasswordChangeRequest(String currentPassword, String newPassword) {
}

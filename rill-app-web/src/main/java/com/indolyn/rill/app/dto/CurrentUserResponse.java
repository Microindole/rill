package com.indolyn.rill.app.dto;

public record CurrentUserResponse(
    Long userId, String username, String displayName, String role, String kernelDbName) {
}

package com.indolyn.rill.app.dto;

public record AdminUserResponse(
    Long userId,
    String username,
    String email,
    boolean emailVerified,
    String displayName,
    String role,
    String kernelDbName,
    boolean kernelDbProvisioned) {
}

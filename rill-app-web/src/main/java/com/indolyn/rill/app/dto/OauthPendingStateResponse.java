package com.indolyn.rill.app.dto;

public record OauthPendingStateResponse(
    String state,
    String provider,
    String providerLogin,
    String providerEmail,
    String providerDisplayName,
    String suggestedUsername) {
}

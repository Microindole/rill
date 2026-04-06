package com.indolyn.rill.app.service;

import java.time.Instant;

public record OauthPendingState(
    String state,
    String provider,
    String providerUserId,
    String providerLogin,
    String providerEmail,
    String providerDisplayName,
    Instant expiresAt) {
}

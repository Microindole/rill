package com.indolyn.rill.app.service;

public interface OauthPendingStateService {

    OauthPendingState create(
        String provider,
        String providerUserId,
        String providerLogin,
        String providerEmail,
        String providerDisplayName);

    OauthPendingState require(String state);

    void consume(String state);
}

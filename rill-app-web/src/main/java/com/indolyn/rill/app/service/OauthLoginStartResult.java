package com.indolyn.rill.app.service;

public record OauthLoginStartResult(AuthenticatedUser authenticatedUser, OauthPendingState pendingState) {

    public boolean isAuthenticated() {
        return authenticatedUser != null;
    }

    public boolean isPending() {
        return pendingState != null;
    }
}

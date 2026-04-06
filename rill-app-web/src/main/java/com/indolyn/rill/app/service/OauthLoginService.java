package com.indolyn.rill.app.service;

import org.springframework.security.oauth2.core.user.OAuth2User;

public interface OauthLoginService {

    OauthLoginStartResult beginGithubLogin(OAuth2User oauth2User);

    OauthPendingState requirePendingState(String state);

    AuthenticatedUser createAccountFromGithub(String state, String username, String displayName);

    AuthenticatedUser bindGithubToExistingAccount(String state, String username, String password);
}

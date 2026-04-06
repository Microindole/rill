package com.indolyn.rill.app.security;

import com.indolyn.rill.app.service.AuthenticatedUser;
import com.indolyn.rill.app.service.OauthLoginService;
import com.indolyn.rill.app.service.OauthLoginStartResult;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class Oauth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final OauthLoginService oauthLoginService;
    private final String frontendBaseUrl;

    public Oauth2LoginSuccessHandler(
        OauthLoginService oauthLoginService,
        @Value("${app.auth.frontend-base-url:http://localhost:5173}") String frontendBaseUrl) {
        this.oauthLoginService = oauthLoginService;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication) throws IOException, ServletException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        OauthLoginStartResult loginResult = oauthLoginService.beginGithubLogin(token.getPrincipal());
        if (loginResult.isAuthenticated()) {
            AuthenticatedUser authenticatedUser = loginResult.authenticatedUser();
            response.sendRedirect(
                frontendBaseUrl
                    + "/login/oauth2/success?token="
                    + URLEncoder.encode(authenticatedUser.token(), StandardCharsets.UTF_8));
            return;
        }
        response.sendRedirect(
            frontendBaseUrl
                + "/login?mode=oauth2-link&state="
                + URLEncoder.encode(loginResult.pendingState().state(), StandardCharsets.UTF_8));
    }
}

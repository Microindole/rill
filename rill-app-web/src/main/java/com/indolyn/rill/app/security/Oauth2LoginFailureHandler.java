package com.indolyn.rill.app.security;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class Oauth2LoginFailureHandler implements AuthenticationFailureHandler {

    private final String frontendBaseUrl;

    public Oauth2LoginFailureHandler(@Value("${app.auth.frontend-base-url:http://localhost:5173}") String frontendBaseUrl) {
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Override
    public void onAuthenticationFailure(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException exception) throws IOException, ServletException {
        response.sendRedirect(
            frontendBaseUrl
                + "/login/oauth2/error?message="
                + URLEncoder.encode(exception.getMessage(), StandardCharsets.UTF_8));
    }
}

package com.indolyn.rill.app.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.indolyn.rill.app.service.CaptchaVerificationService;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CaptchaVerificationServiceImpl implements CaptchaVerificationService {

    private final boolean captchaEnabled;
    private final String provider;
    private final String secretKey;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public CaptchaVerificationServiceImpl(
        @Value("${app.auth.captcha.enabled:false}") boolean captchaEnabled,
        @Value("${app.auth.captcha.provider:turnstile}") String provider,
        @Value("${app.auth.captcha.turnstile.secret-key:}") String secretKey,
        ObjectMapper objectMapper) {
        this.captchaEnabled = captchaEnabled;
        this.provider = provider;
        this.secretKey = secretKey;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public void verifyLoginCaptcha(String token) {
        if (!captchaEnabled) {
            return;
        }
        if (!"turnstile".equalsIgnoreCase(provider)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unsupported captcha provider");
        }
        if (secretKey == null || secretKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Captcha secret key is not configured");
        }
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Captcha token cannot be empty");
        }
        try {
            String requestBody =
                "secret="
                    + java.net.URLEncoder.encode(secretKey, StandardCharsets.UTF_8)
                    + "&response="
                    + java.net.URLEncoder.encode(token, StandardCharsets.UTF_8);
            HttpRequest request =
                HttpRequest.newBuilder()
                    .uri(URI.create("https://challenges.cloudflare.com/turnstile/v0/siteverify"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode payload = objectMapper.readTree(response.body());
            if (!payload.path("success").asBoolean(false)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Captcha verification failed");
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Captcha verification failed");
        }
    }
}

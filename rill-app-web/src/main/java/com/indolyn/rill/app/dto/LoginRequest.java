package com.indolyn.rill.app.dto;

public record LoginRequest(String username, String password, String captchaToken) {
}

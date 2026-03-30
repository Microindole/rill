package com.indolyn.rill.app.dto;

public record AuthConfigResponse(boolean captchaEnabled, String captchaProvider, String captchaSiteKey) {
}

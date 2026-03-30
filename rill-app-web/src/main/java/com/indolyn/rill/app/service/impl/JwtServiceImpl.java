package com.indolyn.rill.app.service.impl;

import com.indolyn.rill.app.persistence.entity.AppUserEntity;
import com.indolyn.rill.app.service.JwtPrincipal;
import com.indolyn.rill.app.service.JwtService;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class JwtServiceImpl implements JwtService {

    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();
    private static final Pattern STRING_CLAIM_PATTERN = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern NUMBER_CLAIM_PATTERN = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(\\d+)");

    private final byte[] secret;

    public JwtServiceImpl(@Value("${app.auth.jwt-secret:rill-dev-secret-change-me}") String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String issueToken(AppUserEntity user, String jwtId, Instant expiresAt) {
        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payloadJson =
            "{"
                + "\"sub\":\""
                + escape(user.getId().toString())
                + "\","
                + "\"username\":\""
                + escape(user.getUsername())
                + "\","
                + "\"role\":\""
                + escape(user.getRole())
                + "\","
                + "\"kernelDbName\":\""
                + escape(user.getKernelDbName())
                + "\","
                + "\"jti\":\""
                + escape(jwtId)
                + "\","
                + "\"exp\":"
                + expiresAt.getEpochSecond()
                + "}";
        String header = URL_ENCODER.encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
        String payload = URL_ENCODER.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        String signature = sign(header + "." + payload);
        return header + "." + payload + "." + signature;
    }

    @Override
    public JwtPrincipal verify(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }
        String expectedSignature = sign(parts[0] + "." + parts[1]);
        if (!expectedSignature.equals(parts[2])) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }
        String payloadJson = new String(URL_DECODER.decode(parts[1]), StandardCharsets.UTF_8);
        Map<String, String> stringClaims = extractStringClaims(payloadJson);
        Map<String, Long> numberClaims = extractNumberClaims(payloadJson);
        Instant expiresAt = Instant.ofEpochSecond(numberClaims.getOrDefault("exp", 0L));
        if (expiresAt.isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token expired");
        }
        try {
            return new JwtPrincipal(
                Long.parseLong(stringClaims.getOrDefault("sub", "0")),
                stringClaims.getOrDefault("username", ""),
                stringClaims.getOrDefault("role", "GUEST"),
                stringClaims.getOrDefault("kernelDbName", "default"),
                stringClaims.getOrDefault("jti", ""),
                expiresAt);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }
    }

    private Map<String, String> extractStringClaims(String payloadJson) {
        Map<String, String> claims = new LinkedHashMap<>();
        Matcher matcher = STRING_CLAIM_PATTERN.matcher(payloadJson);
        while (matcher.find()) {
            claims.put(matcher.group(1), matcher.group(2));
        }
        return claims;
    }

    private Map<String, Long> extractNumberClaims(String payloadJson) {
        Map<String, Long> claims = new LinkedHashMap<>();
        Matcher matcher = NUMBER_CLAIM_PATTERN.matcher(payloadJson);
        while (matcher.find()) {
            claims.put(matcher.group(1), Long.parseLong(matcher.group(2)));
        }
        return claims;
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return URL_ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign JWT", e);
        }
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

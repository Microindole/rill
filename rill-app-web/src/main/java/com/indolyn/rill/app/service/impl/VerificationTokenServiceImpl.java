package com.indolyn.rill.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.indolyn.rill.app.persistence.entity.AppUserEntity;
import com.indolyn.rill.app.persistence.entity.AppVerificationTokenEntity;
import com.indolyn.rill.app.persistence.mapper.AppVerificationTokenMapper;
import com.indolyn.rill.app.service.VerificationTokenService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class VerificationTokenServiceImpl implements VerificationTokenService {

    private final AppVerificationTokenMapper appVerificationTokenMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final String tokenStore;
    private final long tokenTtlMinutes;
    private final String redisKeyPrefix;

    public VerificationTokenServiceImpl(
        AppVerificationTokenMapper appVerificationTokenMapper,
        ObjectMapper objectMapper,
        @Value("${app.auth.verification-token.store:database}") String tokenStore,
        @Value("${app.auth.verification-token.ttl-minutes:30}") long tokenTtlMinutes,
        @Value("${app.auth.verification-token.redis-key-prefix:rill:auth:verification:}") String redisKeyPrefix,
        @org.springframework.lang.Nullable StringRedisTemplate redisTemplate) {
        this.appVerificationTokenMapper = appVerificationTokenMapper;
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
        this.tokenStore = tokenStore == null ? "database" : tokenStore.trim().toLowerCase();
        this.tokenTtlMinutes = tokenTtlMinutes;
        this.redisKeyPrefix = redisKeyPrefix == null ? "rill:auth:verification:" : redisKeyPrefix;
        this.redisTemplate = redisTemplate;
    }

    @Override
    @Transactional
    public AppVerificationTokenEntity create(AppUserEntity user, String purpose) {
        Instant now = Instant.now();
        AppVerificationTokenEntity token = new AppVerificationTokenEntity();
        token.setUserId(user.getId());
        token.setPurpose(purpose);
        token.setToken(UUID.randomUUID().toString().replace("-", ""));
        token.setExpiresAt(now.plus(tokenTtlMinutes, ChronoUnit.MINUTES));
        token.setUsed(false);
        token.setCreatedAt(now);
        if (useRedisStore()) {
            saveTokenToRedis(token);
            return token;
        }
        appVerificationTokenMapper.insert(token);
        return token;
    }

    @Override
    public AppVerificationTokenEntity requireUsableToken(String token, String purpose) {
        if (useRedisStore()) {
            AppVerificationTokenEntity existing = loadTokenFromRedis(token);
            validateToken(existing, purpose);
            return existing;
        }
        AppVerificationTokenEntity existing =
            appVerificationTokenMapper.selectOne(
                new QueryWrapper<AppVerificationTokenEntity>()
                    .eq("token", token)
                    .eq("purpose", purpose)
                    .eq("used", false)
                    .last("limit 1"));
        validateToken(existing, purpose);
        return existing;
    }

    @Override
    @Transactional
    public void markUsed(AppVerificationTokenEntity token) {
        if (useRedisStore()) {
            token.setUsed(true);
            token.setUsedAt(Instant.now());
            saveTokenToRedis(token);
            return;
        }
        token.setUsed(true);
        token.setUsedAt(Instant.now());
        appVerificationTokenMapper.updateById(token);
    }

    private boolean useRedisStore() {
        if (!"redis".equals(tokenStore)) {
            return false;
        }
        if (redisTemplate == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Redis verification token store is not available");
        }
        return true;
    }

    private void validateToken(AppVerificationTokenEntity token, String purpose) {
        if (token == null
            || token.isUsed()
            || token.getExpiresAt() == null
            || token.getExpiresAt().isBefore(Instant.now())
            || !purpose.equals(token.getPurpose())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification token is invalid or expired");
        }
    }

    private void saveTokenToRedis(AppVerificationTokenEntity token) {
        long ttlSeconds = Math.max(1, Instant.now().until(token.getExpiresAt(), ChronoUnit.SECONDS));
        try {
            redisTemplate.opsForValue().set(redisKey(token.getToken()), objectMapper.writeValueAsString(token), ttlSeconds, java.util.concurrent.TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store verification token");
        }
    }

    private AppVerificationTokenEntity loadTokenFromRedis(String token) {
        String payload = redisTemplate.opsForValue().get(redisKey(token));
        if (payload == null || payload.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(payload, AppVerificationTokenEntity.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read verification token");
        }
    }

    private String redisKey(String token) {
        return redisKeyPrefix + token;
    }
}

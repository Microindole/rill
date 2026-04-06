package com.indolyn.rill.app.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.indolyn.rill.app.service.OauthPendingState;
import com.indolyn.rill.app.service.OauthPendingStateService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OauthPendingStateServiceImpl implements OauthPendingStateService {

    private static final Map<String, OauthPendingState> MEMORY_STORE = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    private final String store;
    private final long ttlMinutes;
    private final String redisKeyPrefix;

    public OauthPendingStateServiceImpl(
        ObjectMapper objectMapper,
        @Value("${app.auth.oauth2.pending.store:redis}") String store,
        @Value("${app.auth.oauth2.pending.ttl-minutes:10}") long ttlMinutes,
        @Value("${app.auth.oauth2.pending.redis-key-prefix:rill:auth:oauth2:pending:}") String redisKeyPrefix,
        @Nullable StringRedisTemplate redisTemplate) {
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
        this.store = store == null ? "redis" : store.trim().toLowerCase();
        this.ttlMinutes = ttlMinutes;
        this.redisKeyPrefix = redisKeyPrefix == null ? "rill:auth:oauth2:pending:" : redisKeyPrefix;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public OauthPendingState create(
        String provider,
        String providerUserId,
        String providerLogin,
        String providerEmail,
        String providerDisplayName) {
        Instant expiresAt = Instant.now().plus(ttlMinutes, ChronoUnit.MINUTES);
        OauthPendingState pendingState =
            new OauthPendingState(
                UUID.randomUUID().toString().replace("-", ""),
                provider,
                providerUserId,
                providerLogin,
                providerEmail,
                providerDisplayName,
                expiresAt);
        if (useRedis()) {
            saveToRedis(pendingState);
        } else {
            MEMORY_STORE.put(pendingState.state(), pendingState);
        }
        return pendingState;
    }

    @Override
    public OauthPendingState require(String state) {
        OauthPendingState pendingState = useRedis() ? loadFromRedis(state) : MEMORY_STORE.get(state);
        if (pendingState == null || pendingState.expiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OAuth2 login state is invalid or expired");
        }
        return pendingState;
    }

    @Override
    public void consume(String state) {
        if (useRedis()) {
            redisTemplate.delete(redisKey(state));
        } else {
            MEMORY_STORE.remove(state);
        }
    }

    private boolean useRedis() {
        return "redis".equals(store) && redisTemplate != null;
    }

    private void saveToRedis(OauthPendingState pendingState) {
        long ttlSeconds = Math.max(1, Instant.now().until(pendingState.expiresAt(), ChronoUnit.SECONDS));
        try {
            redisTemplate.opsForValue()
                .set(redisKey(pendingState.state()), objectMapper.writeValueAsString(pendingState), ttlSeconds, TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store OAuth2 login state");
        }
    }

    private OauthPendingState loadFromRedis(String state) {
        String payload = redisTemplate.opsForValue().get(redisKey(state));
        if (payload == null || payload.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(payload, OauthPendingState.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read OAuth2 login state");
        }
    }

    private String redisKey(String state) {
        return redisKeyPrefix + state;
    }
}

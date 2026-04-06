package com.indolyn.rill.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.indolyn.rill.app.persistence.entity.AppUserEntity;
import com.indolyn.rill.app.persistence.entity.AppVerificationTokenEntity;
import com.indolyn.rill.app.persistence.mapper.AppVerificationTokenMapper;
import com.indolyn.rill.app.service.impl.VerificationTokenServiceImpl;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class VerificationTokenServiceImplTest {

    private final JsonMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    @Test
    void createShouldStoreTokenInRedisWhenConfigured() {
        AppVerificationTokenMapper tokenMapper = Mockito.mock(AppVerificationTokenMapper.class);
        StringRedisTemplate redisTemplate = Mockito.mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = Mockito.mock(ValueOperations.class);
        doReturn(valueOperations).when(redisTemplate).opsForValue();
        VerificationTokenService service =
            new VerificationTokenServiceImpl(tokenMapper, objectMapper, "redis", 30, "test:rill:auth:verification:", redisTemplate);

        AppVerificationTokenEntity created = service.create(user(7L), VerificationTokenService.PURPOSE_REGISTER);

        assertNotNull(created.getToken());
        assertEquals(7L, created.getUserId());
        verify(valueOperations).set(anyString(), anyString(), anyLong(), Mockito.eq(TimeUnit.SECONDS));
        Mockito.verifyNoInteractions(tokenMapper);
    }

    @Test
    void requireUsableTokenShouldReadFromRedisWhenConfigured() throws Exception {
        AppVerificationTokenMapper tokenMapper = Mockito.mock(AppVerificationTokenMapper.class);
        StringRedisTemplate redisTemplate = Mockito.mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = Mockito.mock(ValueOperations.class);
        doReturn(valueOperations).when(redisTemplate).opsForValue();
        AppVerificationTokenEntity token = new AppVerificationTokenEntity();
        token.setUserId(11L);
        token.setPurpose(VerificationTokenService.PURPOSE_PASSWORD_RESET);
        token.setToken("token-1");
        token.setExpiresAt(Instant.now().plusSeconds(1800));
        token.setUsed(false);
        when(valueOperations.get("test:rill:auth:verification:token-1")).thenReturn(objectMapper.writeValueAsString(token));
        VerificationTokenService service =
            new VerificationTokenServiceImpl(tokenMapper, objectMapper, "redis", 30, "test:rill:auth:verification:", redisTemplate);

        AppVerificationTokenEntity resolved =
            service.requireUsableToken("token-1", VerificationTokenService.PURPOSE_PASSWORD_RESET);

        assertEquals(11L, resolved.getUserId());
        assertEquals("token-1", resolved.getToken());
        Mockito.verifyNoInteractions(tokenMapper);
    }

    @Test
    void markUsedShouldPersistUpdatedTokenToRedis() throws Exception {
        AppVerificationTokenMapper tokenMapper = Mockito.mock(AppVerificationTokenMapper.class);
        StringRedisTemplate redisTemplate = Mockito.mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = Mockito.mock(ValueOperations.class);
        doReturn(valueOperations).when(redisTemplate).opsForValue();
        VerificationTokenService service =
            new VerificationTokenServiceImpl(tokenMapper, objectMapper, "redis", 30, "test:rill:auth:verification:", redisTemplate);
        AppVerificationTokenEntity token = new AppVerificationTokenEntity();
        token.setUserId(15L);
        token.setPurpose(VerificationTokenService.PURPOSE_PASSWORD_CHANGE);
        token.setToken("token-2");
        token.setExpiresAt(Instant.now().plusSeconds(1800));
        token.setUsed(false);

        service.markUsed(token);

        assertTrue(token.isUsed());
        verify(valueOperations).set(anyString(), anyString(), anyLong(), Mockito.eq(TimeUnit.SECONDS));
        Mockito.verifyNoInteractions(tokenMapper);
    }

    private AppUserEntity user(Long id) {
        AppUserEntity user = new AppUserEntity();
        user.setId(id);
        return user;
    }
}

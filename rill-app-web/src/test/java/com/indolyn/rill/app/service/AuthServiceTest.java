package com.indolyn.rill.app.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.indolyn.rill.app.persistence.entity.AppJwtSessionEntity;
import com.indolyn.rill.app.persistence.entity.AppUserEntity;
import com.indolyn.rill.app.persistence.mapper.AppJwtSessionMapper;
import com.indolyn.rill.app.persistence.mapper.AppUserMapper;
import com.indolyn.rill.app.security.RequestUserContext;
import com.indolyn.rill.app.security.RequestUserContextHolder;
import com.indolyn.rill.app.service.impl.AuthServiceImpl;
import com.indolyn.rill.app.service.impl.JwtServiceImpl;

import java.time.Instant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AuthServiceTest {

    @AfterEach
    void clearRequestContext() {
        RequestUserContextHolder.clear();
    }

    @Test
    void loginShouldIssueBearerToken() {
        AppUserMapper appUserMapper = Mockito.mock(AppUserMapper.class);
        AppJwtSessionMapper appJwtSessionMapper = Mockito.mock(AppJwtSessionMapper.class);
        RillQueryService rillQueryService = Mockito.mock(RillQueryService.class);
        AuthService service =
            new AuthServiceImpl(appUserMapper, appJwtSessionMapper, new JwtServiceImpl("test-secret"), rillQueryService);
        AppUserEntity user = user(1L, "demo", "Demo Admin", "demo123", "ADMIN", "demo");
        when(appUserMapper.selectOne(any())).thenReturn(user);

        AuthenticatedUser authenticatedUser = service.login("demo", "demo123");

        assertEquals(1L, authenticatedUser.user().getId());
        assertFalse(authenticatedUser.token().isBlank());
        verify(appJwtSessionMapper).insert(any(AppJwtSessionEntity.class));
    }

    @Test
    void registerShouldPersistUserAndIssueToken() {
        AppUserMapper appUserMapper = Mockito.mock(AppUserMapper.class);
        AppJwtSessionMapper appJwtSessionMapper = Mockito.mock(AppJwtSessionMapper.class);
        RillQueryService rillQueryService = Mockito.mock(RillQueryService.class);
        AuthService service =
            new AuthServiceImpl(appUserMapper, appJwtSessionMapper, new JwtServiceImpl("test-secret"), rillQueryService);
        when(appUserMapper.selectOne(any())).thenReturn(null);
        Mockito.doAnswer(
                invocation -> {
                    AppUserEntity inserted = invocation.getArgument(0);
                    inserted.setId(2L);
                    return 1;
                })
            .when(appUserMapper)
            .insert(any(AppUserEntity.class));
        when(rillQueryService.execute("default", "create database newuser;"))
            .thenReturn(Mockito.mock(DatabaseExecution.class));

        AuthenticatedUser authenticatedUser = service.register("newuser", "New User", "secret");

        assertEquals("newuser", authenticatedUser.user().getUsername());
        assertEquals("USER", authenticatedUser.user().getRole());
        assertEquals("newuser", authenticatedUser.user().getKernelDbName());
        verify(appUserMapper).insert(any(AppUserEntity.class));
        verify(appJwtSessionMapper).insert(any(AppJwtSessionEntity.class));
    }

    @Test
    void logoutShouldRevokeCurrentToken() {
        AppUserMapper appUserMapper = Mockito.mock(AppUserMapper.class);
        AppJwtSessionMapper appJwtSessionMapper = Mockito.mock(AppJwtSessionMapper.class);
        RillQueryService rillQueryService = Mockito.mock(RillQueryService.class);
        AuthService service =
            new AuthServiceImpl(appUserMapper, appJwtSessionMapper, new JwtServiceImpl("test-secret"), rillQueryService);
        AppJwtSessionEntity jwtSession = session(9L, 1L, "jti-1", false);
        RequestUserContextHolder.set(new RequestUserContext(user(1L, "demo", "Demo Admin", "demo123", "ADMIN", "demo"), "token-demo", "jti-1"));
        when(appJwtSessionMapper.selectOne(any())).thenReturn(jwtSession);

        service.logout();

        assertEquals(true, jwtSession.isRevoked());
        verify(appJwtSessionMapper).updateById(jwtSession);
    }

    @Test
    void logoutShouldIgnoreAnonymousRequest() {
        AppUserMapper appUserMapper = Mockito.mock(AppUserMapper.class);
        AppJwtSessionMapper appJwtSessionMapper = Mockito.mock(AppJwtSessionMapper.class);
        RillQueryService rillQueryService = Mockito.mock(RillQueryService.class);
        AuthService service =
            new AuthServiceImpl(appUserMapper, appJwtSessionMapper, new JwtServiceImpl("test-secret"), rillQueryService);

        assertDoesNotThrow(service::logout);
    }

    private AppUserEntity user(
        Long id, String username, String displayName, String password, String role, String kernelDbName) {
        AppUserEntity user = new AppUserEntity();
        user.setId(id);
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setPassword(password);
        user.setRole(role);
        user.setKernelDbName(kernelDbName);
        return user;
    }

    private AppJwtSessionEntity session(Long id, Long userId, String jwtId, boolean revoked) {
        AppJwtSessionEntity jwtSession = new AppJwtSessionEntity();
        jwtSession.setId(id);
        jwtSession.setUserId(userId);
        jwtSession.setJwtId(jwtId);
        jwtSession.setRevoked(revoked);
        jwtSession.setExpiresAt(Instant.now().plusSeconds(3600));
        jwtSession.setCreatedAt(Instant.now());
        jwtSession.setUpdatedAt(Instant.now());
        return jwtSession;
    }
}

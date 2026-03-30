package com.indolyn.rill.app.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.indolyn.rill.app.persistence.entity.AppJwtSessionEntity;
import com.indolyn.rill.app.persistence.entity.AppUserEntity;
import com.indolyn.rill.app.persistence.entity.AppVerificationTokenEntity;
import com.indolyn.rill.app.persistence.mapper.AppJwtSessionMapper;
import com.indolyn.rill.app.persistence.mapper.AppUserMapper;
import com.indolyn.rill.app.persistence.mapper.AppVerificationTokenMapper;
import com.indolyn.rill.app.security.RequestUserContext;
import com.indolyn.rill.app.security.RequestUserContextHolder;
import com.indolyn.rill.app.service.impl.AuthServiceImpl;
import com.indolyn.rill.app.service.impl.CaptchaVerificationServiceImpl;
import com.indolyn.rill.app.service.impl.JwtServiceImpl;
import com.indolyn.rill.app.service.impl.MailServiceImpl;
import com.indolyn.rill.app.service.impl.VerificationTokenServiceImpl;

import java.time.Instant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AuthServiceTest {

    @AfterEach
    void clearRequestContext() {
        RequestUserContextHolder.clear();
    }

    @Test
    void loginShouldIssueBearerToken() {
        AppUserMapper appUserMapper = Mockito.mock(AppUserMapper.class);
        AppJwtSessionMapper appJwtSessionMapper = Mockito.mock(AppJwtSessionMapper.class);
        AppVerificationTokenMapper tokenMapper = Mockito.mock(AppVerificationTokenMapper.class);
        RillQueryService rillQueryService = Mockito.mock(RillQueryService.class);
        AuthService service = newService(appUserMapper, appJwtSessionMapper, tokenMapper, rillQueryService);
        AppUserEntity user = user(1L, "demo", "demo@example.com", "Demo Admin", new BCryptPasswordEncoder().encode("demo123"), "ADMIN", "demo");
        user.setEmailVerified(true);
        user.setKernelDbProvisioned(true);
        when(appUserMapper.selectOne(any())).thenReturn(user);

        AuthenticatedUser authenticatedUser = service.login("demo", "demo123", "captcha-token");

        assertEquals(1L, authenticatedUser.user().getId());
        assertFalse(authenticatedUser.token().isBlank());
        verify(appJwtSessionMapper).insert(any(AppJwtSessionEntity.class));
    }

    @Test
    void registerShouldPersistUserAndCreateVerificationToken() {
        AppUserMapper appUserMapper = Mockito.mock(AppUserMapper.class);
        AppJwtSessionMapper appJwtSessionMapper = Mockito.mock(AppJwtSessionMapper.class);
        AppVerificationTokenMapper tokenMapper = Mockito.mock(AppVerificationTokenMapper.class);
        RillQueryService rillQueryService = Mockito.mock(RillQueryService.class);
        AuthService service = newService(appUserMapper, appJwtSessionMapper, tokenMapper, rillQueryService);
        when(appUserMapper.selectOne(any())).thenReturn(null);
        Mockito.doAnswer(
                invocation -> {
                    AppUserEntity inserted = invocation.getArgument(0);
                    inserted.setId(2L);
                    return 1;
                })
            .when(appUserMapper)
            .insert(any(AppUserEntity.class));

        service.register("newuser", "newuser@example.com", "New User", "secret");

        verify(appUserMapper).insert(any(AppUserEntity.class));
        verify(tokenMapper).insert(any(AppVerificationTokenEntity.class));
    }

    @Test
    void confirmEmailShouldProvisionDatabaseAndIssueToken() {
        AppUserMapper appUserMapper = Mockito.mock(AppUserMapper.class);
        AppJwtSessionMapper appJwtSessionMapper = Mockito.mock(AppJwtSessionMapper.class);
        AppVerificationTokenMapper tokenMapper = Mockito.mock(AppVerificationTokenMapper.class);
        RillQueryService rillQueryService = Mockito.mock(RillQueryService.class);
        AuthService service = newService(appUserMapper, appJwtSessionMapper, tokenMapper, rillQueryService);
        AppVerificationTokenEntity token = new AppVerificationTokenEntity();
        token.setId(5L);
        token.setUserId(1L);
        token.setPurpose(VerificationTokenService.PURPOSE_REGISTER);
        token.setToken("verify-token");
        token.setExpiresAt(Instant.now().plusSeconds(1800));
        token.setUsed(false);
        AppUserEntity user = user(1L, "demo", "demo@example.com", "Demo User", "encoded", "USER", "demo");
        user.setEmailVerified(false);
        user.setKernelDbProvisioned(false);
        when(tokenMapper.selectOne(any())).thenReturn(token);
        when(appUserMapper.selectById(1L)).thenReturn(user);
        when(rillQueryService.execute("default", "create database demo;")).thenReturn(Mockito.mock(DatabaseExecution.class));

        AuthenticatedUser authenticatedUser = service.confirmEmail("verify-token");

        assertTrue(authenticatedUser.user().isEmailVerified());
        assertTrue(authenticatedUser.user().isKernelDbProvisioned());
        verify(appUserMapper).updateById(user);
        verify(tokenMapper).updateById(token);
        verify(appJwtSessionMapper).insert(any(AppJwtSessionEntity.class));
    }

    @Test
    void logoutShouldRevokeCurrentToken() {
        AppUserMapper appUserMapper = Mockito.mock(AppUserMapper.class);
        AppJwtSessionMapper appJwtSessionMapper = Mockito.mock(AppJwtSessionMapper.class);
        AppVerificationTokenMapper tokenMapper = Mockito.mock(AppVerificationTokenMapper.class);
        RillQueryService rillQueryService = Mockito.mock(RillQueryService.class);
        AuthService service = newService(appUserMapper, appJwtSessionMapper, tokenMapper, rillQueryService);
        AppJwtSessionEntity jwtSession = session(9L, 1L, "jti-1", false);
        AppUserEntity user = user(1L, "demo", "demo@example.com", "Demo Admin", "encoded", "ADMIN", "demo");
        RequestUserContextHolder.set(new RequestUserContext(user, "token-demo", "jti-1"));
        when(appJwtSessionMapper.selectOne(any())).thenReturn(jwtSession);

        service.logout();

        assertTrue(jwtSession.isRevoked());
        verify(appJwtSessionMapper).updateById(jwtSession);
    }

    @Test
    void logoutShouldIgnoreAnonymousRequest() {
        AppUserMapper appUserMapper = Mockito.mock(AppUserMapper.class);
        AppJwtSessionMapper appJwtSessionMapper = Mockito.mock(AppJwtSessionMapper.class);
        AppVerificationTokenMapper tokenMapper = Mockito.mock(AppVerificationTokenMapper.class);
        RillQueryService rillQueryService = Mockito.mock(RillQueryService.class);
        AuthService service = newService(appUserMapper, appJwtSessionMapper, tokenMapper, rillQueryService);

        assertDoesNotThrow(service::logout);
    }

    private AuthService newService(
        AppUserMapper appUserMapper,
        AppJwtSessionMapper appJwtSessionMapper,
        AppVerificationTokenMapper tokenMapper,
        RillQueryService rillQueryService) {
        return new AuthServiceImpl(
            appUserMapper,
            appJwtSessionMapper,
            new JwtServiceImpl("test-secret"),
            rillQueryService,
            new BCryptPasswordEncoder(),
            new CaptchaVerificationServiceImpl(false, "turnstile", "", new ObjectMapper()),
            new VerificationTokenServiceImpl(tokenMapper),
            new MailServiceImpl(false, "noreply@example.com", Mockito.mock(JavaMailSender.class)),
            7,
            "http://localhost:5173");
    }

    private AppUserEntity user(
        Long id, String username, String email, String displayName, String password, String role, String kernelDbName) {
        AppUserEntity user = new AppUserEntity();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
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

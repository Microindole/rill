package com.indolyn.rill.app.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.indolyn.rill.app.persistence.entity.AppJwtSessionEntity;
import com.indolyn.rill.app.persistence.entity.AppUserEntity;
import com.indolyn.rill.app.persistence.mapper.AppJwtSessionMapper;
import com.indolyn.rill.app.persistence.mapper.AppUserMapper;
import com.indolyn.rill.app.service.JwtService;
import com.indolyn.rill.app.service.impl.JwtServiceImpl;

import java.io.IOException;
import java.time.Instant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;

class JwtAuthenticationFilterTest {

    @AfterEach
    void clearRequestContext() {
        RequestUserContextHolder.clear();
    }

    @Test
    void filterShouldPopulateRequestUserContextForValidJwt() throws Exception {
        AppUserMapper appUserMapper = Mockito.mock(AppUserMapper.class);
        AppJwtSessionMapper appJwtSessionMapper = Mockito.mock(AppJwtSessionMapper.class);
        JwtService jwtService = new JwtServiceImpl("test-secret");
        JwtAuthenticationFilter filter =
            new JwtAuthenticationFilter(appUserMapper, appJwtSessionMapper, jwtService, new ObjectMapper());

        AppUserEntity user = user(1L, "demo", "Demo", "demo123", "ADMIN", "demo");
        String token = jwtService.issueToken(user, "jti-1", Instant.now().plusSeconds(3600));
        when(appJwtSessionMapper.selectOne(any())).thenReturn(session(7L, 1L, "jti-1", false));
        when(appUserMapper.selectById(1L)).thenReturn(user);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain =
            (servletRequest, servletResponse) -> {
                RequestUserContext context = RequestUserContextHolder.get();
                assertEquals("demo", context.user().getUsername());
                assertEquals("jti-1", context.jwtId());
            };

        filter.doFilter(request, response, filterChain);

        assertEquals(200, response.getStatus());
        assertNull(RequestUserContextHolder.get());
    }

    @Test
    void filterShouldRejectInvalidAuthorizationHeader() throws Exception {
        AppUserMapper appUserMapper = Mockito.mock(AppUserMapper.class);
        AppJwtSessionMapper appJwtSessionMapper = Mockito.mock(AppJwtSessionMapper.class);
        JwtAuthenticationFilter filter =
            new JwtAuthenticationFilter(appUserMapper, appJwtSessionMapper, new JwtServiceImpl("test-secret"), new ObjectMapper());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Token abc");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {});

        assertEquals(401, response.getStatus());
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

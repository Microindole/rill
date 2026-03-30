package com.indolyn.rill.app.security;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.indolyn.rill.app.persistence.entity.AppJwtSessionEntity;
import com.indolyn.rill.app.persistence.entity.AppUserEntity;
import com.indolyn.rill.app.persistence.mapper.AppJwtSessionMapper;
import com.indolyn.rill.app.persistence.mapper.AppUserMapper;
import com.indolyn.rill.app.service.JwtPrincipal;
import com.indolyn.rill.app.service.JwtService;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final AppUserMapper appUserMapper;
    private final AppJwtSessionMapper appJwtSessionMapper;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(
        AppUserMapper appUserMapper,
        AppJwtSessionMapper appJwtSessionMapper,
        JwtService jwtService,
        ObjectMapper objectMapper) {
        this.appUserMapper = appUserMapper;
        this.appJwtSessionMapper = appJwtSessionMapper;
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        RequestUserContextHolder.clear();
        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        if (authorization == null || authorization.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!authorization.startsWith(BEARER_PREFIX)) {
            writeUnauthorized(response, "Invalid authorization header");
            return;
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (token.isBlank()) {
            writeUnauthorized(response, "Invalid authorization header");
            return;
        }
        try {
            JwtPrincipal principal = jwtService.verify(token);
            AppJwtSessionEntity jwtSession = appJwtSessionMapper.selectOne(
                new QueryWrapper<AppJwtSessionEntity>()
                    .eq("jwt_id", principal.jwtId())
                    .eq("revoked", false)
                    .last("limit 1"));
            if (jwtSession == null || jwtSession.getExpiresAt() == null || jwtSession.getExpiresAt().isBefore(Instant.now())) {
                writeUnauthorized(response, "Login required");
                return;
            }
            AppUserEntity user = appUserMapper.selectById(jwtSession.getUserId());
            if (user == null) {
                writeUnauthorized(response, "Current user not found");
                return;
            }
            RequestUserContextHolder.set(new RequestUserContext(user, token, principal.jwtId()));
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            writeUnauthorized(response, "Login required");
        } finally {
            RequestUserContextHolder.clear();
        }
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Map.of("status", 401, "message", message));
    }
}

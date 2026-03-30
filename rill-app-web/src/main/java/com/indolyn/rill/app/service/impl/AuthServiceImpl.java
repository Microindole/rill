package com.indolyn.rill.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.indolyn.rill.app.persistence.entity.AppJwtSessionEntity;
import com.indolyn.rill.app.persistence.entity.AppUserEntity;
import com.indolyn.rill.app.persistence.mapper.AppJwtSessionMapper;
import com.indolyn.rill.app.persistence.mapper.AppUserMapper;
import com.indolyn.rill.app.security.RequestUserContext;
import com.indolyn.rill.app.security.RequestUserContextHolder;
import com.indolyn.rill.app.service.AuthService;
import com.indolyn.rill.app.service.AuthenticatedUser;
import com.indolyn.rill.app.service.JwtService;
import com.indolyn.rill.app.service.RillQueryService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthServiceImpl implements AuthService {

    private static final String GUEST_USERNAME = "guest";
    private static final long TOKEN_TTL_DAYS = 7;

    private final AppUserMapper appUserMapper;
    private final AppJwtSessionMapper appJwtSessionMapper;
    private final JwtService jwtService;
    private final RillQueryService rillQueryService;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(
        AppUserMapper appUserMapper,
        AppJwtSessionMapper appJwtSessionMapper,
        JwtService jwtService,
        RillQueryService rillQueryService,
        PasswordEncoder passwordEncoder) {
        this.appUserMapper = appUserMapper;
        this.appJwtSessionMapper = appJwtSessionMapper;
        this.jwtService = jwtService;
        this.rillQueryService = rillQueryService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public AuthenticatedUser register(String username, String displayName, String password) {
        String normalizedUsername = normalizeUsername(username);
        String normalizedDisplayName = normalizeDisplayName(displayName);
        String normalizedPassword = normalizePassword(password);
        AppUserEntity existing =
            appUserMapper.selectOne(
                new QueryWrapper<AppUserEntity>().eq("username", normalizedUsername).last("limit 1"));
        if (existing != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }
        Instant now = Instant.now();
        AppUserEntity user = new AppUserEntity();
        user.setUsername(normalizedUsername);
        user.setDisplayName(normalizedDisplayName);
        user.setPassword(passwordEncoder.encode(normalizedPassword));
        user.setRole("USER");
        user.setKernelDbName(allocateKernelDatabaseName(normalizedUsername));
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        appUserMapper.insert(user);
        ensureKernelDatabaseExists(user.getKernelDbName());
        return issueToken(user);
    }

    @Override
    @Transactional
    public AuthenticatedUser login(String username, String password) {
        String normalizedUsername = normalizeUsername(username);
        String normalizedPassword = normalizePassword(password);
        AppUserEntity user =
            appUserMapper.selectOne(
                new QueryWrapper<AppUserEntity>().eq("username", normalizedUsername).last("limit 1"));
        if (user == null || user.getPassword() == null || !passwordEncoder.matches(normalizedPassword, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        return issueToken(user);
    }

    @Override
    @Transactional
    public void logout() {
        RequestUserContext context = RequestUserContextHolder.get();
        if (context == null) {
            return;
        }
        AppJwtSessionEntity jwtSession =
            appJwtSessionMapper.selectOne(
                new QueryWrapper<AppJwtSessionEntity>()
                    .eq("jwt_id", context.jwtId())
                    .eq("revoked", false)
                    .last("limit 1"));
        if (jwtSession == null) {
            return;
        }
        jwtSession.setRevoked(true);
        jwtSession.setUpdatedAt(Instant.now());
        appJwtSessionMapper.updateById(jwtSession);
    }

    private AuthenticatedUser issueToken(AppUserEntity user) {
        Instant now = Instant.now();
        String jwtId = UUID.randomUUID().toString().replace("-", "");
        AppJwtSessionEntity jwtSession = new AppJwtSessionEntity();
        jwtSession.setUserId(user.getId());
        jwtSession.setJwtId(jwtId);
        jwtSession.setExpiresAt(now.plus(TOKEN_TTL_DAYS, ChronoUnit.DAYS));
        jwtSession.setRevoked(false);
        jwtSession.setCreatedAt(now);
        jwtSession.setUpdatedAt(now);
        appJwtSessionMapper.insert(jwtSession);
        return new AuthenticatedUser(user, jwtService.issueToken(user, jwtId, jwtSession.getExpiresAt()));
    }

    private String allocateKernelDatabaseName(String username) {
        String normalized = username.trim().toLowerCase().replaceAll("[^a-z0-9_]", "_");
        if (normalized.isBlank() || "default".equals(normalized) || GUEST_USERNAME.equals(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username cannot be used as database name");
        }
        return normalized;
    }

    private void ensureKernelDatabaseExists(String kernelDbName) {
        try {
            rillQueryService.execute("default", "create database " + kernelDbName + ";");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to provision kernel database");
        }
    }

    private String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username cannot be empty");
        }
        return username.trim();
    }

    private String normalizeDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Display name cannot be empty");
        }
        return displayName.trim();
    }

    private String normalizePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password cannot be empty");
        }
        return password;
    }
}

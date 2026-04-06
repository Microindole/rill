package com.indolyn.rill.app.service.impl;

import com.indolyn.rill.app.persistence.entity.AppJwtSessionEntity;
import com.indolyn.rill.app.persistence.entity.AppUserEntity;
import com.indolyn.rill.app.persistence.mapper.AppJwtSessionMapper;
import com.indolyn.rill.app.service.AuthenticatedUser;
import com.indolyn.rill.app.service.JwtService;
import com.indolyn.rill.app.service.LoginSessionService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginSessionServiceImpl implements LoginSessionService {

    private final AppJwtSessionMapper appJwtSessionMapper;
    private final JwtService jwtService;
    private final long tokenTtlDays;

    public LoginSessionServiceImpl(
        AppJwtSessionMapper appJwtSessionMapper,
        JwtService jwtService,
        @Value("${app.auth.jwt-ttl-days:7}") long tokenTtlDays) {
        this.appJwtSessionMapper = appJwtSessionMapper;
        this.jwtService = jwtService;
        this.tokenTtlDays = tokenTtlDays;
    }

    @Override
    @Transactional
    public AuthenticatedUser issueToken(AppUserEntity user) {
        Instant now = Instant.now();
        String jwtId = UUID.randomUUID().toString().replace("-", "");
        AppJwtSessionEntity jwtSession = new AppJwtSessionEntity();
        jwtSession.setUserId(user.getId());
        jwtSession.setJwtId(jwtId);
        jwtSession.setExpiresAt(now.plus(tokenTtlDays, ChronoUnit.DAYS));
        jwtSession.setRevoked(false);
        jwtSession.setCreatedAt(now);
        jwtSession.setUpdatedAt(now);
        appJwtSessionMapper.insert(jwtSession);
        return new AuthenticatedUser(user, jwtService.issueToken(user, jwtId, jwtSession.getExpiresAt()));
    }
}

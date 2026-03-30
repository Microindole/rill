package com.indolyn.rill.app.service;

import com.indolyn.rill.app.persistence.entity.AppUserEntity;

import java.time.Instant;

public interface JwtService {

    String issueToken(AppUserEntity user, String jwtId, Instant expiresAt);

    JwtPrincipal verify(String token);
}

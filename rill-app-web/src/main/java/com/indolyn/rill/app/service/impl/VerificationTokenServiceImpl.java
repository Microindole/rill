package com.indolyn.rill.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.indolyn.rill.app.persistence.entity.AppUserEntity;
import com.indolyn.rill.app.persistence.entity.AppVerificationTokenEntity;
import com.indolyn.rill.app.persistence.mapper.AppVerificationTokenMapper;
import com.indolyn.rill.app.service.VerificationTokenService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class VerificationTokenServiceImpl implements VerificationTokenService {

    private static final long TOKEN_TTL_MINUTES = 30;

    private final AppVerificationTokenMapper appVerificationTokenMapper;

    public VerificationTokenServiceImpl(AppVerificationTokenMapper appVerificationTokenMapper) {
        this.appVerificationTokenMapper = appVerificationTokenMapper;
    }

    @Override
    @Transactional
    public AppVerificationTokenEntity create(AppUserEntity user, String purpose) {
        Instant now = Instant.now();
        AppVerificationTokenEntity token = new AppVerificationTokenEntity();
        token.setUserId(user.getId());
        token.setPurpose(purpose);
        token.setToken(UUID.randomUUID().toString().replace("-", ""));
        token.setExpiresAt(now.plus(TOKEN_TTL_MINUTES, ChronoUnit.MINUTES));
        token.setUsed(false);
        token.setCreatedAt(now);
        appVerificationTokenMapper.insert(token);
        return token;
    }

    @Override
    public AppVerificationTokenEntity requireUsableToken(String token, String purpose) {
        AppVerificationTokenEntity existing =
            appVerificationTokenMapper.selectOne(
                new QueryWrapper<AppVerificationTokenEntity>()
                    .eq("token", token)
                    .eq("purpose", purpose)
                    .eq("used", false)
                    .last("limit 1"));
        if (existing == null || existing.getExpiresAt() == null || existing.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification token is invalid or expired");
        }
        return existing;
    }

    @Override
    @Transactional
    public void markUsed(AppVerificationTokenEntity token) {
        token.setUsed(true);
        token.setUsedAt(Instant.now());
        appVerificationTokenMapper.updateById(token);
    }
}

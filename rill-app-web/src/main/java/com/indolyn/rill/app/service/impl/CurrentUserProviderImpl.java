package com.indolyn.rill.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.indolyn.rill.app.persistence.entity.AppUserEntity;
import com.indolyn.rill.app.persistence.mapper.AppUserMapper;
import com.indolyn.rill.app.service.AuthenticatedUser;
import com.indolyn.rill.app.service.CurrentUserProvider;
import com.indolyn.rill.app.security.RequestUserContext;
import com.indolyn.rill.app.security.RequestUserContextHolder;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CurrentUserProviderImpl implements CurrentUserProvider {

    private static final String GUEST_USERNAME = "guest";

    private final AppUserMapper appUserMapper;

    public CurrentUserProviderImpl(AppUserMapper appUserMapper) {
        this.appUserMapper = appUserMapper;
    }

    @Override
    public AppUserEntity requireCurrentUser() {
        RequestUserContext context = RequestUserContextHolder.get();
        if (context != null) {
            return context.user();
        }
        return requireGuestUser();
    }

    @Override
    public AuthenticatedUser requireAuthenticatedUser() {
        RequestUserContext context = RequestUserContextHolder.get();
        if (context == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required");
        }
        return new AuthenticatedUser(context.user(), context.token());
    }

    @Override
    public Long requireCurrentUserId() {
        return requireCurrentUser().getId();
    }

    private AppUserEntity requireGuestUser() {
        AppUserEntity guest =
            appUserMapper.selectOne(new QueryWrapper<AppUserEntity>().eq("username", GUEST_USERNAME).last("limit 1"));
        if (guest == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Guest user not initialized");
        }
        return guest;
    }
}

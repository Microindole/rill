package com.indolyn.rill.app.service;

import com.indolyn.rill.app.persistence.entity.AppUserEntity;
import com.indolyn.rill.app.persistence.mapper.AppUserMapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class CurrentUserProvider {

    private static final long DEFAULT_USER_ID = 1L;
    private static final String USER_ID_HEADER = "X-Rill-User-Id";

    private final AppUserMapper appUserMapper;

    public CurrentUserProvider(AppUserMapper appUserMapper) {
        this.appUserMapper = appUserMapper;
    }

    public AppUserEntity requireCurrentUser() {
        Long userId = resolveCurrentUserId();
        AppUserEntity user = appUserMapper.selectById(userId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current user not found");
        }
        return user;
    }

    public Long requireCurrentUserId() {
        return requireCurrentUser().getId();
    }

    public AppUserEntity login(String username, String password) {
        AppUserEntity user =
            appUserMapper.selectOne(new QueryWrapper<AppUserEntity>().eq("username", username).last("limit 1"));
        if (user == null || !user.getPassword().equals(password)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        return user;
    }

    private Long resolveCurrentUserId() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            HttpServletRequest request = servletAttributes.getRequest();
            String headerValue = request.getHeader(USER_ID_HEADER);
            if (headerValue != null && !headerValue.isBlank()) {
                try {
                    return Long.parseLong(headerValue.trim());
                } catch (NumberFormatException ignored) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid X-Rill-User-Id header");
                }
            }
        }
        return DEFAULT_USER_ID;
    }
}

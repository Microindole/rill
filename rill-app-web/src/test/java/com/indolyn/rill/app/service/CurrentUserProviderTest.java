package com.indolyn.rill.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.indolyn.rill.app.persistence.entity.AppUserEntity;
import com.indolyn.rill.app.persistence.mapper.AppUserMapper;
import com.indolyn.rill.app.security.RequestUserContext;
import com.indolyn.rill.app.security.RequestUserContextHolder;
import com.indolyn.rill.app.service.impl.CurrentUserProviderImpl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.server.ResponseStatusException;

class CurrentUserProviderTest {

    @AfterEach
    void clearRequestContext() {
        RequestUserContextHolder.clear();
    }

    @Test
    void requireCurrentUserShouldResolveAuthenticatedContext() {
        AppUserMapper appUserMapper = Mockito.mock(AppUserMapper.class);
        CurrentUserProvider provider = new CurrentUserProviderImpl(appUserMapper);
        AppUserEntity appUser = user(1L, "demo", "Demo Admin", "demo123", "ADMIN", "demo");
        RequestUserContextHolder.set(new RequestUserContext(appUser, "token-demo", "jti-1"));

        AppUserEntity currentUser = provider.requireCurrentUser();

        assertEquals("demo", currentUser.getUsername());
    }

    @Test
    void requireAuthenticatedUserShouldReturnTokenBoundUser() {
        AppUserMapper appUserMapper = Mockito.mock(AppUserMapper.class);
        CurrentUserProvider provider = new CurrentUserProviderImpl(appUserMapper);
        AppUserEntity appUser = user(1L, "demo", "Demo Admin", "demo123", "ADMIN", "demo");
        RequestUserContextHolder.set(new RequestUserContext(appUser, "token-demo", "jti-1"));

        AuthenticatedUser currentUser = provider.requireAuthenticatedUser();

        assertEquals("demo", currentUser.user().getUsername());
        assertEquals("token-demo", currentUser.token());
    }

    @Test
    void requireAuthenticatedUserShouldRejectAnonymousRequest() {
        AppUserMapper appUserMapper = Mockito.mock(AppUserMapper.class);
        CurrentUserProvider provider = new CurrentUserProviderImpl(appUserMapper);

        assertThrows(ResponseStatusException.class, provider::requireAuthenticatedUser);
    }

    @Test
    void requireCurrentUserShouldFallbackToGuestWithoutToken() {
        AppUserMapper appUserMapper = Mockito.mock(AppUserMapper.class);
        CurrentUserProvider provider = new CurrentUserProviderImpl(appUserMapper);
        when(appUserMapper.selectOne(any())).thenReturn(user(2L, "guest", "Guest", "guest", "GUEST", "default"));

        AppUserEntity currentUser = provider.requireCurrentUser();

        assertEquals("guest", currentUser.getUsername());
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
}

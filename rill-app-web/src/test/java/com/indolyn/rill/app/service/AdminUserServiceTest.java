package com.indolyn.rill.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.indolyn.rill.app.persistence.entity.AppUserEntity;
import com.indolyn.rill.app.persistence.mapper.AppUserMapper;
import com.indolyn.rill.app.service.impl.AdminUserServiceImpl;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AdminUserServiceTest {

    @Test
    void listUsersShouldReturnResponses() {
        AppUserMapper appUserMapper = Mockito.mock(AppUserMapper.class);
        CurrentUserProvider currentUserProvider = Mockito.mock(CurrentUserProvider.class);
        RillQueryService rillQueryService = Mockito.mock(RillQueryService.class);
        AdminUserService service = new AdminUserServiceImpl(appUserMapper, currentUserProvider, rillQueryService);
        when(currentUserProvider.requireAuthenticatedUser())
            .thenReturn(new AuthenticatedUser(user(1L, "demo", "demo@example.com", "ADMIN", "demo"), "token"));
        when(appUserMapper.selectList(any())).thenReturn(List.of(user(2L, "alice", "alice@example.com", "USER", "alice")));

        var responses = service.listUsers();

        assertEquals(1, responses.size());
        assertEquals("alice", responses.get(0).username());
    }

    @Test
    void provisionUserDatabaseShouldCreateDatabase() {
        AppUserMapper appUserMapper = Mockito.mock(AppUserMapper.class);
        CurrentUserProvider currentUserProvider = Mockito.mock(CurrentUserProvider.class);
        RillQueryService rillQueryService = Mockito.mock(RillQueryService.class);
        AdminUserService service = new AdminUserServiceImpl(appUserMapper, currentUserProvider, rillQueryService);
        when(currentUserProvider.requireAuthenticatedUser())
            .thenReturn(new AuthenticatedUser(user(1L, "demo", "demo@example.com", "ADMIN", "demo"), "token"));
        AppUserEntity alice = user(2L, "alice", "alice@example.com", "USER", "alice");
        alice.setKernelDbProvisioned(false);
        when(appUserMapper.selectById(2L)).thenReturn(alice);
        when(rillQueryService.execute("default", "create database alice;")).thenReturn(Mockito.mock(DatabaseExecution.class));

        var response = service.provisionUserDatabase(2L);

        assertEquals(true, response.kernelDbProvisioned());
        verify(appUserMapper).updateById(alice);
    }

    private AppUserEntity user(Long id, String username, String email, String role, String kernelDbName) {
        AppUserEntity user = new AppUserEntity();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setDisplayName(username);
        user.setRole(role);
        user.setKernelDbName(kernelDbName);
        user.setEmailVerified(true);
        user.setKernelDbProvisioned(true);
        return user;
    }
}

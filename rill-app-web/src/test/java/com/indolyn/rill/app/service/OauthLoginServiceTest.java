package com.indolyn.rill.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.indolyn.rill.app.persistence.entity.AppOauthAccountEntity;
import com.indolyn.rill.app.persistence.entity.AppUserEntity;
import com.indolyn.rill.app.persistence.mapper.AppOauthAccountMapper;
import com.indolyn.rill.app.persistence.mapper.AppUserMapper;
import com.indolyn.rill.app.service.impl.OauthLoginServiceImpl;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

class OauthLoginServiceTest {

    @Test
    void beginGithubLoginShouldIssueTokenForExistingOauthAccount() {
        AppUserMapper appUserMapper = Mockito.mock(AppUserMapper.class);
        AppOauthAccountMapper appOauthAccountMapper = Mockito.mock(AppOauthAccountMapper.class);
        RillQueryService rillQueryService = Mockito.mock(RillQueryService.class);
        LoginSessionService loginSessionService = Mockito.mock(LoginSessionService.class);
        OauthPendingStateService oauthPendingStateService = Mockito.mock(OauthPendingStateService.class);
        OauthLoginService service =
            new OauthLoginServiceImpl(
                appUserMapper,
                appOauthAccountMapper,
                rillQueryService,
                new BCryptPasswordEncoder(),
                loginSessionService,
                oauthPendingStateService);
        AppOauthAccountEntity account = new AppOauthAccountEntity();
        account.setId(2L);
        account.setUserId(9L);
        account.setProvider("github");
        account.setProviderUserId("1001");
        AppUserEntity user = user(9L, "octocat", "octocat@example.com", "Octo Cat");
        when(appOauthAccountMapper.selectOne(any())).thenReturn(account);
        when(appUserMapper.selectById(9L)).thenReturn(user);
        when(loginSessionService.issueToken(user)).thenReturn(new AuthenticatedUser(user, "jwt-token"));

        OauthLoginStartResult result =
            service.beginGithubLogin(githubUser("1001", "octocat", "octocat@example.com", "Octo Cat"));

        assertEquals("jwt-token", result.authenticatedUser().token());
        verify(appOauthAccountMapper).updateById(account);
    }

    @Test
    void beginGithubLoginShouldCreatePendingStateWhenMissing() {
        AppUserMapper appUserMapper = Mockito.mock(AppUserMapper.class);
        AppOauthAccountMapper appOauthAccountMapper = Mockito.mock(AppOauthAccountMapper.class);
        RillQueryService rillQueryService = Mockito.mock(RillQueryService.class);
        LoginSessionService loginSessionService = Mockito.mock(LoginSessionService.class);
        OauthPendingStateService oauthPendingStateService = Mockito.mock(OauthPendingStateService.class);
        OauthLoginService service =
            new OauthLoginServiceImpl(
                appUserMapper,
                appOauthAccountMapper,
                rillQueryService,
                new BCryptPasswordEncoder(),
                loginSessionService,
                oauthPendingStateService);
        when(appOauthAccountMapper.selectOne(any())).thenReturn(null);
        when(oauthPendingStateService.create("github", "2002", "new-octo", "github_2002@users.noreply.rill.local", "New Octo"))
            .thenReturn(new OauthPendingState("state-1", "github", "2002", "new-octo", "github_2002@users.noreply.rill.local", "New Octo", java.time.Instant.now().plusSeconds(600)));

        OauthLoginStartResult result = service.beginGithubLogin(githubUser("2002", "new-octo", null, "New Octo"));

        assertEquals("state-1", result.pendingState().state());
        verify(oauthPendingStateService).create("github", "2002", "new-octo", "github_2002@users.noreply.rill.local", "New Octo");
    }

    private OAuth2User githubUser(String id, String login, String email, String name) {
        java.util.Map<String, Object> attributes = new java.util.HashMap<>();
        attributes.put("id", id);
        attributes.put("login", login);
        attributes.put("email", email);
        attributes.put("name", name);
        return new DefaultOAuth2User(
            java.util.List.of(),
            attributes,
            "login");
    }

    private AppUserEntity user(Long id, String username, String email, String displayName) {
        AppUserEntity user = new AppUserEntity();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setDisplayName(displayName);
        user.setRole("USER");
        user.setKernelDbName(username);
        user.setEmailVerified(true);
        user.setKernelDbProvisioned(true);
        return user;
    }
}

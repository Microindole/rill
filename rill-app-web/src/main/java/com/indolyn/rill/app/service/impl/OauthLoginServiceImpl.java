package com.indolyn.rill.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.indolyn.rill.app.persistence.entity.AppOauthAccountEntity;
import com.indolyn.rill.app.persistence.entity.AppUserEntity;
import com.indolyn.rill.app.persistence.mapper.AppOauthAccountMapper;
import com.indolyn.rill.app.persistence.mapper.AppUserMapper;
import com.indolyn.rill.app.service.AuthenticatedUser;
import com.indolyn.rill.app.service.LoginSessionService;
import com.indolyn.rill.app.service.OauthLoginService;
import com.indolyn.rill.app.service.OauthLoginStartResult;
import com.indolyn.rill.app.service.OauthPendingState;
import com.indolyn.rill.app.service.OauthPendingStateService;
import com.indolyn.rill.app.service.RillQueryService;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OauthLoginServiceImpl implements OauthLoginService {

    private static final String PROVIDER_GITHUB = "github";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AppUserMapper appUserMapper;
    private final AppOauthAccountMapper appOauthAccountMapper;
    private final RillQueryService rillQueryService;
    private final PasswordEncoder passwordEncoder;
    private final LoginSessionService loginSessionService;
    private final OauthPendingStateService oauthPendingStateService;

    public OauthLoginServiceImpl(
        AppUserMapper appUserMapper,
        AppOauthAccountMapper appOauthAccountMapper,
        RillQueryService rillQueryService,
        PasswordEncoder passwordEncoder,
        LoginSessionService loginSessionService,
        OauthPendingStateService oauthPendingStateService) {
        this.appUserMapper = appUserMapper;
        this.appOauthAccountMapper = appOauthAccountMapper;
        this.rillQueryService = rillQueryService;
        this.passwordEncoder = passwordEncoder;
        this.loginSessionService = loginSessionService;
        this.oauthPendingStateService = oauthPendingStateService;
    }

    @Override
    @Transactional
    public OauthLoginStartResult beginGithubLogin(OAuth2User oauth2User) {
        String providerUserId = requiredString(oauth2User.getAttribute("id"), "GitHub user id");
        String login = requiredString(oauth2User.getAttribute("login"), "GitHub login");
        String email = normalizedEmail(oauth2User.getAttribute("email"), providerUserId);
        String displayName = normalizedDisplayName(oauth2User.getAttribute("name"), login);

        AppOauthAccountEntity existingAccount =
            appOauthAccountMapper.selectOne(
                new QueryWrapper<AppOauthAccountEntity>()
                    .eq("provider", PROVIDER_GITHUB)
                    .eq("provider_user_id", providerUserId)
                    .last("limit 1"));
        if (existingAccount != null) {
            AppUserEntity user = requireUser(existingAccount.getUserId());
            syncAccount(existingAccount, login, email);
            return new OauthLoginStartResult(loginSessionService.issueToken(user), null);
        }
        OauthPendingState pendingState =
            oauthPendingStateService.create(PROVIDER_GITHUB, providerUserId, login, email, displayName);
        return new OauthLoginStartResult(null, pendingState);
    }

    @Override
    public OauthPendingState requirePendingState(String state) {
        return oauthPendingStateService.require(state);
    }

    @Override
    @Transactional
    public AuthenticatedUser createAccountFromGithub(String state, String username, String displayName) {
        OauthPendingState pendingState = oauthPendingStateService.require(state);
        if (appOauthAccountMapper.selectOne(
                new QueryWrapper<AppOauthAccountEntity>()
                    .eq("provider", pendingState.provider())
                    .eq("provider_user_id", pendingState.providerUserId())
                    .last("limit 1")) != null) {
            oauthPendingStateService.consume(state);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "GitHub account is already linked");
        }
        AppUserEntity user = createUser(username, pendingState.providerEmail(), displayName, pendingState.providerLogin());
        createOauthAccount(user, pendingState.provider(), pendingState.providerUserId(), pendingState.providerLogin(), pendingState.providerEmail());
        oauthPendingStateService.consume(state);
        return loginSessionService.issueToken(user);
    }

    @Override
    @Transactional
    public AuthenticatedUser bindGithubToExistingAccount(String state, String username, String password) {
        OauthPendingState pendingState = oauthPendingStateService.require(state);
        AppUserEntity user =
            appUserMapper.selectOne(new QueryWrapper<AppUserEntity>().eq("username", normalizeUsername(username)).last("limit 1"));
        if (user == null || user.getPassword() == null || !passwordEncoder.matches(normalizePassword(password), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        if (appOauthAccountMapper.selectOne(
                new QueryWrapper<AppOauthAccountEntity>()
                    .eq("provider", pendingState.provider())
                    .eq("provider_user_id", pendingState.providerUserId())
                    .last("limit 1")) != null) {
            oauthPendingStateService.consume(state);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "GitHub account is already linked");
        }
        createOauthAccount(user, pendingState.provider(), pendingState.providerUserId(), pendingState.providerLogin(), pendingState.providerEmail());
        oauthPendingStateService.consume(state);
        return loginSessionService.issueToken(user);
    }

    private AppUserEntity createUser(String requestedUsername, String email, String requestedDisplayName, String loginFallback) {
        Instant now = Instant.now();
        if (appUserMapper.selectOne(new QueryWrapper<AppUserEntity>().eq("email", email).last("limit 1")) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already used by another account");
        }
        String username = allocateUniqueUsername(requestedUsername == null || requestedUsername.isBlank() ? loginFallback : requestedUsername);
        String kernelDbName = allocateUniqueKernelDbName(username);
        AppUserEntity user = new AppUserEntity();
        user.setUsername(username);
        user.setEmail(email);
        user.setDisplayName(normalizedDisplayName(requestedDisplayName, loginFallback));
        user.setPassword(passwordEncoder.encode(randomPassword()));
        user.setRole("USER");
        user.setKernelDbName(kernelDbName);
        user.setEmailVerified(true);
        user.setKernelDbProvisioned(false);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        appUserMapper.insert(user);
        ensureKernelDatabaseExists(kernelDbName);
        user.setKernelDbProvisioned(true);
        user.setUpdatedAt(Instant.now());
        appUserMapper.updateById(user);
        return user;
    }

    private void createOauthAccount(AppUserEntity user, String provider, String providerUserId, String login, String email) {
        Instant now = Instant.now();
        AppOauthAccountEntity account = new AppOauthAccountEntity();
        account.setUserId(user.getId());
        account.setProvider(provider);
        account.setProviderUserId(providerUserId);
        account.setProviderLogin(login);
        account.setProviderEmail(email);
        account.setCreatedAt(now);
        account.setUpdatedAt(now);
        appOauthAccountMapper.insert(account);
    }

    private void syncAccount(AppOauthAccountEntity account, String login, String email) {
        account.setProviderLogin(login);
        account.setProviderEmail(email);
        account.setUpdatedAt(Instant.now());
        appOauthAccountMapper.updateById(account);
    }

    private AppUserEntity requireUser(Long userId) {
        AppUserEntity user = appUserMapper.selectById(userId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Linked user not found");
        }
        return user;
    }

    private String allocateUniqueUsername(String login) {
        String base = normalizeIdentifier(login, "github_user");
        return allocateUniqueValue(base, false);
    }

    private String allocateUniqueKernelDbName(String username) {
        String base = normalizeIdentifier(username, "github_db");
        return allocateUniqueValue(base, true);
    }

    private String allocateUniqueValue(String base, boolean databaseName) {
        String candidate = base;
        int suffix = 1;
        while (exists(candidate, databaseName)) {
            candidate = base + "_" + suffix++;
        }
        return candidate;
    }

    private boolean exists(String value, boolean databaseName) {
        QueryWrapper<AppUserEntity> query = new QueryWrapper<AppUserEntity>();
        if (databaseName) {
            query.eq("kernel_db_name", value);
        } else {
            query.eq("username", value);
        }
        return appUserMapper.selectOne(query.last("limit 1")) != null;
    }

    private String normalizeIdentifier(String value, String fallback) {
        String normalized = value == null ? fallback : value.trim().toLowerCase().replaceAll("[^a-z0-9_]", "_");
        normalized = normalized.replaceAll("_+", "_");
        normalized = normalized.replaceAll("^_+|_+$", "");
        if (normalized.isBlank() || "default".equals(normalized) || "guest".equals(normalized)) {
            return fallback;
        }
        return normalized;
    }

    private String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username cannot be empty");
        }
        return username.trim();
    }

    private String normalizePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password cannot be empty");
        }
        return password;
    }

    private void ensureKernelDatabaseExists(String kernelDbName) {
        try {
            rillQueryService.execute("default", "create database " + kernelDbName + ";");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to provision kernel database");
        }
    }

    private String requiredString(Object value, String fieldName) {
        if (value == null || value.toString().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is missing");
        }
        return value.toString().trim();
    }

    private String normalizedEmail(Object email, String providerUserId) {
        if (email == null || email.toString().isBlank()) {
            return "github_" + providerUserId + "@users.noreply.rill.local";
        }
        return email.toString().trim().toLowerCase();
    }

    private String normalizedDisplayName(Object name, String login) {
        if (name == null || name.toString().isBlank()) {
            return login;
        }
        return name.toString().trim();
    }

    private String randomPassword() {
        byte[] bytes = new byte[24];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}

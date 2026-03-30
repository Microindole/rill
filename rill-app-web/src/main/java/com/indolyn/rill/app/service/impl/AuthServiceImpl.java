package com.indolyn.rill.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.indolyn.rill.app.persistence.entity.AppJwtSessionEntity;
import com.indolyn.rill.app.persistence.entity.AppUserEntity;
import com.indolyn.rill.app.persistence.entity.AppVerificationTokenEntity;
import com.indolyn.rill.app.persistence.mapper.AppJwtSessionMapper;
import com.indolyn.rill.app.persistence.mapper.AppUserMapper;
import com.indolyn.rill.app.security.RequestUserContext;
import com.indolyn.rill.app.security.RequestUserContextHolder;
import com.indolyn.rill.app.service.AuthService;
import com.indolyn.rill.app.service.AuthenticatedUser;
import com.indolyn.rill.app.service.CaptchaVerificationService;
import com.indolyn.rill.app.service.JwtService;
import com.indolyn.rill.app.service.MailService;
import com.indolyn.rill.app.service.RillQueryService;
import com.indolyn.rill.app.service.VerificationTokenService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthServiceImpl implements AuthService {

    private static final String GUEST_USERNAME = "guest";

    private final AppUserMapper appUserMapper;
    private final AppJwtSessionMapper appJwtSessionMapper;
    private final JwtService jwtService;
    private final RillQueryService rillQueryService;
    private final PasswordEncoder passwordEncoder;
    private final CaptchaVerificationService captchaVerificationService;
    private final VerificationTokenService verificationTokenService;
    private final MailService mailService;
    private final long tokenTtlDays;
    private final String frontendBaseUrl;

    public AuthServiceImpl(
        AppUserMapper appUserMapper,
        AppJwtSessionMapper appJwtSessionMapper,
        JwtService jwtService,
        RillQueryService rillQueryService,
        PasswordEncoder passwordEncoder,
        CaptchaVerificationService captchaVerificationService,
        VerificationTokenService verificationTokenService,
        MailService mailService,
        @Value("${app.auth.jwt-ttl-days:7}") long tokenTtlDays,
        @Value("${app.auth.frontend-base-url:http://localhost:5173}") String frontendBaseUrl) {
        this.appUserMapper = appUserMapper;
        this.appJwtSessionMapper = appJwtSessionMapper;
        this.jwtService = jwtService;
        this.rillQueryService = rillQueryService;
        this.passwordEncoder = passwordEncoder;
        this.captchaVerificationService = captchaVerificationService;
        this.verificationTokenService = verificationTokenService;
        this.mailService = mailService;
        this.tokenTtlDays = tokenTtlDays;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Override
    @Transactional
    public void register(String username, String email, String displayName, String password) {
        String normalizedUsername = normalizeUsername(username);
        String normalizedEmail = normalizeEmail(email);
        String normalizedDisplayName = normalizeDisplayName(displayName);
        String normalizedPassword = normalizePassword(password);
        AppUserEntity existing =
            appUserMapper.selectOne(
                new QueryWrapper<AppUserEntity>().eq("username", normalizedUsername).last("limit 1"));
        if (existing != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }
        AppUserEntity emailExisting =
            appUserMapper.selectOne(
                new QueryWrapper<AppUserEntity>().eq("email", normalizedEmail).last("limit 1"));
        if (emailExisting != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }
        Instant now = Instant.now();
        AppUserEntity user = new AppUserEntity();
        user.setUsername(normalizedUsername);
        user.setEmail(normalizedEmail);
        user.setDisplayName(normalizedDisplayName);
        user.setPassword(passwordEncoder.encode(normalizedPassword));
        user.setRole("USER");
        user.setKernelDbName(allocateKernelDatabaseName(normalizedUsername));
        user.setEmailVerified(false);
        user.setKernelDbProvisioned(false);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        appUserMapper.insert(user);
        AppVerificationTokenEntity token = verificationTokenService.create(user, VerificationTokenService.PURPOSE_REGISTER);
        sendVerificationEmail(user, token);
    }

    @Override
    @Transactional
    public AuthenticatedUser confirmEmail(String token) {
        AppVerificationTokenEntity verificationToken =
            verificationTokenService.requireUsableToken(token, VerificationTokenService.PURPOSE_REGISTER);
        AppUserEntity user = requireUser(verificationToken.getUserId());
        if (!user.isKernelDbProvisioned()) {
            ensureKernelDatabaseExists(user.getKernelDbName());
            user.setKernelDbProvisioned(true);
        }
        user.setEmailVerified(true);
        user.setUpdatedAt(Instant.now());
        appUserMapper.updateById(user);
        verificationTokenService.markUsed(verificationToken);
        return issueToken(user);
    }

    @Override
    @Transactional
    public AuthenticatedUser login(String username, String password, String captchaToken) {
        captchaVerificationService.verifyLoginCaptcha(captchaToken);
        String normalizedUsername = normalizeUsername(username);
        String normalizedPassword = normalizePassword(password);
        AppUserEntity user =
            appUserMapper.selectOne(
                new QueryWrapper<AppUserEntity>().eq("username", normalizedUsername).last("limit 1"));
        if (user == null || user.getPassword() == null || !passwordEncoder.matches(normalizedPassword, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        if (!user.isEmailVerified() && !GUEST_USERNAME.equalsIgnoreCase(user.getUsername())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Email verification required");
        }
        return issueToken(user);
    }

    @Override
    @Transactional
    public void requestPasswordChange(Long userId, String currentPassword, String newPassword) {
        AppUserEntity user = requireUser(userId);
        if (!passwordEncoder.matches(normalizePassword(currentPassword), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }
        normalizePassword(newPassword);
        AppVerificationTokenEntity token =
            verificationTokenService.create(user, VerificationTokenService.PURPOSE_PASSWORD_CHANGE);
        sendPasswordChangeEmail(user, token);
    }

    @Override
    @Transactional
    public void confirmPasswordChange(String token, String newPassword) {
        AppVerificationTokenEntity verificationToken =
            verificationTokenService.requireUsableToken(token, VerificationTokenService.PURPOSE_PASSWORD_CHANGE);
        updatePassword(verificationToken, normalizePassword(newPassword));
    }

    @Override
    @Transactional
    public void requestPasswordReset(String email) {
        String normalizedEmail = normalizeEmail(email);
        AppUserEntity user =
            appUserMapper.selectOne(new QueryWrapper<AppUserEntity>().eq("email", normalizedEmail).last("limit 1"));
        if (user == null) {
            return;
        }
        AppVerificationTokenEntity token =
            verificationTokenService.create(user, VerificationTokenService.PURPOSE_PASSWORD_RESET);
        sendPasswordResetEmail(user, token);
    }

    @Override
    @Transactional
    public void confirmPasswordReset(String token, String newPassword) {
        AppVerificationTokenEntity verificationToken =
            verificationTokenService.requireUsableToken(token, VerificationTokenService.PURPOSE_PASSWORD_RESET);
        updatePassword(verificationToken, normalizePassword(newPassword));
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
        jwtSession.setExpiresAt(now.plus(tokenTtlDays, ChronoUnit.DAYS));
        jwtSession.setRevoked(false);
        jwtSession.setCreatedAt(now);
        jwtSession.setUpdatedAt(now);
        appJwtSessionMapper.insert(jwtSession);
        return new AuthenticatedUser(user, jwtService.issueToken(user, jwtId, jwtSession.getExpiresAt()));
    }

    private void updatePassword(AppVerificationTokenEntity verificationToken, String newPassword) {
        AppUserEntity user = requireUser(verificationToken.getUserId());
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(Instant.now());
        appUserMapper.updateById(user);
        verificationTokenService.markUsed(verificationToken);
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

    private AppUserEntity requireUser(Long userId) {
        AppUserEntity user = appUserMapper.selectById(userId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        return user;
    }

    private String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username cannot be empty");
        }
        return username.trim();
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email cannot be empty");
        }
        return email.trim().toLowerCase();
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

    private void sendVerificationEmail(AppUserEntity user, AppVerificationTokenEntity token) {
        String link = frontendBaseUrl + "/login?mode=verify&token=" + token.getToken();
        mailService.sendEmail(
            user.getEmail(),
            "Verify your Rill account",
            "Click the link to verify your account within 30 minutes:\n" + link);
    }

    private void sendPasswordChangeEmail(AppUserEntity user, AppVerificationTokenEntity token) {
        String link = frontendBaseUrl + "/login?mode=change-password&token=" + token.getToken();
        mailService.sendEmail(
            user.getEmail(),
            "Confirm your Rill password change",
            "Click the link to confirm your password change within 30 minutes:\n" + link);
    }

    private void sendPasswordResetEmail(AppUserEntity user, AppVerificationTokenEntity token) {
        String link = frontendBaseUrl + "/login?mode=reset-password&token=" + token.getToken();
        mailService.sendEmail(
            user.getEmail(),
            "Reset your Rill password",
            "Click the link to reset your password within 30 minutes:\n" + link);
    }
}

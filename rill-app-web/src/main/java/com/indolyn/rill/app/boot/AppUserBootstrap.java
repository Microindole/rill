package com.indolyn.rill.app.boot;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.indolyn.rill.app.persistence.entity.AppUserEntity;
import com.indolyn.rill.app.persistence.mapper.AppUserMapper;

import java.time.Instant;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AppUserBootstrap implements ApplicationRunner {

    private final AppUserMapper appUserMapper;
    private final PasswordEncoder passwordEncoder;

    public AppUserBootstrap(AppUserMapper appUserMapper, PasswordEncoder passwordEncoder) {
        this.appUserMapper = appUserMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureUser("demo", "demo@example.com", "Demo Admin", "demo123", "ADMIN", "demo", true, true);
        ensureUser("guest", "guest@example.com", "Guest", "guest", "GUEST", "default", true, true);
    }

    private void ensureUser(
        String username,
        String email,
        String displayName,
        String rawPassword,
        String role,
        String kernelDbName,
        boolean emailVerified,
        boolean kernelDbProvisioned) {
        AppUserEntity existing =
            appUserMapper.selectOne(new QueryWrapper<AppUserEntity>().eq("username", username).last("limit 1"));
        if (existing == null) {
            Instant now = Instant.now();
            AppUserEntity user = new AppUserEntity();
            user.setUsername(username);
            user.setEmail(email);
            user.setDisplayName(displayName);
            user.setPassword(passwordEncoder.encode(rawPassword));
            user.setRole(role);
            user.setKernelDbName(kernelDbName);
            user.setEmailVerified(emailVerified);
            user.setKernelDbProvisioned(kernelDbProvisioned);
            user.setCreatedAt(now);
            user.setUpdatedAt(now);
            appUserMapper.insert(user);
            return;
        }
        existing.setEmail(email);
        existing.setEmailVerified(emailVerified);
        existing.setKernelDbProvisioned(kernelDbProvisioned);
        if (existing.getPassword() == null || !existing.getPassword().startsWith("$2")) {
            existing.setPassword(passwordEncoder.encode(rawPassword));
        }
        existing.setUpdatedAt(Instant.now());
        appUserMapper.updateById(existing);
    }
}

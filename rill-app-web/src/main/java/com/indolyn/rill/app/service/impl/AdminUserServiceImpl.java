package com.indolyn.rill.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.indolyn.rill.app.dto.AdminUserResponse;
import com.indolyn.rill.app.persistence.entity.AppUserEntity;
import com.indolyn.rill.app.persistence.mapper.AppUserMapper;
import com.indolyn.rill.app.service.AdminUserService;
import com.indolyn.rill.app.service.CurrentUserProvider;
import com.indolyn.rill.app.service.RillQueryService;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    private final AppUserMapper appUserMapper;
    private final CurrentUserProvider currentUserProvider;
    private final RillQueryService rillQueryService;

    public AdminUserServiceImpl(
        AppUserMapper appUserMapper, CurrentUserProvider currentUserProvider, RillQueryService rillQueryService) {
        this.appUserMapper = appUserMapper;
        this.currentUserProvider = currentUserProvider;
        this.rillQueryService = rillQueryService;
    }

    @Override
    public List<AdminUserResponse> listUsers() {
        requireAdmin();
        return appUserMapper.selectList(new QueryWrapper<>()).stream().map(this::toResponse).toList();
    }

    @Override
    public AdminUserResponse getUser(Long userId) {
        requireAdmin();
        return toResponse(requireUser(userId));
    }

    @Override
    @Transactional
    public AdminUserResponse provisionUserDatabase(Long userId) {
        requireAdmin();
        AppUserEntity user = requireUser(userId);
        ensureDatabaseExists(user.getKernelDbName());
        user.setKernelDbProvisioned(true);
        user.setUpdatedAt(Instant.now());
        appUserMapper.updateById(user);
        return toResponse(user);
    }

    @Override
    @Transactional
    public void deleteUserDatabase(Long userId) {
        requireAdmin();
        AppUserEntity user = requireUser(userId);
        if (user.getKernelDbName() == null || user.getKernelDbName().isBlank() || "default".equals(user.getKernelDbName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "System database cannot be deleted");
        }
        try {
            rillQueryService.execute("default", "drop database " + user.getKernelDbName() + ";");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete kernel database");
        }
        user.setKernelDbProvisioned(false);
        user.setUpdatedAt(Instant.now());
        appUserMapper.updateById(user);
    }

    private void requireAdmin() {
        AppUserEntity user = currentUserProvider.requireAuthenticatedUser().user();
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role required");
        }
    }

    private AppUserEntity requireUser(Long userId) {
        AppUserEntity user = appUserMapper.selectById(userId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        return user;
    }

    private void ensureDatabaseExists(String kernelDbName) {
        try {
            rillQueryService.execute("default", "create database " + kernelDbName + ";");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to provision kernel database");
        }
    }

    private AdminUserResponse toResponse(AppUserEntity user) {
        return new AdminUserResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.isEmailVerified(),
            user.getDisplayName(),
            user.getRole(),
            user.getKernelDbName(),
            user.isKernelDbProvisioned());
    }
}

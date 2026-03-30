package com.indolyn.rill.app.service;

import com.indolyn.rill.app.dto.AdminUserResponse;

import java.util.List;

public interface AdminUserService {

    List<AdminUserResponse> listUsers();

    AdminUserResponse getUser(Long userId);

    AdminUserResponse provisionUserDatabase(Long userId);

    void deleteUserDatabase(Long userId);
}

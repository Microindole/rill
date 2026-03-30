package com.indolyn.rill.app.controller;

import com.indolyn.rill.app.dto.AdminUserResponse;
import com.indolyn.rill.app.service.AdminUserService;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public List<AdminUserResponse> listUsers() {
        return adminUserService.listUsers();
    }

    @GetMapping("/{userId}")
    public AdminUserResponse getUser(@PathVariable Long userId) {
        return adminUserService.getUser(userId);
    }

    @PostMapping("/{userId}/database/provision")
    public AdminUserResponse provisionUserDatabase(@PathVariable Long userId) {
        return adminUserService.provisionUserDatabase(userId);
    }

    @DeleteMapping("/{userId}/database")
    public void deleteUserDatabase(@PathVariable Long userId) {
        adminUserService.deleteUserDatabase(userId);
    }
}

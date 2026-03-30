package com.indolyn.rill.app.service;

public interface AuthService {

    AuthenticatedUser register(String username, String displayName, String password);

    AuthenticatedUser login(String username, String password);

    void logout();
}

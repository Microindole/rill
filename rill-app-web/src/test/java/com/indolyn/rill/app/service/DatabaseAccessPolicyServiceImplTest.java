package com.indolyn.rill.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.indolyn.rill.app.persistence.entity.AppUserEntity;
import com.indolyn.rill.app.service.impl.DatabaseAccessPolicyServiceImpl;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class DatabaseAccessPolicyServiceImplTest {

    private final DatabaseAccessPolicyServiceImpl service = new DatabaseAccessPolicyServiceImpl();

    @Test
    void guestShouldOnlySeeDefaultDatabase() {
        List<String> databases = service.accessibleDatabases(
            user("guest", "GUEST", "default"),
            List.of("default", "demo", "bill"));

        assertEquals(List.of("default"), databases);
    }

    @Test
    void authenticatedUserShouldSeeAllLoadedDatabases() {
        List<String> databases = service.accessibleDatabases(
            user("alice", "USER", "alice"),
            List.of("default", "alice", "bill"));

        assertEquals(List.of("default", "alice"), databases);
    }

    @Test
    void authenticatedUserShouldSeeOwnDatabaseEvenWhenNotLoaded() {
        List<String> databases = service.accessibleDatabases(
            user("alice", "USER", "alice"),
            List.of("default"));

        assertEquals(List.of("default", "alice"), databases);
    }

    @Test
    void authenticatedUserShouldNotBeAbleToUseOtherUserDatabase() {
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.assertCanUseDatabase(
                user("alice", "USER", "alice"),
                "bill",
                List.of("default", "alice", "bill")));

        assertEquals(403, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("cannot access database"));
    }

    @Test
    void guestShouldNotBeAbleToUseOtherDatabase() {
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.assertCanUseDatabase(
                user("guest", "GUEST", "default"),
                "bill",
                List.of("default", "alice", "bill")));

        assertEquals(403, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("cannot access database"));
    }

    @Test
    void nonAdminShouldOnlyCreateOwnDatabase() {
        service.assertCanExecute(
            user("alice", "USER", "alice"),
            "alice",
            "create database alice;",
            List.of("default", "alice", "bill"));
    }

    @Test
    void rootShouldSeeLoadedDatabasesAndCreateAnyDatabase() {
        List<String> databases = service.accessibleDatabases(
            user("root", "ADMIN", "default"),
            List.of("default", "alice", "bill"));

        assertEquals(List.of("default", "alice", "bill"), databases);
        service.assertCanExecute(
            user("root", "ADMIN", "default"),
            "default",
            "create database demo;",
            List.of("default", "alice", "bill"));
    }

    @Test
    void rootShouldDropOtherDatabase() {
        service.assertCanExecute(
            user("root", "ADMIN", "default"),
            "default",
            "drop database alice;",
            List.of("default", "alice", "bill"));
    }

    @Test
    void authenticatedUserShouldBeAbleToUseDefaultDatabase() {
        service.assertCanExecute(
            user("alice", "USER", "alice"),
            "alice",
            "use default;",
            List.of("default", "alice"));
    }

    @Test
    void nonAdminShouldNotCreateForeignDatabase() {
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.assertCanExecute(
                user("alice", "USER", "alice"),
                "alice",
                "create database bill;",
                List.of("default", "alice", "bill")));

        assertEquals(403, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("only create own database"));
    }

    @Test
    void nonRootShouldNotDropDatabase() {
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.assertCanExecute(
                user("alice", "USER", "alice"),
                "alice",
                "drop database alice;",
                List.of("default", "alice", "bill")));

        assertEquals(403, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("Only root can drop databases"));
    }

    private AppUserEntity user(String username, String role, String kernelDbName) {
        AppUserEntity user = new AppUserEntity();
        user.setUsername(username);
        user.setRole(role);
        user.setKernelDbName(kernelDbName);
        return user;
    }
}

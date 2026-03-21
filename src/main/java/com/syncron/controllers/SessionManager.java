package com.syncron.controllers;

import com.syncron.models.User;

public class SessionManager {
    private static final java.util.concurrent.atomic.AtomicReference<String> currentUserRole =
            new java.util.concurrent.atomic.AtomicReference<>("");
    private static final java.util.concurrent.atomic.AtomicReference<User> currentUser =
            new java.util.concurrent.atomic.AtomicReference<>();

    private SessionManager() {
    }

    public static String getCurrentUserRole() {
        return currentUserRole.get();
    }

    public static void setCurrentUserRole(String currentUserRole) {
        SessionManager.currentUserRole.set(currentUserRole == null ? "" : currentUserRole);
    }

    public static User getCurrentUser() {
        return currentUser.get();
    }

    public static void setCurrentUser(User user) {
        currentUser.set(user);
    }
}

package com.syncron.controllers;

public class SessionManager {
    private static final java.util.concurrent.atomic.AtomicReference<String> currentUserRole =
            new java.util.concurrent.atomic.AtomicReference<>("");

    private SessionManager() {
    }

    public static String getCurrentUserRole() {
        return currentUserRole.get();
    }

    public static void setCurrentUserRole(String currentUserRole) {
        SessionManager.currentUserRole.set(currentUserRole == null ? "" : currentUserRole);
    }
}

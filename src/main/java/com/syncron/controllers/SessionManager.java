package com.syncron.controllers;

public class SessionManager {
    private static String currentUserRole = "";

    private SessionManager() {
    }

    public static String getCurrentUserRole() {
        return currentUserRole;
    }

    public static void setCurrentUserRole(String currentUserRole) {
        SessionManager.currentUserRole = currentUserRole;
    }
}

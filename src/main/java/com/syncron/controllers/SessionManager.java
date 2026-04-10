package com.syncron.controllers;

import com.syncron.models.User;

public class SessionManager {
    private static final java.util.concurrent.atomic.AtomicReference<String> currentUserRole =
            new java.util.concurrent.atomic.AtomicReference<>("");
    private static final java.util.concurrent.atomic.AtomicReference<User> currentUser =
            new java.util.concurrent.atomic.AtomicReference<>();

    private static final java.util.concurrent.atomic.AtomicReference<String> currentCourseCode =
            new java.util.concurrent.atomic.AtomicReference<>("");

    private SessionManager() {}

    public static String getCurrentUserRole() {
        return currentUserRole.get();
    }

    public static void setCurrentUserRole(String currentUserRole) {
        SessionManager.currentUserRole.set(currentUserRole == null ? "" : currentUserRole);
    }

    public static User getCurrentUser() {return currentUser.get();}


    public static void setCurrentUser(User user) {currentUser.set(user);}

    // Course Context Memory
    public static String getCurrentCourseCode() { return currentCourseCode.get(); }
    public static void setCurrentCourseCode(String code) { currentCourseCode.set(code == null ? "" : code); }

    // Evaluation Memory
    private static final java.util.concurrent.atomic.AtomicReference<String> currentEvaluationId = new java.util.concurrent.atomic.AtomicReference<>("");
    public static String getCurrentEvaluationId() { return currentEvaluationId.get(); }
    public static void setCurrentEvaluationId(String id) { currentEvaluationId.set(id == null ? "" : id); }


    // Evaluations Edit Memory
    private static final java.util.concurrent.atomic.AtomicReference<String> editEvaluationId = new java.util.concurrent.atomic.AtomicReference<>("");
    public static String getEditEvaluationId() { return editEvaluationId.get(); }
    public static void setEditEvaluationId(String id) { editEvaluationId.set(id == null ? "" : id); }

    // memory for viewing other people's profiles
    private static final java.util.concurrent.atomic.AtomicReference<String> viewProfileId = new java.util.concurrent.atomic.AtomicReference<>("");
    public static String getViewProfileId() { return viewProfileId.get(); }
    public static void setViewProfileId(String id) { viewProfileId.set(id == null ? "" : id); }

    // The Sidebar Memory anchor
    private static String lastSidebarTab = "OFFLINE";
    public static String getLastSidebarTab() { return lastSidebarTab; }
    public static void setLastSidebarTab(String tab) { lastSidebarTab = tab; }

    //
    private static String currentCourseType;
    public static String getCurrentCourseType() { return currentCourseType; }
    public static void setCurrentCourseType(String type) { currentCourseType = type; }


    private static String currentAnnouncementId;
    public static String getCurrentAnnouncementId() { return currentAnnouncementId; }
    public static void setCurrentAnnouncementId(String id) { currentAnnouncementId = id; }


    private static String editAnnouncementId;
    public static String getEditAnnouncementId() { return editAnnouncementId; }
    public static void setEditAnnouncementId(String id) { editAnnouncementId = id; }


    private static boolean cameFromCalendar = false;
    public static boolean isCameFromCalendar() { return cameFromCalendar; }
    public static void setCameFromCalendar(boolean val) { cameFromCalendar = val; }
}

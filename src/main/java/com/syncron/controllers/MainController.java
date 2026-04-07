package com.syncron.controllers;

import com.syncron.utils.NavigationManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.controlsfx.control.PopOver; // 🚀 Modern Dropdown UI

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainController {
    public static MainController instance;

    @FXML private BorderPane mainBorderPane;
    @FXML private HBox breadcrumbBar;
    @FXML private VBox sidebar;
    @FXML private VBox sidebarButtonContainer;
    @FXML private StackPane contentArea;
    @FXML private VBox courseHeaderBox;
    @FXML private Label courseHeaderLabel;
    @FXML private Label courseTypeFlair;
    @FXML private Label creditsFlair;

    @FXML private HBox profileBtn;
    @FXML private Button notificationBtn;

    private PopOver notificationsPopOver; // The beautiful new dropdown

    private String courseCode = "";
    private String courseTitle = "";
    private String courseType = "theory";
    private String courseCredits = "3.0";
    private Button activeButton;

    private static final String[] COMMON_BUTTONS = {"Common", "Announcements"};
    private static final String[] THEORY_BUTTONS = {"CT and Assignments", "Weekly Timeline", "Grades", "Participants"};
    private static final String[] SESSIONAL_BUTTONS = {"Offlines", "Onlines", "Weekly Timeline", "Grades", "Participants"};

    private String getFxmlPathForButton(String buttonLabel) {
        return switch (buttonLabel) {
            case "Common" -> "common.fxml";
            case "Announcements" -> "announcements.fxml";
            case "CT and Assignments" -> "ct_assignments.fxml";
            case "Weekly Timeline" -> "weekly_timeline.fxml";
            case "Grades" -> "grades.fxml";
            case "Participants" -> "participants.fxml";
            case "Offlines" -> "offlines.fxml";
            case "Onlines" -> "onlines.fxml";
            default -> null;
        };
    }

    @FXML
    public void initialize() {
        instance = this;
        NavigationManager.initialize(contentArea, this);
        buildSidebar();
        NavigationManager.switchScreen("common.fxml");

        if (profileBtn != null) {
            profileBtn.setOnMouseClicked(e -> openProfile());
        }
    }

    /* -------------------------------------------------------------------------
     * 🔥 MODERN NOTIFICATION SYSTEM
     * -----------------------------------------------------------------------*/

    @FXML
    private void showNotifications() {
        if (notificationsPopOver == null) {
            VBox container = new VBox(15);
            container.setStyle("-fx-background-color: rgba(255, 255, 255, 0.98); -fx-padding: 20; -fx-background-radius: 12;");
            container.setPrefWidth(380);
            container.setPrefHeight(450);

            Label header = new Label("Recent Announcements");
            header.setStyle("-fx-font-family: 'Georgia', serif; -fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

            VBox notifList = new VBox(10);
            ScrollPane scrollPane = new ScrollPane(notifList);
            scrollPane.setFitToWidth(true);
            scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

            container.getChildren().addAll(header, scrollPane);

            notificationsPopOver = new PopOver(container);
            notificationsPopOver.setArrowLocation(PopOver.ArrowLocation.TOP_RIGHT);
            notificationsPopOver.setDetachable(false);
            notificationsPopOver.setCornerRadius(12);

            // Async Fetching so the UI doesn't freeze!
            notifList.getChildren().add(new Label("Scanning your courses..."));

            new Thread(() -> {
                List<Map<String, String>> allNotifs = fetchAllUserNotifications();
                Platform.runLater(() -> {
                    notifList.getChildren().clear();
                    if (allNotifs.isEmpty()) {
                        Label empty = new Label("All caught up! No new notifications.");
                        empty.setStyle("-fx-text-fill: #95A5A6; -fx-font-style: italic;");
                        notifList.getChildren().add(empty);
                    } else {
                        for (Map<String, String> notif : allNotifs) {
                            notifList.getChildren().add(createNotifCard(notif));
                        }
                    }
                });
            }).start();
        }

        if (notificationsPopOver.isShowing()) {
            notificationsPopOver.hide();
        } else {
            notificationsPopOver.show(notificationBtn);
        }
    }

    private VBox createNotifCard(Map<String, String> notif) {
        VBox card = new VBox(8);
        String baseStyle = "-fx-background-color: #F8F9FA; -fx-padding: 12; -fx-border-radius: 10; -fx-background-radius: 10; -fx-cursor: hand;";
        String hoverStyle = "-fx-background-color: #EBF5FB; -fx-padding: 12; -fx-border-radius: 10; -fx-background-radius: 10; -fx-cursor: hand; -fx-border-color: #3498DB; -fx-border-width: 1.5;";

        card.setStyle(baseStyle);
        card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
        card.setOnMouseExited(e -> card.setStyle(baseStyle));

        Label badge = new Label(notif.get("courseCode"));
        badge.setStyle("-fx-background-color: #D6EAF8; -fx-text-fill: #2980B9; -fx-font-weight: bold; -fx-padding: 3 8 3 8; -fx-background-radius: 12; -fx-font-size: 11px;");

        Label author = new Label(notif.get("author") + " • " + notif.get("timestamp"));
        author.setStyle("-fx-text-fill: #7F8C8D; -fx-font-size: 11px;");

        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);
        topRow.getChildren().addAll(badge, author);

        // Preview snippet of the announcement
        String cleanMsg = notif.get("message").replaceAll("\\[ASSESSMENT:\\d+:([^\\]]+)\\]", "📌 $1");
        Label msg = new Label(cleanMsg);
        msg.setStyle("-fx-text-fill: #2C3E50; -fx-font-size: 13px; -fx-font-weight: bold;");
        msg.setWrapText(true);
        msg.setMaxWidth(300);

        card.getChildren().addAll(topRow, msg);

        // The Magic Routing!
        card.setOnMouseClicked(e -> {
            notificationsPopOver.hide();
            navigateToAnnouncement(
                    notif.get("courseCode"),
                    notif.get("courseTitle"),
                    notif.get("courseType"),
                    notif.get("id")
            );
        });

        return card;
    }

    private List<Map<String, String>> fetchAllUserNotifications() {
        List<Map<String, String>> results = new ArrayList<>();
        try {
            String role = SessionManager.getCurrentUser().getRole().toLowerCase();
            String id = SessionManager.getCurrentUser().getId();
            String url = "http://localhost:8080/api/dashboard/" + role + "/" + id;

            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder().uri(java.net.URI.create(url)).GET().build();
            java.net.http.HttpResponse<String> res = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (res.statusCode() == 200) {
                com.google.gson.Gson gson = new com.google.gson.Gson();
                Map<String, Object> data = gson.fromJson(res.body(), new com.google.gson.reflect.TypeToken<Map<String, Object>>(){}.getType());

                List<Map<String, Object>> courses = null;
                if (data.containsKey("courses")) courses = (List<Map<String, Object>>) data.get("courses");
                else if (data.containsKey("assignedCourses")) courses = (List<Map<String, Object>>) data.get("assignedCourses");

                if (courses != null) {
                    for (Map<String, Object> course : courses) {
                        String code = String.valueOf(course.get("code"));
                        if (code == null || code.equals("null")) code = String.valueOf(course.get("courseCode"));

                        String title = course.get("title") != null ? String.valueOf(course.get("title")) : "Course";
                        String type = course.get("type") != null ? String.valueOf(course.get("type")) : "theory";

                        java.net.http.HttpRequest aReq = java.net.http.HttpRequest.newBuilder()
                                .uri(java.net.URI.create("http://localhost:8080/api/announcements/" + code.replace(" ", "%20")))
                                .GET().build();
                        java.net.http.HttpResponse<String> aRes = client.send(aReq, java.net.http.HttpResponse.BodyHandlers.ofString());

                        if (aRes.statusCode() == 200) {
                            List<Map<String, String>> posts = gson.fromJson(aRes.body(), new com.google.gson.reflect.TypeToken<List<Map<String, String>>>(){}.getType());
                            for (Map<String, String> p : posts) {
                                p.put("courseCode", code);
                                p.put("courseTitle", title);
                                p.put("courseType", type);
                                results.add(p);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return results;
    }

    /**
     * Cross-Course Teleportation System for Notifications
     */
    public void navigateToAnnouncement(String targetCode, String targetTitle, String targetType, String announcementId) {
        // 1. Force the layout into the target course context (Sidebar & Headers will reset automatically)
        if (!targetCode.equals(this.courseCode)) {
            setCourseContext(targetCode, targetTitle, targetType, "3.0");
        }

        // 2. Queue the announcement up
        SessionManager.setCurrentAnnouncementId(announcementId);

        // 3. Navigate & Update Sidebar
        NavigationManager.switchScreen("view_announcement.fxml");
        forceSidebarSelection("Announcements");

        // 4. Set the breadcrumb precisely so it doesn't corrupt!
        updateBreadcrumb("Announcements / View Post");
    }

    /* -------------------------------------------------------------------------
     * CORE ROUTING & BREADCRUMBS
     * -----------------------------------------------------------------------*/

    // start change
    public void setCourseContext(String courseCode, String courseTitle, String courseType, String credits) {
        this.courseCode = courseCode != null ? courseCode : "";
        this.courseTitle = courseTitle != null ? courseTitle : "";

        // THE STRICT NORMALIZATION FIX
        if (courseType != null) {
            this.courseType = courseType.trim().toLowerCase();
        } else {
            // Failsafe: if somehow null, guess based on title or code
            boolean isSessional = this.courseTitle.toLowerCase().contains("sessional") || this.courseCode.matches(".*[02468]$");
            this.courseType = isSessional ? "sessional" : "theory";
        }

        SessionManager.setCurrentCourseType(this.courseType);

        this.courseCredits = credits != null ? credits : "3.0";
        buildSidebar();
        updateCourseHeader();
        if (!sidebarButtonContainer.getChildren().isEmpty()) {
            Button firstButton = (Button) sidebarButtonContainer.getChildren().get(0);
            setActiveButton(firstButton);
        }
        NavigationManager.switchScreen("common.fxml");
        updateBreadcrumb("Common");
    }
// End change

    private void updateCourseHeader() {
        if (courseCode != null && !courseCode.isEmpty()) {
            String heading = courseCode;
            if (courseTitle != null && !courseTitle.isEmpty()) heading += " : " + courseTitle;
            courseHeaderLabel.setText(heading);
            courseHeaderBox.setVisible(true);
            courseHeaderBox.setManaged(true);
            courseTypeFlair.setText("sessional".equals(courseType) ? "Sessional" : "Theory");
            creditsFlair.setText(courseCredits + " Credits");
        } else {
            courseHeaderBox.setVisible(false);
            courseHeaderBox.setManaged(false);
        }
    }

    private void buildSidebar() {
        sidebarButtonContainer.getChildren().clear();
        for (String label : COMMON_BUTTONS) sidebarButtonContainer.getChildren().add(createSidebarButton(label));
        String[] typeButtons = "sessional".equals(courseType) ? SESSIONAL_BUTTONS : THEORY_BUTTONS;
        for (String label : typeButtons) sidebarButtonContainer.getChildren().add(createSidebarButton(label));
    }

    private Button createSidebarButton(String label) {
        Button button = new Button(label);
        button.getStyleClass().add("nav-btn");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(event -> {
            setActiveButton(button);
            updateBreadcrumb(label);
            String fxmlPath = getFxmlPathForButton(label);
            if (fxmlPath != null) NavigationManager.switchScreen(fxmlPath);
        });
        return button;
    }

    private void setActiveButton(Button button) {
        if (activeButton != null) activeButton.getStyleClass().remove("nav-btn-active");
        button.getStyleClass().add("nav-btn-active");
        activeButton = button;
    }

    public void updateBreadcrumb(String section) {
        breadcrumbBar.getChildren().clear();
        breadcrumbBar.getChildren().add(createBreadcrumbSegment("Dashboard", this::handleBackToHome));

        if (courseCode != null && !courseCode.isEmpty()) {
            breadcrumbBar.getChildren().add(createBreadcrumbSeparator());
            breadcrumbBar.getChildren().add(createBreadcrumbSegment("My Courses", this::handleBackToHome));
            breadcrumbBar.getChildren().add(createBreadcrumbSeparator());
            breadcrumbBar.getChildren().add(createBreadcrumbSegment(courseCode, () -> {
                NavigationManager.switchScreen("common.fxml");
                updateBreadcrumb("Common");
                if (!sidebarButtonContainer.getChildren().isEmpty()) setActiveButton((Button) sidebarButtonContainer.getChildren().get(0));
            }));

            if (section != null && !section.isEmpty()) {
                String[] pathSegments = section.split(" / ");
                for (int i=0; i<pathSegments.length; i++) {
                    breadcrumbBar.getChildren().add(createBreadcrumbSeparator());
                    String currentWord = pathSegments[i];
                    if (i == pathSegments.length - 1) {
                        Label currentLabel = new Label(currentWord);
                        currentLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #2C3E50; -fx-font-weight: bold;");
                        breadcrumbBar.getChildren().add(currentLabel);
                    } else {
                        breadcrumbBar.getChildren().add(createBreadcrumbSegment(currentWord, () -> {
                            String fxmlPath = getFxmlPathForButton(currentWord);
                            if (fxmlPath != null) {
                                NavigationManager.switchScreen(fxmlPath);
                                updateBreadcrumb(currentWord);
                            }
                        }));
                    }
                }
            }
        }
    }

    private Label createBreadcrumbSegment(String text, Runnable onClick) {
        Label label = new Label(text);
        label.getStyleClass().add("breadcrumb-link");
        label.setOnMouseClicked(e -> onClick.run());
        return label;
    }

    private Label createBreadcrumbSeparator() {
        Label sep = new Label(" / ");
        sep.getStyleClass().add("breadcrumb-text");
        return sep;
    }

    @FXML
    private void handleBackToHome() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/syncron/views/home.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) mainBorderPane.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    private void openProfile() {
        try {
            ProfileController.viewingUser = null;
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/syncron/views/profile.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) mainBorderPane.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void forceSidebarSelection(String labelText) {
        for (javafx.scene.Node node : sidebarButtonContainer.getChildren()) {
            if (node instanceof Button btn) {
                if (btn.getText().equals(labelText)) {
                    setActiveButton(btn);
                    break;
                }
            }
        }
    }
}
package com.syncron.controllers;

import com.syncron.models.Course;
import com.syncron.models.Module;
import com.syncron.models.User;
import com.syncron.utils.DatabaseHandler;
import com.syncron.utils.NavigationManager;
import com.syncron.utils.ServerConfig;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HomeController {

    @FXML private Label topHeaderNameLabel;
    @FXML private Label welcomeLabel;

    @FXML private HBox profileBtn;

    @FXML private VBox courseCardContainer;
    @FXML private VBox urgentContainer;

    @FXML private Button detailsBtn;

    @FXML private Label semesterTitleLabel;
    @FXML private Label semesterStatusLabel;

    @FXML private Label termFinalLabel;
    @FXML private Label daysRemainingLabel;

    @FXML private TextField searchField;
    private List<Course> allCourses = new ArrayList<>();

    // --- NOTIFICATION FXML VARIABLES ---
    @FXML private Label notificationBell;
    @FXML private VBox notificationPanel;
    @FXML private VBox notificationList;

    @FXML private VBox announcementsFeedContainer;

    @FXML
    public void initialize() throws SQLException {
        // 1. Load the real courses once from the database into memory
        allCourses = fetchCoursesFromServer();
        renderCourses(allCourses);

        // 2. THE SEARCH ENGINE LISTENER
        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                filterCourses(newValue); // Instantly triggers when you type!
            });
        }

        // 3. Load the urgent deadlines
        loadUrgentDeadlines();

        // 4. Load current semester's data
        loadSemesterData();

        // 5. load announcement feed box
        loadAnnouncementsFeed();

        if (profileBtn != null) profileBtn.setOnMouseClicked(e -> openProfile());
        if (detailsBtn != null) detailsBtn.setOnAction(e -> openSemesterDetails());


        // nick name fetching from server
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser != null && topHeaderNameLabel != null && welcomeLabel != null) {
            topHeaderNameLabel.setText(currentUser.getName());

            // Fetch nickname from Cloud for the welcome message
            String displayName = currentUser.getName().split(" ")[0]; // Fallback: first name
            try {
                java.net.http.HttpClient nickClient = java.net.http.HttpClient.newHttpClient();
                java.net.http.HttpRequest nickReq = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create(ServerConfig.getBaseUrl() + "/api/users/" + currentUser.getId() + "/profile"))
                        .GET().build();
                java.net.http.HttpResponse<String> nickRes = nickClient.send(nickReq, java.net.http.HttpResponse.BodyHandlers.ofString());

                if (nickRes.statusCode() == 200) {
                    com.google.gson.Gson g = new com.google.gson.Gson();
                    java.util.Map<String, String> profileData = g.fromJson(nickRes.body(),
                            new com.google.gson.reflect.TypeToken<java.util.Map<String, String>>(){}.getType());

                    String nickname = profileData.getOrDefault("nickname", "");
                    if (nickname != null && !nickname.trim().isEmpty()) {
                        displayName = nickname;
                    }
                }
            } catch (Exception ignored) {}

            welcomeLabel.setText("Welcome Back, " + displayName + "!");
        }

    }

    private void loadAnnouncementsFeed() {
        if (announcementsFeedContainer == null) return;
        announcementsFeedContainer.getChildren().clear();

        javafx.scene.control.Label loading = new javafx.scene.control.Label("Loading announcements...");
        loading.setStyle("-fx-text-fill: #95A5A6; -fx-font-style: italic; -fx-font-size: 12px;");
        announcementsFeedContainer.getChildren().add(loading);

        new Thread(() -> {
            java.util.List<java.util.Map<String, String>> allAnnouncements = new java.util.ArrayList<>();

            try {
                java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();

                // Fetch the user's courses first
                String role = SessionManager.getCurrentUser().getRole();
                String name = SessionManager.getCurrentUser().getName().replace(" ", "%20");
                java.net.http.HttpRequest coursesReq = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create(ServerConfig.getBaseUrl() + "/api/dashboard/courses?role=" + role + "&name=" + name))
                        .GET().build();
                java.net.http.HttpResponse<String> coursesRes = client.send(coursesReq, java.net.http.HttpResponse.BodyHandlers.ofString());

                if (coursesRes.statusCode() == 200) {
                    com.google.gson.Gson gson = new com.google.gson.Gson();
                    java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<java.util.List<java.util.Map<String, String>>>(){}.getType();
                    java.util.List<java.util.Map<String, String>> courses = gson.fromJson(coursesRes.body(), listType);

                    // For each course, fetch announcements
                    for (java.util.Map<String, String> course : courses) {
                        String courseCode = course.get("courseCode");
                        String courseTitle = course.get("courseTitle");

                        if (courseCode == null || courseCode.equals("null")) continue;

                        try {
                            java.net.http.HttpRequest annReq = java.net.http.HttpRequest.newBuilder()
                                    .uri(java.net.URI.create(ServerConfig.getBaseUrl() + "/api/announcements/" + courseCode.replace(" ", "%20")))
                                    .GET().build();
                            java.net.http.HttpResponse<String> annRes = client.send(annReq, java.net.http.HttpResponse.BodyHandlers.ofString());

                            if (annRes.statusCode() == 200) {
                                java.util.List<java.util.Map<String, String>> posts = gson.fromJson(annRes.body(), listType);
                                for (java.util.Map<String, String> post : posts) {
                                    post.put("courseCode", courseCode);
                                    post.put("courseTitle", courseTitle != null ? courseTitle : "Course");
                                    allAnnouncements.add(post);
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }

            // Sort by timestamp (newest first) — timestamps are strings like "07 Apr 2026"
            // We'll just keep the order from the API (already newest-first per course)

            java.util.List<java.util.Map<String, String>> finalList = allAnnouncements;

            javafx.application.Platform.runLater(() -> {
                announcementsFeedContainer.getChildren().clear();

                if (finalList.isEmpty()) {
                    javafx.scene.control.Label empty = new javafx.scene.control.Label("No announcements yet.");
                    empty.setStyle("-fx-text-fill: #BDC3C7; -fx-font-style: italic; -fx-font-size: 12px;");
                    announcementsFeedContainer.getChildren().add(empty);
                    return;
                }

                // Show at most 20 announcements
                int limit = Math.min(finalList.size(), 20);
                for (int i = 0; i < limit; i++) {
                    java.util.Map<String, String> post = finalList.get(i);
                    announcementsFeedContainer.getChildren().add(buildAnnouncementCard(post));
                }
            });
        }).start();
    }

    private javafx.scene.layout.VBox buildAnnouncementCard(java.util.Map<String, String> post) {
        javafx.scene.layout.VBox card = new javafx.scene.layout.VBox(6);
        card.setPadding(new javafx.geometry.Insets(12, 14, 12, 14));

        String baseStyle = "-fx-background-color: #FFFFFF; -fx-background-radius: 10; -fx-border-color: #ECF0F1; -fx-border-radius: 10; -fx-border-width: 1; -fx-cursor: hand;";
        String hoverStyle = "-fx-background-color: #F0F7FF; -fx-background-radius: 10; -fx-border-color: #3498DB; -fx-border-radius: 10; -fx-border-width: 1; -fx-cursor: hand;";
        card.setStyle(baseStyle);
        card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
        card.setOnMouseExited(e -> card.setStyle(baseStyle));

        // Course badge
        String courseCode = post.get("courseCode");
        javafx.scene.control.Label courseBadge = new javafx.scene.control.Label(courseCode != null ? courseCode : "");
        courseBadge.setStyle("-fx-background-color: #EBF5FB; -fx-text-fill: #2980B9; -fx-font-weight: bold; -fx-padding: 2 8; -fx-background-radius: 10; -fx-font-size: 10px;");

        // Author + time
        String author = post.get("author") != null ? post.get("author") : "";
        String timestamp = post.get("timestamp") != null ? post.get("timestamp") : "";
        javafx.scene.control.Label metaLabel = new javafx.scene.control.Label(author + " • " + timestamp);
        metaLabel.setStyle("-fx-text-fill: #95A5A6; -fx-font-size: 10px;");

        javafx.scene.layout.HBox topRow = new javafx.scene.layout.HBox(8);
        topRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        topRow.getChildren().addAll(courseBadge, metaLabel);

        // Message preview (truncated, with smart-tag cleanup)
        String rawMsg = post.get("message") != null ? post.get("message") : "";
        String cleanMsg = rawMsg.replaceAll("\\[ASSESSMENT:\\d+:([^\\]]+)\\]", "\uD83D\uDCCC $1");
        if (cleanMsg.length() > 120) cleanMsg = cleanMsg.substring(0, 117) + "...";

        javafx.scene.control.Label msgLabel = new javafx.scene.control.Label(cleanMsg);
        msgLabel.setStyle("-fx-text-fill: #2C3E50; -fx-font-size: 12px; -fx-font-weight: bold;");
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(260);

        card.getChildren().addAll(topRow, msgLabel);

        // Click → navigate to the specific announcement inside the course
        card.setOnMouseClicked(e -> {
            String cc = post.get("courseCode");
            String ct = post.get("courseTitle");
            String annId = post.get("id");

            if (cc == null || annId == null) return;

            try {
                // Determine course type
                boolean isSessional = (ct != null && ct.toLowerCase().contains("sessional")) || (cc != null && cc.matches(".*[02468]$"));
                String courseType = isSessional ? "sessional" : "theory";
                String credits = isSessional ? "1.5" : "3.0";

                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/syncron/views/main_layout.fxml"));
                javafx.scene.Parent root = loader.load();

                SessionManager.setCurrentCourseCode(cc);
                SessionManager.setCurrentAnnouncementId(annId);

                MainController controller = loader.getController();
                controller.setCourseContext(cc, ct != null ? ct : "Course", courseType, credits);

                // Navigate to the specific announcement post
                com.syncron.utils.NavigationManager.switchScreen("view_announcement.fxml");
                controller.forceSidebarSelection("Announcements");
                controller.updateBreadcrumb("Announcements / View Post");

                javafx.stage.Stage stage = (javafx.stage.Stage) announcementsFeedContainer.getScene().getWindow();
                stage.getScene().setRoot(root);

            } catch (Exception ex) { ex.printStackTrace(); }
        });

        return card;
    }

    private java.util.List<Course> fetchCoursesFromServer() {
        java.util.List<Course> downloadedCourses = new java.util.ArrayList<>();
        try {
            System.out.println("⏳ Attempting to fetch courses from server...");
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();

            String role = SessionManager.getCurrentUser().getRole();
            String name = SessionManager.getCurrentUser().getName().replace(" ", "%20");
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(ServerConfig.getBaseUrl() + "/api/dashboard/courses?role=" + role + "&name=" + name))
                    .GET()
                    .build();

            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("✅ Server responded 200 OK! Parsing JSON...");

                com.google.gson.Gson gson = new com.google.gson.Gson();
                java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<java.util.List<java.util.Map<String, Object>>>(){}.getType();
                java.util.List<java.util.Map<String, Object>> courseData = gson.fromJson(response.body(), listType);

                for (java.util.Map<String, Object> data : courseData) {
                    String cCode = String.valueOf(data.get("courseCode"));
                    String cTitle = String.valueOf(data.get("courseTitle"));
                    String cType = String.valueOf(data.get("type"));

                    Object creditsObj = data.get("credits");
                    String cCredits;

                    if (creditsObj != null && !String.valueOf(creditsObj).equals("null") && !String.valueOf(creditsObj).isEmpty()) {
                        cCredits = String.valueOf(creditsObj);
                    } else {
                        boolean isSessional = cTitle.toLowerCase().contains("sessional") || cCode.matches(".*[02468]$");
                        cCredits = isSessional ? "1.5" : "3.0";
                        if (cType.equals("null")) cType = isSessional ? "Sessional" : "Theory";
                    }

                    Course c = new Course(cCode, cTitle, cCredits, cType);
                    downloadedCourses.add(c);
                }

                System.out.println("🎯 Successfully rendered " + downloadedCourses.size() + " courses to UI!");
            } else {
                System.out.println("❌ Server refused to send courses. HTTP Status: " + response.statusCode());
            }
        } catch (Exception e) {
            System.out.println("❌ CRASH during course fetch/parse:");
            e.printStackTrace();
        }
        return downloadedCourses;
    }

    private void loadSemesterData() {
        semesterTitleLabel.setText("Semester : Level I Term II");
        semesterStatusLabel.setText("PRESENT");
    }

    private void filterCourses(String query) {
        if (query == null || query.trim().isEmpty()) {
            renderCourses(allCourses);
            return;
        }

        String lowerQuery = query.toLowerCase();
        java.util.List<Course> filteredList = new java.util.ArrayList<>();

        for (Course course : allCourses) {
            if (course.getCourseCode().toLowerCase().contains(lowerQuery) ||
                    course.getCourseTitle().toLowerCase().contains(lowerQuery)) {
                filteredList.add(course);
            }
        }
        renderCourses(filteredList);
    }

    private void renderCourses(java.util.List<Course> coursesToRender) {
        courseCardContainer.getChildren().clear();

        if (coursesToRender.isEmpty()) {
            Label noMatch = new Label("No courses found matching your search.");
            noMatch.setStyle("-fx-text-fill: #7F8C8D; -fx-font-style: italic; -fx-padding: 10 0;");
            courseCardContainer.getChildren().add(noMatch);
            return;
        }

        for (Course course : coursesToRender) {
            HBox card = new HBox();
            card.setSpacing(10);
            card.setPadding(new Insets(18));
            card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 10; " +
                    "-fx-border-color: #E0D5C7; -fx-border-radius: 10; " +
                    "-fx-effect: dropshadow(three-pass-box, rgba(74,44,26,0.04), 4, 0, 0, 2); " +
                    "-fx-cursor: hand;");

            VBox infoBox = new VBox();
            Label codeLabel = new Label(course.getCourseCode());
            codeLabel.setStyle("-fx-font-family: 'Georgia', serif; -fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #4A2C1A;");

            Label titleLabel = new Label(course.getCourseTitle());
            titleLabel.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 13px; -fx-text-fill: #8C7A6B;");

            infoBox.getChildren().addAll(codeLabel, titleLabel);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label arrowLabel = new Label("➜");
            arrowLabel.setStyle("-fx-text-fill: #3498DB; -fx-font-size: 18px;");

            card.getChildren().addAll(infoBox, spacer, arrowLabel);

            card.setOnMouseEntered(e -> card.setStyle(card.getStyle() + "-fx-background-color: #F9FAFC;"));
            card.setOnMouseExited(e -> card.setStyle(card.getStyle().replace("-fx-background-color: #F9FAFC;", "-fx-background-color: #FFFFFF;")));

            card.setOnMouseClicked(event -> openCoursePortal(course));

            courseCardContainer.getChildren().add(card);
        }
    }

    private void openCoursePortal(Course course) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/syncron/views/main_layout.fxml"));
            Parent root = loader.load();

            SessionManager.setCurrentCourseCode(course.getCourseCode());

            MainController controller = loader.getController();
            controller.setCourseContext(course.getCourseCode(), course.getCourseTitle(), course.getType(), course.getCredits());

            Stage stage = (Stage) courseCardContainer.getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openSemesterDetails() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/syncron/views/semester_details.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) courseCardContainer.getScene().getWindow();
            stage.getScene().setRoot(root);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void openProfile() {
        try {
            ProfileController.viewingUser = null;
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/syncron/views/profile.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) profileBtn.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadUrgentDeadlines() {
        urgentContainer.getChildren().clear();
        urgentContainer.getStyleClass().clear();
        urgentContainer.getStyleClass().add("glow-box-red");

        Label headerLbl = new Label("🔥 Upcoming Deadlines");
        headerLbl.setStyle("-fx-text-fill: #C0392B; -fx-font-weight: bold; -fx-font-size: 15px; -fx-padding: 0 0 10 0;");
        urgentContainer.getChildren().add(headerLbl);

        new Thread(() -> {
            try {
                java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
                String role = SessionManager.getCurrentUser().getRole();
                String name = SessionManager.getCurrentUser().getName().replace(" ", "%20");

                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create("http://localhost:8080/api/dashboard/deadlines?role=" + role + "&name=" + name))
                        .GET().build();

                java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    com.google.gson.Gson gson = new com.google.gson.Gson();
                    java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<java.util.List<java.util.Map<String, String>>>(){}.getType();
                    java.util.List<java.util.Map<String, String>> tasks = gson.fromJson(response.body(), listType);

                    javafx.application.Platform.runLater(() -> {
                        if (tasks.isEmpty()) {
                            Label emptyMsg = new Label("No upcoming deadlines! ☕");
                            emptyMsg.setStyle("-fx-text-fill: #7F8C8D; -fx-font-style: italic; -fx-font-size: 12px;");
                            urgentContainer.getChildren().add(emptyMsg);
                            return;
                        }

                        for (java.util.Map<String, String> task : tasks) {
                            VBox card = new VBox(4);
                            card.setPadding(new Insets(10, 12, 10, 12));

                            // Type color
                            String type = task.get("type") != null ? task.get("type").toUpperCase() : "CT";
                            String cardBg = "#FFFFFF";
                            String accentColor = "#7F8C8D";
                            if ("CT".equals(type)) { cardBg = "#FFF8F0"; accentColor = "#D35400"; }
                            else if ("ASSIGNMENT".equals(type)) { cardBg = "#F0FFF4"; accentColor = "#27AE60"; }
                            else if ("OFFLINE".equals(type)) { cardBg = "#F8F0FF"; accentColor = "#8E44AD"; }
                            else if ("ONLINE".equals(type)) { cardBg = "#F0F8FF"; accentColor = "#2980B9"; }

                            String baseStyle = "-fx-background-color: " + cardBg + "; -fx-background-radius: 8; -fx-border-color: " + accentColor + "30; -fx-border-radius: 8; -fx-border-width: 1; -fx-cursor: hand;";
                            String hoverStyle = baseStyle + " -fx-effect: dropshadow(three-pass-box, " + accentColor + "40, 8, 0, 0, 2);";
                            card.setStyle(baseStyle);
                            card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
                            card.setOnMouseExited(e -> card.setStyle(baseStyle));

                            // Course code badge
                            String courseCode = task.get("courseCode") != null ? task.get("courseCode") : "";
                            Label courseBadge = new Label(courseCode);
                            courseBadge.setStyle("-fx-text-fill: " + accentColor + "; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-color: " + accentColor + "15; -fx-padding: 1 6; -fx-background-radius: 6;");

                            // Title
                            String title = task.get("title") != null ? task.get("title") : "Assessment";
                            Label titleLbl = new Label(title);
                            titleLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");
                            titleLbl.setWrapText(true);
                            titleLbl.setMaxWidth(200);

                            // Countdown
                            String dDate = task.get("deadlineDate");
                            String dTime = task.get("deadlineTime") != null ? task.get("deadlineTime") : "23:59";
                            String countdownText = "";
                            try {
                                java.time.LocalDate deadline = java.time.LocalDate.parse(dDate);
                                long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), deadline);
                                if (daysLeft == 0) countdownText = "Due today!";
                                else if (daysLeft == 1) countdownText = "Due tomorrow";
                                else countdownText = daysLeft + " days left";
                            } catch (Exception ignored) {
                                countdownText = dDate != null ? dDate : "TBD";
                            }

                            Label countdownLbl = new Label(countdownText + " • " + formatTimeShort(dTime));
                            String urgencyColor = "#95A5A6";
                            try {
                                long dl = java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), java.time.LocalDate.parse(dDate));
                                if (dl <= 1) urgencyColor = "#E74C3C";
                                else if (dl <= 3) urgencyColor = "#E67E22";
                            } catch (Exception ignored) {}
                            countdownLbl.setStyle("-fx-text-fill: " + urgencyColor + "; -fx-font-size: 11px; -fx-font-weight: bold;");

                            card.getChildren().addAll(courseBadge, titleLbl, countdownLbl);

                            // Click → navigate to the evaluation
                            final String evalId = task.get("id");
                            final String evalType = type;
                            final String evalCourseCode = courseCode;
                            final String courseTitle = task.get("courseTitle") != null ? task.get("courseTitle") : "";

                            card.setOnMouseClicked(e -> {
                                try {
                                    boolean isSessional = courseTitle.toLowerCase().contains("sessional") || evalCourseCode.matches(".*[02468]$");
                                    String cType = isSessional ? "sessional" : "theory";
                                    String credits = isSessional ? "1.5" : "3.0";

                                    javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/syncron/views/main_layout.fxml"));
                                    javafx.scene.Parent root = loader.load();

                                    SessionManager.setCurrentCourseCode(evalCourseCode);
                                    SessionManager.setCurrentEvaluationId(evalId);

                                    MainController controller = loader.getController();
                                    controller.setCourseContext(evalCourseCode, courseTitle, cType, credits);
                                    NavigationManager.switchScreen("evaluation_details.fxml");

                                    String parentTab = "Common";
                                    if ("CT".equals(evalType) || "ASSIGNMENT".equals(evalType)) parentTab = "CT and Assignments";
                                    else if ("ONLINE".equals(evalType)) parentTab = "Onlines";
                                    else if ("OFFLINE".equals(evalType)) parentTab = "Offlines";

                                    controller.forceSidebarSelection(parentTab);
                                    controller.updateBreadcrumb(parentTab + " / " + title);

                                    javafx.stage.Stage stage = (javafx.stage.Stage) urgentContainer.getScene().getWindow();
                                    stage.getScene().setRoot(root);
                                } catch (Exception ex) { ex.printStackTrace(); }
                            });

                            urgentContainer.getChildren().add(card);
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    Label err = new Label("Could not load deadlines.");
                    err.setStyle("-fx-text-fill: #E74C3C; -fx-font-size: 11px;");
                    urgentContainer.getChildren().add(err);
                });
            }
        }).start();
    }

    private String formatTimeShort(String time24) {
        if (time24 == null || time24.isEmpty()) return "";
        try {
            java.time.LocalTime t = java.time.LocalTime.parse(time24);
            return t.format(java.time.format.DateTimeFormatter.ofPattern("h:mm a"));
        } catch (Exception e) { return time24; }
    }
    // --- Opens a course's Common section with correct breadcrumb/sidebar ---
    private void openCourseCommon(String courseCode) {
        if (courseCode == null || courseCode.trim().isEmpty() || courseCode.equals("null")) return;

        Course targetCourse = null;
        for (Course c : allCourses) {
            if (c.getCourseCode().trim().equalsIgnoreCase(courseCode.trim())) {
                targetCourse = c;
                break;
            }
        }
        if (targetCourse == null) {
            targetCourse = new Course(courseCode, "Course", "3.0", "theory");
        }

        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/syncron/views/main_layout.fxml"));
            Parent root = loader.load();

            SessionManager.setCurrentCourseCode(targetCourse.getCourseCode());

            MainController controller = loader.getController();
            controller.setCourseContext(targetCourse.getCourseCode(), targetCourse.getCourseTitle(), targetCourse.getType(), targetCourse.getCredits());

            // setCourseContext already navigates to common.fxml and sets sidebar to "Common"

            Stage stage = (Stage) urgentContainer.getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 👇 THE FIX: Robust Course State Regeneration
    private void openEvaluationDirectly(String courseCode, String evaluationId, String assessmentType) {
        Course targetCourse = null;
        for (Course c : allCourses) {
            // Ignore strict case formatting
            if (c.getCourseCode().trim().equalsIgnoreCase(courseCode.trim())) {
                targetCourse = c;
                break;
            }
        }

        // Anti-Crash mechanism if the course was genuinely not found
        if (targetCourse == null) {
            targetCourse = new Course(courseCode, "Course", "3.0", "theory");
        }

        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/syncron/views/main_layout.fxml"));
            Parent root = loader.load();

            SessionManager.setCurrentCourseCode(targetCourse.getCourseCode());
            SessionManager.setCurrentEvaluationId(evaluationId);

            MainController controller = loader.getController();
            controller.setCourseContext(targetCourse.getCourseCode(), targetCourse.getCourseTitle(), targetCourse.getType(), targetCourse.getCredits());

            String parentTab = "Common";
            if ("CT".equalsIgnoreCase(assessmentType) || "ASSIGNMENT".equalsIgnoreCase(assessmentType)) {
                parentTab = "CT and Assignments";
            } else if ("ONLINE".equalsIgnoreCase(assessmentType)) {
                parentTab = "Onlines";
            } else if ("OFFLINE".equalsIgnoreCase(assessmentType)) {
                parentTab = "Offlines";
            }

            NavigationManager.switchScreen("evaluation_details.fxml");

            // Locks everything visually back in place
            controller.forceSidebarSelection(parentTab);
            controller.updateBreadcrumb(parentTab + " / Assessment Details");

            Stage stage = (Stage) urgentContainer.getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // ==========================================
    // --- NOTIFICATION UI ANIMATIONS ---
    // ==========================================
    @FXML
    private void glowBell() {
        if (notificationBell != null) {
            notificationBell.setStyle("-fx-font-size: 22px; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, #F1C40F, 15, 0.5, 0, 0);");
        }
    }

    @FXML
    private void unglowBell() {
        if (notificationBell != null) {
            notificationBell.setStyle("-fx-font-size: 22px; -fx-cursor: hand;");
        }
    }

    @FXML
    private void toggleNotifications() {
        if (notificationPanel == null) return;

        boolean isVisible = notificationPanel.isVisible();
        notificationPanel.setVisible(!isVisible);

        if (!isVisible) {
            notificationPanel.toFront();
            fetchRealNotifications();
        }
    }

    private void fetchRealNotifications() {
        notificationList.getChildren().clear();
        Label loading = new Label("Fetching latest updates...");
        loading.setStyle("-fx-padding: 15; -fx-text-fill: #7F8C8D; -fx-font-style: italic;");
        notificationList.getChildren().add(loading);

        new Thread(() -> {
            try {
                java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
                java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create(ServerConfig.getBaseUrl() + "/api/notifications"))
                        .GET().build();

                java.net.http.HttpResponse<String> res = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());

                if (res.statusCode() == 200) {
                    com.google.gson.Gson gson = new com.google.gson.Gson();
                    java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<java.util.List<java.util.Map<String, String>>>(){}.getType();
                    java.util.List<java.util.Map<String, String>> announcements = gson.fromJson(res.body(), listType);

                    javafx.application.Platform.runLater(() -> renderNotificationRows(announcements));
                } else {
                    javafx.application.Platform.runLater(this::loadFallbackNotifications);
                }
            } catch (Exception e) {
                javafx.application.Platform.runLater(this::loadFallbackNotifications);
            }
        }).start();
    }

    private void loadFallbackNotifications() {
        java.util.List<java.util.Map<String, String>> mockData = new java.util.ArrayList<>();
        mockData.add(java.util.Map.of("courseCode", "CSE 108", "message", "Project Evaluation (ASAN) posted."));
        mockData.add(java.util.Map.of("courseCode", "CSE 105", "message", "Assignment 3 deadline extended!"));
        mockData.add(java.util.Map.of("courseCode", "MATH 143", "message", "Linear Algebra mid-term syllabus updated."));

        renderNotificationRows(mockData);
    }

    private void renderNotificationRows(java.util.List<java.util.Map<String, String>> announcements) {
        notificationList.getChildren().clear();

        if (announcements.isEmpty()) {
            Label empty = new Label("No new notifications.");
            empty.setStyle("-fx-padding: 15; -fx-text-fill: #BDC3C7;");
            notificationList.getChildren().add(empty);
            return;
        }

        for (java.util.Map<String, String> ann : announcements) {
            String cCode = ann.get("courseCode");
            String msg = ann.get("message");

            VBox row = new VBox(5);
            row.setStyle("-fx-padding: 12; -fx-border-color: #ECF0F1; -fx-border-width: 0 0 1 0; -fx-cursor: hand; -fx-background-color: white;");

            row.setOnMouseEntered(e -> row.setStyle("-fx-padding: 12; -fx-border-color: #ECF0F1; -fx-border-width: 0 0 1 0; -fx-cursor: hand; -fx-background-color: #F4F6F6;"));
            row.setOnMouseExited(e -> row.setStyle("-fx-padding: 12; -fx-border-color: #ECF0F1; -fx-border-width: 0 0 1 0; -fx-cursor: hand; -fx-background-color: white;"));

            Label title = new Label(cCode + " Announcement");
            title.setStyle("-fx-font-weight: bold; -fx-text-fill: #2C3E50;");
            Label desc = new Label(msg);
            desc.setStyle("-fx-text-fill: #7F8C8D; -fx-font-size: 12px;");

            row.getChildren().addAll(title, desc);

            row.setOnMouseClicked(e -> {
                try {
                    SessionManager.setCurrentCourseCode(cCode);
                    javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/syncron/views/main_layout.fxml"));
                    javafx.scene.Parent root = loader.load();

                    MainController controller = loader.getController();

                    String cTitle = "Course"; String cType = "Theory"; String cCredits = "3.0";
                    for(Course c : allCourses) {
                        if(c.getCourseCode().equalsIgnoreCase(cCode)) {
                            cTitle = c.getCourseTitle(); cType = c.getType(); cCredits = c.getCredits();
                            break;
                        }
                    }
                    controller.setCourseContext(cCode, cTitle, cType, cCredits);

                    try { NavigationManager.switchScreen("announcements.fxml"); } catch (Exception ignored) {}
                    controller.forceSidebarSelection("Announcements");
                    controller.updateBreadcrumb("Announcements");

                    javafx.stage.Stage stage = (javafx.stage.Stage) notificationBell.getScene().getWindow();
                    stage.getScene().setRoot(root);
                } catch (Exception ex) { ex.printStackTrace(); }
            });

            notificationList.getChildren().add(row);
        }
    }

    @FXML
    private void openCalendar() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/syncron/views/calendar.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) courseCardContainer.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
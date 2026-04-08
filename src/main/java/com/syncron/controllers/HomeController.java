package com.syncron.controllers;

import com.syncron.models.Course;
import com.syncron.models.Module;
import com.syncron.models.User;
import com.syncron.utils.DatabaseHandler;
import com.syncron.utils.NavigationManager;
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

        User currentUser = SessionManager.getCurrentUser();
        if (currentUser != null && topHeaderNameLabel != null && welcomeLabel != null) {
            topHeaderNameLabel.setText(currentUser.getName());
            String FirstName = currentUser.getName().split(" ")[0];
            welcomeLabel.setText("Welcome Back, " + FirstName + "!");
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
                        .uri(java.net.URI.create("http://localhost:8080/api/dashboard/courses?role=" + role + "&name=" + name))
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
                                    .uri(java.net.URI.create("http://localhost:8080/api/announcements/" + courseCode.replace(" ", "%20")))
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
                    .uri(java.net.URI.create("http://localhost:8080/api/dashboard/courses?role=" + role + "&name=" + name))
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

        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();

            String role = SessionManager.getCurrentUser().getRole();
            String name = SessionManager.getCurrentUser().getName().replace(" ", "%20");
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://localhost:8080/api/dashboard/deadlines?role=" + role + "&name=" + name))
                    .GET()
                    .build();

            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                com.google.gson.Gson gson = new com.google.gson.Gson();
                java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<java.util.List<java.util.Map<String, String>>>(){}.getType();
                java.util.List<java.util.Map<String, String>> urgentTasks = gson.fromJson(response.body(), listType);

                if (urgentTasks.isEmpty()) {
                    Label emptyMsg = new Label("No dues left. Relax! ☕");
                    emptyMsg.setStyle("-fx-text-fill: #7F8C8D; -fx-font-style: italic;");
                    urgentContainer.getChildren().add(emptyMsg);
                    return;
                }

                for (java.util.Map<String, String> task : urgentTasks) {

                    // --- Time calculation ---
                    String dDate = task.get("deadlineDate") != null ? task.get("deadlineDate") : task.get("dueDate");
                    String dTime = task.get("deadlineTime") != null ? task.get("deadlineTime") : task.get("dueTime");
                    String dueText = "Due: " + dDate;
                    boolean isPassed = false;

                    try {
                        java.time.LocalDateTime now = java.time.LocalDateTime.now();
                        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                        java.time.LocalDateTime deadline = java.time.LocalDateTime.parse(dDate + " " + dTime, formatter);

                        long minutesLeft = java.time.temporal.ChronoUnit.MINUTES.between(now, deadline);
                        long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(now, deadline);
                        long hoursLeft = java.time.temporal.ChronoUnit.HOURS.between(now, deadline) % 24;

                        java.time.format.DateTimeFormatter niceDate = java.time.format.DateTimeFormatter.ofPattern("dd MMMM yyyy");

                        if (minutesLeft < 0) {
                            isPassed = true;
                        } else if (daysLeft > 0) {
                            dueText = deadline.format(niceDate) + " (" + daysLeft + "d " + hoursLeft + "h left)";
                        } else if (hoursLeft > 0) {
                            long minsLeft = java.time.temporal.ChronoUnit.MINUTES.between(now, deadline) % 60;
                            dueText = "Today (" + hoursLeft + "h " + minsLeft + "m left)";
                        } else {
                            long minsLeft = java.time.temporal.ChronoUnit.MINUTES.between(now, deadline);
                            dueText = "⚡ Due in " + minsLeft + " minutes!";
                        }
                    } catch (Exception ignored) {}

                    if (isPassed) continue;

                    // --- Build the card ---
                    VBox taskBox = new VBox(5);
                    taskBox.setStyle("-fx-padding: 10 12; -fx-background-color: #FFFFFF; -fx-background-radius: 8; -fx-border-color: #F5D5D5; -fx-border-radius: 8; -fx-border-width: 1;");

                    // Row 1: Course name (clickable blue link → takes to course Common tab)
                    String courseCode = task.get("courseCode");
                    String courseTitle = task.get("courseTitle");

                    // Build display text for the course badge
                    String courseDisplay = (courseCode != null && !courseCode.equals("null")) ? courseCode : "";
                    if (courseTitle != null && !courseTitle.equals("null") && !courseTitle.isEmpty()) {
                        courseDisplay += " — " + courseTitle;
                    }

                    if (!courseDisplay.isEmpty()) {
                        Label courseLink = new Label(courseDisplay);
                        courseLink.setStyle("-fx-text-fill: #2980B9; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand; -fx-underline: false;");
                        courseLink.setOnMouseEntered(e -> courseLink.setStyle("-fx-text-fill: #3498DB; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand; -fx-underline: true;"));
                        courseLink.setOnMouseExited(e -> courseLink.setStyle("-fx-text-fill: #2980B9; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand; -fx-underline: false;"));
                        courseLink.setWrapText(true);

                        // Click → open the course's Common section
                        final String fCourseCode = courseCode;
                        courseLink.setOnMouseClicked(e -> openCourseCommon(fCourseCode));

                        taskBox.getChildren().add(courseLink);
                    }

                    // Row 2: Assessment title (clickable → takes to evaluation details)
                    String assessmentType = task.get("type");
                    String typeEmoji = "📝";
                    if ("CT".equalsIgnoreCase(assessmentType)) typeEmoji = "📋";
                    else if ("ASSIGNMENT".equalsIgnoreCase(assessmentType)) typeEmoji = "📎";
                    else if ("OFFLINE".equalsIgnoreCase(assessmentType)) typeEmoji = "💾";
                    else if ("ONLINE".equalsIgnoreCase(assessmentType)) typeEmoji = "🖥";

                    Label titleLabel = new Label(typeEmoji + " " + task.get("title"));
                    titleLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #2C3E50; -fx-cursor: hand;");
                    titleLabel.setWrapText(true);
                    titleLabel.setOnMouseEntered(e -> titleLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #D35400; -fx-cursor: hand; -fx-underline: true;"));
                    titleLabel.setOnMouseExited(e -> titleLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #2C3E50; -fx-cursor: hand;"));

                    // Click → open evaluation details directly
                    final String fCourseCode2 = courseCode;
                    titleLabel.setOnMouseClicked(e -> {
                        String cc = fCourseCode2;
                        if (cc == null || cc.trim().isEmpty() || cc.equals("null")) {
                            String taskTitle = task.get("title");
                            if (taskTitle != null && taskTitle.contains("-")) {
                                cc = taskTitle.split("-")[0].trim();
                            } else {
                                java.util.regex.Matcher m = java.util.regex.Pattern.compile("([A-Za-z]+\\s*\\d{3})").matcher(taskTitle != null ? taskTitle : "");
                                if (m.find()) cc = m.group(1);
                                else cc = taskTitle;
                            }
                        }
                        openEvaluationDirectly(cc, task.get("id"), task.get("type"));
                    });

                    // Row 3: Type badge + Time remaining
                    javafx.scene.layout.HBox metaRow = new javafx.scene.layout.HBox(8);
                    metaRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                    Label typeBadge = new Label(assessmentType != null ? assessmentType : "TASK");
                    String badgeColor = "#7F8C8D";
                    if ("CT".equalsIgnoreCase(assessmentType)) badgeColor = "#E67E22";
                    else if ("ASSIGNMENT".equalsIgnoreCase(assessmentType)) badgeColor = "#27AE60";
                    else if ("OFFLINE".equalsIgnoreCase(assessmentType)) badgeColor = "#8E44AD";
                    else if ("ONLINE".equalsIgnoreCase(assessmentType)) badgeColor = "#2980B9";

                    typeBadge.setStyle("-fx-background-color: " + badgeColor + "; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 2 8; -fx-background-radius: 10;");

                    Label dateLabel = new Label(dueText);
                    dateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #E74C3C; -fx-font-weight: bold;");

                    metaRow.getChildren().addAll(typeBadge, dateLabel);

                    taskBox.getChildren().addAll(titleLabel, metaRow);

                    // Hover effect on the whole card
                    taskBox.setOnMouseEntered(e -> taskBox.setStyle("-fx-padding: 10 12; -fx-background-color: #FEF5F5; -fx-background-radius: 8; -fx-border-color: #E74C3C; -fx-border-radius: 8; -fx-border-width: 1;"));
                    taskBox.setOnMouseExited(e -> taskBox.setStyle("-fx-padding: 10 12; -fx-background-color: #FFFFFF; -fx-background-radius: 8; -fx-border-color: #F5D5D5; -fx-border-radius: 8; -fx-border-width: 1;"));

                    urgentContainer.getChildren().add(taskBox);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
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
                        .uri(java.net.URI.create("http://localhost:8080/api/notifications"))
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
}
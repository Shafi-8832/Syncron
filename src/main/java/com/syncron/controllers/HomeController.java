package com.syncron.controllers;

import com.syncron.models.Course;
import com.syncron.models.Module;
import com.syncron.models.User;
import com.syncron.utils.DatabaseHandler;
import com.syncron.utils.NavigationManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
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

    @FXML
    public void initialize() throws SQLException {
        // 1. Load the real courses once from the database into memory
        allCourses = fetchCoursesFromServer();
        renderCourses(allCourses);

        // 2. 👉 THE SEARCH ENGINE LISTENER
        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                filterCourses(newValue); // Instantly triggers when you type!
            });
        }

        // 3. Load the urgent deadlines
        loadUrgentDeadlines();

        // 4. Load current semester's data
        loadSemesterData();

        if (profileBtn != null) profileBtn.setOnMouseClicked(e -> openProfile());
        if (detailsBtn != null) detailsBtn.setOnAction(e -> openSemesterDetails());

        User currentUser = SessionManager.getCurrentUser();
        if (currentUser != null && topHeaderNameLabel != null && welcomeLabel != null) {
            topHeaderNameLabel.setText(currentUser.getName());
            String FirstName = currentUser.getName().split(" ")[0];
            welcomeLabel.setText("Welcome Back, " + FirstName + "!");
        }
    }

    private java.util.List<Course> fetchCoursesFromServer() {
        java.util.List<Course> downloadedCourses = new java.util.ArrayList<>();
        try {
            System.out.println("⏳ Attempting to fetch courses from server...");
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://localhost:8080/api/courses"))
                    .GET()
                    .build();

            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("✅ Server responded 200 OK! Parsing JSON...");

                com.google.gson.Gson gson = new com.google.gson.Gson();
                // UPGRADE: Changed String, String to String, Object to prevent Number format crashes!
                java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<java.util.List<java.util.Map<String, Object>>>(){}.getType();
                java.util.List<java.util.Map<String, Object>> courseData = gson.fromJson(response.body(), listType);

                for (java.util.Map<String, Object> data : courseData) {
                    // UPGRADE: Safely force everything to a String so the Course constructor doesn't panic
                    String cCode = String.valueOf(data.get("courseCode"));
                    String cTitle = String.valueOf(data.get("courseTitle"));
                    String cType = String.valueOf(data.get("type"));

                    // Handle numbers like 3.0 gracefully without decimal trailing if needed, or just pass as string
                    String cCredits = String.valueOf(data.get("credits"));

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
        // will be updated later
        semesterTitleLabel.setText("Semester : Level 1 Term 2");
        semesterStatusLabel.setText("PRESENT");
        termFinalLabel.setText("📅 Term Final: 15 August 2026");
        daysRemainingLabel.setText("⏳ Days Remaining: 145");
    }

    private void filterCourses(String query) {
        // If the search bar is empty, show everything!
        if (query == null || query.trim().isEmpty()) {
            renderCourses(allCourses);
            return;
        }

        String lowerQuery = query.toLowerCase();
        java.util.List<Course> filteredList = new java.util.ArrayList<>();

        // Search through course codes AND course titles
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
            // Crisp white card to pop against the light-blue background
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
            arrowLabel.setStyle("-fx-text-fill: #3498DB; -fx-font-size: 18px;"); // Beautiful blue arrow to match the theme

            card.getChildren().addAll(infoBox, spacer, arrowLabel);

            card.setOnMouseEntered(e -> card.setStyle(card.getStyle() + "-fx-background-color: #F9FAFC;"));
            card.setOnMouseExited(e -> card.setStyle(card.getStyle().replace("-fx-background-color: #F9FAFC;", "-fx-background-color: #FFFFFF;")));

            card.setOnMouseClicked(event -> openCoursePortal(course));

            courseCardContainer.getChildren().add(card);
        }
    }

    // pass the full Course object to grab its real Type and Credits
    private void openCoursePortal(Course course) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/syncron/views/main_layout.fxml"));
            Parent root = loader.load();

            // Save the context so the inner tabs (Common, Participants) know where we are!
            SessionManager.setCurrentCourseCode(course.getCourseCode());

            MainController controller = loader.getController();
            // Pass the REAL type and credits from the database!
            controller.setCourseContext(course.getCourseCode(), course.getCourseTitle(), course.getType(), course.getCredits());

            Stage stage = (Stage) courseCardContainer.getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 3. The Navigation Down here
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
            // Tell the ProfileController to load the logged-in user
            ProfileController.viewingUser = null;

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/syncron/views/profile.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) profileBtn.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 4. Load urgent deadlines with this method
    private void loadUrgentDeadlines() {
        urgentContainer.getChildren().clear();

        // Apply the new glowing red CSS
        urgentContainer.getStyleClass().clear();
        urgentContainer.getStyleClass().add("glow-box-red");

        Label headerLbl = new Label("🔥 Upcoming Deadlines");
        headerLbl.setStyle("-fx-text-fill: #C0392B; -fx-font-weight: bold; -fx-font-size: 15px; -fx-padding: 0 0 10 0;");
        urgentContainer.getChildren().add(headerLbl);

        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://localhost:8080/api/dashboard/urgent"))
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
                    VBox taskBox = new VBox(4); // Slightly more spacing
                    taskBox.setStyle("-fx-padding: 8 0; -fx-cursor: hand;");

                    Label titleLabel = new Label("• " + task.get("title"));
                    titleLabel.getStyleClass().add("clean-link");
                    // BIGGER, PREMIUM FONT
                    titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
                    titleLabel.setWrapText(true);

                    // --- TIME CALCULATION ENGINE ---
                    String dueText = "Due: " + task.get("dueDate");
                    try {
                        java.time.LocalDateTime now = java.time.LocalDateTime.now();
                        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                        java.time.LocalDateTime deadline = java.time.LocalDateTime.parse(task.get("dueDate") + " " + task.get("dueTime"), formatter);

                        long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(now, deadline);
                        long hoursLeft = java.time.temporal.ChronoUnit.HOURS.between(now, deadline) % 24;

                        java.time.format.DateTimeFormatter niceDate = java.time.format.DateTimeFormatter.ofPattern("dd MMMM yyyy");

                        if (daysLeft > 0) {
                            dueText = deadline.format(niceDate) + " (" + daysLeft + " days Left)";
                        } else if (hoursLeft > 0) {
                            dueText = "Today (" + hoursLeft + " hours Left)";
                        } else {
                            dueText = "Due Very Soon!";
                        }
                    } catch (Exception ignored) {}

                    Label dateLabel = new Label(dueText);
                    // 👉 BIGGER FONT FOR DATES
                    dateLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #E74C3C; -fx-padding: 0 0 0 15; -fx-font-weight: bold;");

                    taskBox.getChildren().addAll(titleLabel, dateLabel);

                    // THE FIX: SAFE CLICK ROUTING
// CLICK ROUTING
                    taskBox.setOnMouseClicked(e -> {
                        String courseCode = task.get("title").split(" - ")[0].trim();
                        // Pass the TYPE to the teleporter
                        openEvaluationDirectly(courseCode, task.get("id"), task.get("type"));
                    });

                    urgentContainer.getChildren().add(taskBox);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // THE SOLID TELEPORTER
    private void openEvaluationDirectly(String courseCode, String evaluationId, String assessmentType) {
        Course targetCourse = null;
        for (Course c : allCourses) {
            if (c.getCourseCode().equalsIgnoreCase(courseCode)) {
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
            SessionManager.setCurrentEvaluationId(evaluationId);

            MainController controller = loader.getController();
            controller.setCourseContext(targetCourse.getCourseCode(), targetCourse.getCourseTitle(), targetCourse.getType(), targetCourse.getCredits());

            // 1. Calculate the correct parent Tab based on Assessment Type
            String parentTab = "Common";
            if ("CT".equalsIgnoreCase(assessmentType) || "ASSIGNMENT".equalsIgnoreCase(assessmentType)) {
                parentTab = "CT and Assignments";
            } else if ("ONLINE".equalsIgnoreCase(assessmentType)) {
                parentTab = "Onlines";
            } else if ("OFFLINE".equalsIgnoreCase(assessmentType)) {
                parentTab = "Offlines";
            }

            // 2. Load the Details Page
            NavigationManager.switchScreen("evaluation_details.fxml");

            // 3. Fix the UI: Sync Sidebar and Interactive Breadcrumbs!
            controller.forceSidebarSelection(parentTab);
            controller.updateBreadcrumb(parentTab + " / Assessment Details");

            Stage stage = (Stage) urgentContainer.getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
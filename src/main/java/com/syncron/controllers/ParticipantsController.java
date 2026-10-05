package com.syncron.controllers;

import com.syncron.models.Student;
import com.syncron.models.Teacher;
import com.syncron.models.User;
import com.syncron.utils.NavigationManager;
import com.syncron.utils.ServerConfig;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ParticipantsController {

    @FXML private TextField searchBox;
    @FXML private ComboBox<String> roleFilter;
    @FXML private VBox participantsContainer;

    private List<User> allParticipants;

    @FXML
    public void initialize() {
        roleFilter.setItems(FXCollections.observableArrayList("All", "Teachers", "Students"));
        roleFilter.setValue("All");

        String currentCourseCode = SessionManager.getCurrentCourseCode();
        allParticipants = fetchParticipantsFromServer(currentCourseCode);

        renderParticipantsList(allParticipants);

        roleFilter.setOnAction(event -> applyFilters());
        searchBox.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
    }

    private void renderParticipantsList(List<User> participants) {
        participantsContainer.getChildren().clear();

        if (participants.isEmpty()) {
            Label empty = new Label("No participants found.");
            empty.setStyle("-fx-text-fill: #95A5A6; -fx-font-style: italic; -fx-padding: 20;");
            participantsContainer.getChildren().add(empty);
            return;
        }

        // Count header
        long teacherCount = participants.stream().filter(u -> "TEACHER".equalsIgnoreCase(u.getRole())).count();
        long studentCount = participants.size() - teacherCount;
        Label countLabel = new Label(teacherCount + " teacher" + (teacherCount != 1 ? "s" : "") + ", " + studentCount + " student" + (studentCount != 1 ? "s" : ""));
        countLabel.setStyle("-fx-text-fill: #95A5A6; -fx-font-size: 12px; -fx-padding: 0 0 5 5;");
        participantsContainer.getChildren().add(countLabel);

        for (User u : participants) {
            HBox listItem = new HBox(15);
            listItem.setAlignment(Pos.CENTER_LEFT);
            listItem.setPadding(new Insets(12, 20, 12, 20));

            boolean isTeacher = "TEACHER".equalsIgnoreCase(u.getRole());

            // Hover effect
            String defaultStyle = "-fx-background-color: transparent; -fx-background-radius: 8; -fx-cursor: hand;";
            String hoverStyle = "-fx-background-color: " + (isTeacher ? "#FEF5E7" : "#EBF5FB") + "; -fx-background-radius: 8; -fx-cursor: hand;";
            listItem.setStyle(defaultStyle);
            listItem.setOnMouseEntered(e -> listItem.setStyle(hoverStyle));
            listItem.setOnMouseExited(e -> listItem.setStyle(defaultStyle));

            // Avatar circle
            Circle avatar = new Circle(18, javafx.scene.paint.Color.web(isTeacher ? "#D35400" : "#3498DB"));
            Label avatarInitial = new Label(u.getName() != null && !u.getName().isEmpty() ? u.getName().substring(0, 1).toUpperCase() : "?");
            avatarInitial.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
            StackPane avatarPane = new StackPane(avatar, avatarInitial);

            // Name & details
            VBox details = new VBox(2);
            Label name = new Label(u.getName());
            name.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

            String subText = u.getId();
            if (!isTeacher && u instanceof Student) {
                Student s = (Student) u;
                String subsec = s.getSubsection();
                if (subsec != null && !subsec.equals("--")) {
                    subText += " • " + subsec;
                }
            }
            Label id = new Label(subText);
            id.setStyle("-fx-font-size: 11px; -fx-text-fill: #95A5A6;");
            details.getChildren().addAll(name, id);

            // Spacer
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            // Role badge
            Label badge = new Label(isTeacher ? "Lecturer" : "Student");
            badge.setStyle("-fx-background-color: " + (isTeacher ? "#FDEBD0" : "#EBF5FB") +
                    "; -fx-text-fill: " + (isTeacher ? "#D35400" : "#2980B9") +
                    "; -fx-padding: 4 12; -fx-background-radius: 14; -fx-font-size: 11px; -fx-font-weight: bold;");

            // Arrow
            Label arrow = new Label("→");
            arrow.setStyle("-fx-text-fill: #BDC3C7; -fx-font-size: 14px;");

            listItem.getChildren().addAll(avatarPane, details, spacer, badge, arrow);

            // --- THE FIX: Route to public_profile.fxml.fxml instead of private profile.fxml ---
            listItem.setOnMouseClicked(event -> {
                SessionManager.setViewProfileId(u.getId());
                NavigationManager.switchScreen("public_profile.fxml");
                NavigationManager.updateGlobalBreadcrumb("Participants / Profile");
            });

            participantsContainer.getChildren().add(listItem);
        }

        // Divider between teachers and students
        // (Already sorted by role from getCourseParticipants — teachers first)
    }

    private void applyFilters() {
        String selectedRole = roleFilter.getValue();
        String query = searchBox.getText() == null ? "" : searchBox.getText().trim().toLowerCase();

        List<User> filtered = new ArrayList<>();
        for (User user : allParticipants) {
            boolean roleMatches = "All".equals(selectedRole)
                    || ("Teachers".equals(selectedRole) && "TEACHER".equalsIgnoreCase(user.getRole()))
                    || ("Students".equals(selectedRole) && "STUDENT".equalsIgnoreCase(user.getRole()));

            boolean searchMatches = query.isEmpty()
                    || (user.getName() != null && user.getName().toLowerCase().contains(query))
                    || (user.getId() != null && user.getId().toLowerCase().contains(query));

            if (roleMatches && searchMatches) {
                filtered.add(user);
            }
        }

        renderParticipantsList(filtered);
    }

    private List<User> fetchParticipantsFromServer(String courseCode) {
        List<User> participants = new ArrayList<>();
        if (courseCode == null || courseCode.isBlank()) return participants;

        try {
            HttpClient client = HttpClient.newHttpClient();
            Gson gson = new Gson();
            Type mapListType = new TypeToken<List<Map<String, Object>>>() {}.getType();
            String safeCourseCode = URLEncoder.encode(courseCode, StandardCharsets.UTF_8);

            HttpRequest teachersReq = HttpRequest.newBuilder()
                    .uri(URI.create(ServerConfig.getBaseUrl() + "/api/courses/" + safeCourseCode + "/teachers"))
                    .GET()
                    .build();
            HttpResponse<String> teachersRes = client.send(teachersReq, HttpResponse.BodyHandlers.ofString());

            Map<String, User> mergedUsers = new LinkedHashMap<>();
            if (teachersRes.statusCode() == 200) {
                List<Map<String, Object>> teacherRows = gson.fromJson(teachersRes.body(), mapListType);
                for (Map<String, Object> row : teacherRows) {
                    Teacher t = new Teacher(
                            getText(row, "id"),
                            getText(row, "name"),
                            getText(row, "email"),
                            "",
                            getTextOrDefault(row, "designation", "Lecturer")
                    );
                    mergedUsers.put(t.getId(), t);
                }
            }

            HttpRequest usersReq = HttpRequest.newBuilder()
                    .uri(URI.create(ServerConfig.getBaseUrl() + "/api/admin/all-users"))
                    .GET()
                    .build();
            HttpResponse<String> usersRes = client.send(usersReq, HttpResponse.BodyHandlers.ofString());
            if (usersRes.statusCode() == 200) {
                List<Map<String, Object>> rows = gson.fromJson(usersRes.body(), mapListType);
                for (Map<String, Object> row : rows) {
                    String role = getText(row, "role");
                    if (!"STUDENT".equalsIgnoreCase(role)) continue;

                    Student s = new Student(
                            getText(row, "id"),
                            getText(row, "name"),
                            getText(row, "email"),
                            "",
                            false
                    );
                    mergedUsers.put(s.getId(), s);
                }
            }

            participants.addAll(mergedUsers.values());
            participants.sort(Comparator.comparing((User u) -> !"TEACHER".equalsIgnoreCase(u.getRole()))
                    .thenComparing(User::getName, String.CASE_INSENSITIVE_ORDER));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return participants;
    }

    private String getText(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private String getTextOrDefault(Map<String, Object> row, String key, String fallback) {
        String value = getText(row, key);
        return value.isBlank() || "null".equalsIgnoreCase(value) ? fallback : value;
    }
}
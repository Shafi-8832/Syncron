package com.syncron.controllers;

import com.syncron.utils.NavigationManager;
import com.syncron.utils.ServerConfig;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

public class WeeklyTimelineController {

    @FXML private VBox timelineContainer;
    @FXML private HBox labDayBox;
    @FXML private ComboBox<String> labDayCombo;

    private static final LocalDate SEMESTER_START = LocalDate.of(2026, 1, 15);
    private String currentLabDay = "Thursday";
    private boolean isSessionalCourse = false;
    private boolean isTeacher = false;

    @FXML
    public void initialize() {
        String currentCourse = SessionManager.getCurrentCourseCode();
        if (currentCourse == null || currentCourse.isEmpty()) {
            NavigationManager.switchScreen("home.fxml");
            return;
        }

        isTeacher = "TEACHER".equals(SessionManager.getCurrentUser().getRole());

        // Determine course type
        String courseType = SessionManager.getCurrentCourseType();
        isSessionalCourse = "sessional".equalsIgnoreCase(courseType);

        // Lab Day Editor — only for sessional courses and teachers
        if (isTeacher && isSessionalCourse) {
            labDayBox.setVisible(true);
            labDayBox.setManaged(true);
            labDayCombo.getItems().setAll("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday");
            labDayCombo.setOnAction(e -> {
                String selected = labDayCombo.getValue();
                if (selected != null) updateLabDayToCloud(currentCourse, selected);
            });
        } else {
            labDayBox.setVisible(false);
            labDayBox.setManaged(false);
        }

        fetchDataAndRender(currentCourse);
    }

    private void updateLabDayToCloud(String courseCode, String labDay) {
        try {
            String json = String.format("{\"courseCode\":\"%s\", \"labDay\":\"%s\"}", courseCode, labDay);
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(ServerConfig.getBaseUrl() + "/api/timeline/settings"))
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(json))
                    .build();
            client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            fetchDataAndRender(courseCode);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void fetchDataAndRender(String currentCourse) {
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            com.google.gson.Gson gson = new com.google.gson.Gson();

            java.net.http.HttpRequest req1 = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(ServerConfig.getBaseUrl() + "/api/timeline/" + currentCourse.replace(" ", "%20")))
                    .GET().build();
            String res1 = client.send(req1, java.net.http.HttpResponse.BodyHandlers.ofString()).body();

            java.lang.reflect.Type timelineType = new com.google.gson.reflect.TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> timelineData = gson.fromJson(res1, timelineType);

            currentLabDay = String.valueOf(timelineData.getOrDefault("labDay", "Thursday"));

            if (isSessionalCourse) {
                labDayCombo.setOnAction(null);
                labDayCombo.setValue(currentLabDay);
                labDayCombo.setOnAction(e -> updateLabDayToCloud(currentCourse, labDayCombo.getValue()));
            }

            List<Map<String, Object>> sections = (List<Map<String, Object>>) timelineData.get("sections");

            java.net.http.HttpRequest req2 = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(ServerConfig.getBaseUrl() + "/api/evaluations/course/" + currentCourse.replace(" ", "%20")))
                    .GET().build();
            String res2 = client.send(req2, java.net.http.HttpResponse.BodyHandlers.ofString()).body();

            java.lang.reflect.Type evalType = new com.google.gson.reflect.TypeToken<List<Map<String, String>>>(){}.getType();
            List<Map<String, String>> assessments = gson.fromJson(res2, evalType);

            generateDynamicTimeline(sections, assessments);

        } catch (Exception e) { e.printStackTrace(); }
    }

    private void generateDynamicTimeline(List<Map<String, Object>> sections, List<Map<String, String>> allAssessments) {
        timelineContainer.getChildren().clear();

        for (Map<String, Object> sec : sections) {
            Double weekNumObj = (Double) sec.get("weekNumber");
            int currentWeek = weekNumObj != null ? weekNumObj.intValue() : 1;

            List<Map<String, String>> weeklyAssessments = new ArrayList<>();

            for (Map<String, String> a : allAssessments) {
                String dateStr = a.get("startDate");
                if (dateStr != null && !dateStr.isEmpty() && !dateStr.equals("null")) {
                    try {
                        LocalDate assessDate = LocalDate.parse(dateStr);
                        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(SEMESTER_START, assessDate);
                        int targetWeek = (int) (daysBetween / 7) + 1;
                        if (targetWeek < 1) targetWeek = 1;
                        if (targetWeek > 14) targetWeek = 14;
                        if (targetWeek == currentWeek) weeklyAssessments.add(a);
                    } catch (Exception ignored) {}
                }
            }

            TitledPane pane = createWeekPane(sec, weeklyAssessments);
            timelineContainer.getChildren().add(pane);
        }
    }

    private TitledPane createWeekPane(Map<String, Object> section, List<Map<String, String>> weeklyAssessments) {
        TitledPane pane = new TitledPane();
        pane.setExpanded(false);
        pane.setAnimated(true);

        String sectionTitle = String.valueOf(section.get("title"));
        Double weekNumObj = (Double) section.get("weekNumber");
        int weekNum = weekNumObj != null ? weekNumObj.intValue() : 1;
        Double secIdObj = (Double) section.get("id");
        int secId = secIdObj != null ? secIdObj.intValue() : 0;

        // --- HEADER ---
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(4, 8, 4, 0));

        Label titleLabel = new Label(sectionTitle);
        titleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #117A65;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox flairsBox = new HBox(6);
        flairsBox.setAlignment(Pos.CENTER_RIGHT);

        Map<String, String[]> colorDict = new HashMap<>();
        colorDict.put("ONLINE", new String[]{"#D5F5E3", "#27AE60"});
        colorDict.put("OFFLINE", new String[]{"#FDEBD0", "#D35400"});
        colorDict.put("CT", new String[]{"#D6EAF8", "#2980B9"});
        colorDict.put("ASSIGNMENT", new String[]{"#E8DAEF", "#8E44AD"});

        LinkedHashMap<String, String[]> seenTypes = new LinkedHashMap<>();
        for (Map<String, String> a : weeklyAssessments) {
            String type = a.get("type");
            if (type != null && !seenTypes.containsKey(type)) {
                seenTypes.put(type, colorDict.getOrDefault(type.toUpperCase(), new String[]{"#ECF0F1", "#7F8C8D"}));
            }
        }
        for (Map.Entry<String, String[]> entry : seenTypes.entrySet()) {
            Label flair = new Label(entry.getKey());
            flair.setStyle("-fx-background-color: " + entry.getValue()[0] + "; -fx-text-fill: " + entry.getValue()[1] + "; -fx-font-size: 11px; -fx-padding: 2 10; -fx-background-radius: 100; -fx-border-color: " + entry.getValue()[1] + "; -fx-border-radius: 100;");
            flairsBox.getChildren().add(flair);
        }

        // Teacher can rename weeks via pencil icon
        if (isTeacher) {
            Button renameBtn = new Button("✏");
            renameBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #BDC3C7; -fx-cursor: hand; -fx-font-size: 12px; -fx-padding: 2 6;");
            renameBtn.setOnMouseEntered(e -> renameBtn.setStyle("-fx-background-color: #EBF5FB; -fx-text-fill: #3498DB; -fx-cursor: hand; -fx-font-size: 12px; -fx-padding: 2 6; -fx-background-radius: 50;"));
            renameBtn.setOnMouseExited(e -> renameBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #BDC3C7; -fx-cursor: hand; -fx-font-size: 12px; -fx-padding: 2 6;"));
            renameBtn.setOnAction(e -> {
                TextInputDialog dialog = new TextInputDialog(sectionTitle);
                dialog.setTitle("Rename Week");
                dialog.setHeaderText("Set a custom name for Week " + weekNum);
                dialog.setContentText("Title:");
                dialog.showAndWait().ifPresent(newTitle -> {
                    if (!newTitle.trim().isEmpty()) {
                        renameWeekOnCloud(secId, newTitle.trim());
                        titleLabel.setText(newTitle.trim());
                    }
                });
            });
            header.getChildren().addAll(titleLabel, renameBtn, spacer, flairsBox);
        } else {
            header.getChildren().addAll(titleLabel, spacer, flairsBox);
        }

        pane.setGraphic(header);

        // --- CONTENT ---
        VBox content = new VBox(18);
        content.setPadding(new Insets(20, 24, 20, 24));
        content.setStyle("-fx-background-color: #FFFFFF;");

        // Lab Session date — ONLY for sessional courses
        if (isSessionalCourse) {
            LocalDate weekStart = SEMESTER_START.plusWeeks(weekNum - 1);
            DayOfWeek targetDay = DayOfWeek.valueOf(currentLabDay.toUpperCase());
            LocalDate actualLabDate = weekStart.with(TemporalAdjusters.nextOrSame(targetDay));

            HBox labRow = new HBox(10);
            labRow.setAlignment(Pos.CENTER_LEFT);
            labRow.setStyle("-fx-background-color: #F0FFF4; -fx-padding: 10 15; -fx-background-radius: 8; -fx-border-color: #C6F6D5; -fx-border-radius: 8;");

            Label labIcon = new Label("🧪");
            labIcon.setStyle("-fx-font-size: 16px;");
            Label labText = new Label("Lab Session: " + actualLabDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")));
            labText.setStyle("-fx-text-fill: #276749; -fx-font-size: 13px; -fx-font-weight: bold;");

            labRow.getChildren().addAll(labIcon, labText);
            content.getChildren().add(labRow);
        } else {
            // Theory course: show Room field (editable by teacher)
            if (isTeacher) {
                HBox roomRow = new HBox(10);
                roomRow.setAlignment(Pos.CENTER_LEFT);
                roomRow.setStyle("-fx-background-color: #FFF8F0; -fx-padding: 10 15; -fx-background-radius: 8; -fx-border-color: #FDEBD0; -fx-border-radius: 8;");

                Label roomIcon = new Label("🏫");
                roomIcon.setStyle("-fx-font-size: 16px;");
                Label roomLabel = new Label("Room:");
                roomLabel.setStyle("-fx-text-fill: #8B6914; -fx-font-weight: bold; -fx-font-size: 13px;");
                TextField roomField = new TextField();
                roomField.setPromptText("e.g., ECE 201");
                roomField.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E0D5C7; -fx-border-radius: 6; -fx-padding: 5 10; -fx-pref-width: 150;");
                roomRow.getChildren().addAll(roomIcon, roomLabel, roomField);
                content.getChildren().add(roomRow);
            }
        }

        // Resources section
        VBox resourcesSection = new VBox(8);
        Label resourcesHeader = new Label("Resources & Materials");
        resourcesHeader.setStyle("-fx-text-fill: #2C3E50; -fx-font-size: 14px; -fx-font-weight: bold;");
        resourcesSection.getChildren().add(resourcesHeader);

        if (isTeacher) {
            Button addMatBtn = new Button("+ Add Resource Link");
            String addBase = "-fx-background-color: #27AE60; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 6 14; -fx-background-radius: 20;";
            String addHover = addBase + " -fx-effect: dropshadow(three-pass-box, rgba(39, 174, 96, 0.4), 8, 0, 0, 2);";
            addMatBtn.setStyle(addBase);
            addMatBtn.setOnMouseEntered(e -> addMatBtn.setStyle(addHover));
            addMatBtn.setOnMouseExited(e -> addMatBtn.setStyle(addBase));
            addMatBtn.setOnAction(e -> handleAddMaterial(secId));
            resourcesSection.getChildren().add(addMatBtn);
        }

        // Assessments section
        VBox assessmentSection = new VBox(10);
        Label assessmentHeader = new Label("Assessments");
        assessmentHeader.setStyle("-fx-text-fill: #2C3E50; -fx-font-size: 14px; -fx-font-weight: bold;");
        assessmentSection.getChildren().add(assessmentHeader);

        if (weeklyAssessments.isEmpty()) {
            Label noAssess = new Label("No assessments scheduled this week.");
            noAssess.setStyle("-fx-text-fill: #95A5A6; -fx-font-size: 12px; -fx-font-style: italic;");
            assessmentSection.getChildren().add(noAssess);
        } else {
            HBox linksBox = new HBox(10);
            linksBox.setAlignment(Pos.CENTER_LEFT);
            linksBox.setStyle("-fx-padding: 5 0 0 0;");

            for (Map<String, String> assessment : weeklyAssessments) {
                String type = assessment.get("type");
                String title = assessment.get("title");
                String id = assessment.get("id");
                String[] colors = colorDict.getOrDefault(type != null ? type.toUpperCase() : "", new String[]{"#ECF0F1", "#7F8C8D"});

                String baseStyle = "-fx-background-color: " + colors[0] + "; -fx-text-fill: " + colors[1] + "; -fx-font-size: 12px; -fx-padding: 8 18; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-weight: bold; -fx-border-color: " + colors[1] + "40; -fx-border-radius: 8;";
                Button jumpLink = new Button(type + ": " + title);
                jumpLink.setStyle(baseStyle);
                jumpLink.setOnMouseEntered(e -> jumpLink.setStyle(baseStyle + " -fx-effect: dropshadow(three-pass-box, " + colors[1] + "60, 10, 0, 0, 2);"));
                jumpLink.setOnMouseExited(e -> jumpLink.setStyle(baseStyle));

                jumpLink.setOnAction(e -> {
                    SessionManager.setCurrentEvaluationId(id);
                    String parentTab = "Common";
                    if ("CT".equalsIgnoreCase(type) || "ASSIGNMENT".equalsIgnoreCase(type)) parentTab = "CT and Assignments";
                    else if ("ONLINE".equalsIgnoreCase(type)) parentTab = "Onlines";
                    else if ("OFFLINE".equalsIgnoreCase(type)) parentTab = "Offlines";

                    MainController.instance.forceSidebarSelection(parentTab);
                    MainController.instance.updateBreadcrumb(parentTab + " / " + title);
                    NavigationManager.switchScreen("evaluation_details.fxml");
                });
                linksBox.getChildren().add(jumpLink);
            }
            assessmentSection.getChildren().add(linksBox);
        }

        content.getChildren().addAll(resourcesSection, assessmentSection);
        pane.setContent(content);

        // Modern card styling
        String normalStyle = "-fx-base: #F7FDFB; -fx-background-color: #F7FDFB; -fx-border-color: #D5E8D4; -fx-border-radius: 10; -fx-background-radius: 10; -fx-border-width: 1.5;";
        String hoverStyle = normalStyle + " -fx-effect: dropshadow(three-pass-box, rgba(46, 204, 113, 0.3), 12, 0, 0, 3); -fx-cursor: hand;";
        pane.setStyle(normalStyle);
        pane.setOnMouseEntered(e -> pane.setStyle(hoverStyle));
        pane.setOnMouseExited(e -> pane.setStyle(normalStyle));

        return pane;
    }

    private void renameWeekOnCloud(int sectionId, String newTitle) {
        try {
            String json = String.format("{\"sectionId\":\"%d\", \"title\":\"%s\"}", sectionId, newTitle.replace("\"", "\\\""));
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(ServerConfig.getBaseUrl() + "/api/sections/rename"))
                    .header("Content-Type", "application/json")
                    .PUT(java.net.http.HttpRequest.BodyPublishers.ofString(json))
                    .build();
            client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void handleAddMaterial(int sectionId) {
        TextInputDialog linkDialog = new TextInputDialog("https://");
        linkDialog.setTitle("Attach Link");
        linkDialog.setHeaderText("Paste a Google Drive, YouTube, or any URL:");

        linkDialog.showAndWait().ifPresent(link -> {
            TextInputDialog titleDialog = new TextInputDialog("Lecture Slides");
            titleDialog.setHeaderText("What is the title of this resource?");

            titleDialog.showAndWait().ifPresent(title -> {
                String jsonPayload = String.format(
                        "{\"sectionId\":\"%d\",\"type\":\"Resource\",\"title\":\"%s\",\"description\":\"\",\"fileLink\":\"%s\",\"dueDate\":\"\"}",
                        sectionId, title.replace("\"", "\\\""), link.replace("\"", "\\\"")
                );
                sendPostRequest(ServerConfig.getBaseUrl() + "/api/modules", jsonPayload);
            });
        });
    }

    private void sendPostRequest(String url, String json) {
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(json))
                    .build();
            client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            String currentCourse = SessionManager.getCurrentCourseCode();
            fetchDataAndRender(currentCourse);
        } catch (Exception e) { e.printStackTrace(); }
    }
}
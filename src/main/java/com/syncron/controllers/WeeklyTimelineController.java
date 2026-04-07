package com.syncron.controllers;

import com.syncron.utils.NavigationManager;
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
    private String currentLabDay = "Thursday"; // Default

    @FXML
    public void initialize() {
        String currentCourse = SessionManager.getCurrentCourseCode();
        if (currentCourse == null || currentCourse.isEmpty()) {
            NavigationManager.switchScreen("home.fxml");
            return;
        }

        // TEACHER ONLY: Show the Lab Day Editor
        if ("TEACHER".equals(SessionManager.getCurrentUser().getRole())) {
            labDayBox.setVisible(true);
            labDayBox.setManaged(true);
            labDayCombo.getItems().setAll("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday");

            labDayCombo.setOnAction(e -> {
                String selected = labDayCombo.getValue();
                if (selected != null) updateLabDayToCloud(currentCourse, selected);
            });
        }

        fetchDataAndRender(currentCourse);
    }

    private void updateLabDayToCloud(String courseCode, String labDay) {
        try {
            String json = String.format("{\"courseCode\":\"%s\", \"labDay\":\"%s\"}", courseCode, labDay);
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://localhost:8080/api/timeline/settings"))
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(json))
                    .build();
            client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            // Reload seamlessly
            fetchDataAndRender(courseCode);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void fetchDataAndRender(String currentCourse) {
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            com.google.gson.Gson gson = new com.google.gson.Gson();

            // 1. Fetch Timeline (14 Weeks) + Lab Day Settings
            java.net.http.HttpRequest req1 = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://localhost:8080/api/timeline/" + currentCourse.replace(" ", "%20")))
                    .GET().build();
            String res1 = client.send(req1, java.net.http.HttpResponse.BodyHandlers.ofString()).body();

            java.lang.reflect.Type timelineType = new com.google.gson.reflect.TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> timelineData = gson.fromJson(res1, timelineType);

            currentLabDay = String.valueOf(timelineData.getOrDefault("labDay", "Thursday"));
            // Silently update combo box without triggering an infinite loop
            labDayCombo.setOnAction(null);
            labDayCombo.setValue(currentLabDay);
            labDayCombo.setOnAction(e -> updateLabDayToCloud(currentCourse, labDayCombo.getValue()));

            List<Map<String, Object>> sections = (List<Map<String, Object>>) timelineData.get("sections");

            // 2. Fetch Assessments
            java.net.http.HttpRequest req2 = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://localhost:8080/api/evaluations/course/" + currentCourse.replace(" ", "%20")))
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

            // 🚀 THE FLAWLESS DATE MATH ENGINE!
            for (Map<String, String> a : allAssessments) {
                String dateStr = a.get("startDate");
                if (dateStr != null && !dateStr.isEmpty() && !dateStr.equals("null")) {
                    try {
                        LocalDate assessDate = LocalDate.parse(dateStr);
                        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(SEMESTER_START, assessDate);

                        // Exact week calculation (Week 1 is days 0-6, Week 2 is days 7-13)
                        int targetWeek = (int) (daysBetween / 7) + 1;

                        if (targetWeek < 1) targetWeek = 1;
                        if (targetWeek > 14) targetWeek = 14;

                        if (targetWeek == currentWeek) {
                            weeklyAssessments.add(a);
                        }
                    } catch (Exception e) { e.printStackTrace(); }
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

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(4, 8, 4, 0));

        Label titleLabel = new Label(String.valueOf(section.get("title")));
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

        header.getChildren().addAll(titleLabel, spacer, flairsBox);
        pane.setGraphic(header);

        VBox content = new VBox(15);
        content.setPadding(new Insets(16, 20, 16, 20));
        content.setStyle("-fx-background-color: #FFFFFF;");

        Double weekNumObj = (Double) section.get("weekNumber");
        int weekNum = weekNumObj != null ? weekNumObj.intValue() : 1;

        //  THE DYNAMIC LAB DATE ENGINE
        LocalDate weekStart = SEMESTER_START.plusWeeks(weekNum - 1);
        DayOfWeek targetDay = DayOfWeek.valueOf(currentLabDay.toUpperCase());
        LocalDate actualLabDate = weekStart.with(TemporalAdjusters.nextOrSame(targetDay));

        Label timeLabel = new Label("Lab Session: " + actualLabDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")));
        timeLabel.setStyle("-fx-text-fill: #34495E; -fx-font-size: 13px; -fx-font-weight: bold;");

        // --- 🚀 THE RESOURCES UPLOAD BOX ---
        VBox resourcesSection = new VBox(8);
        Label resourcesHeader = new Label("Resources & Materials");
        resourcesHeader.setStyle("-fx-text-fill: #2C3E50; -fx-font-size: 14px; -fx-font-weight: bold;");
        resourcesSection.getChildren().add(resourcesHeader);

        if ("TEACHER".equals(SessionManager.getCurrentUser().getRole())) {
            Button addMatBtn = new Button("+ Add Resource Link");
            addMatBtn.setStyle("-fx-background-color: #27AE60; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 5 10; -fx-background-radius: 4;");

            Double secIdObj = (Double) section.get("id");
            int secId = secIdObj != null ? secIdObj.intValue() : 0;

            addMatBtn.setOnAction(e -> handleAddMaterial(secId));
            resourcesSection.getChildren().add(addMatBtn);
        }

        // --- 🚀 THE ASSESSMENTS SECTION (Restored!) ---
        VBox assessmentSection = new VBox(8);
        Label assessmentHeader = new Label("Assessments");
        assessmentHeader.setStyle("-fx-text-fill: #2C3E50; -fx-font-size: 14px; -fx-font-weight: bold;");
        assessmentSection.getChildren().add(assessmentHeader);

        HBox linksBox = new HBox(10);
        linksBox.setAlignment(Pos.CENTER_LEFT);

        if (weeklyAssessments.isEmpty()) {
            Label noAssess = new Label("No assessments scheduled this week.");
            noAssess.setStyle("-fx-text-fill: #95A5A6; -fx-font-size: 12px; -fx-font-style: italic;");
            assessmentSection.getChildren().add(noAssess);
        } else {
            for (Map<String, String> assessment : weeklyAssessments) {
                String type = assessment.get("type");
                String title = assessment.get("title");
                String id = assessment.get("id");
                String[] colors = colorDict.getOrDefault(type.toUpperCase(), new String[]{"#ECF0F1", "#7F8C8D"});

                String baseStyle = "-fx-background-color: " + colors[0] + "; -fx-text-fill: " + colors[1] + "; -fx-font-size: 12px; -fx-padding: 6 16; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-weight: bold; -fx-border-color: " + colors[1] + "; -fx-border-radius: 6;";
                Button jumpLink = new Button(type + ": " + title);
                jumpLink.setStyle(baseStyle);
                jumpLink.setOnMouseEntered(e -> jumpLink.setStyle(baseStyle + " -fx-effect: dropshadow(three-pass-box, " + colors[1] + "80, 10, 0, 0, 0);"));
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

        content.getChildren().addAll(timeLabel, resourcesSection, assessmentSection);
        pane.setContent(content);

        String normalStyle = "-fx-base: #E8F8F5; -fx-background-color: #E8F8F5; -fx-border-color: #A3E4D7; -fx-border-radius: 8; -fx-background-radius: 8;";
        String hoverStyle = normalStyle + " -fx-effect: dropshadow(three-pass-box, rgba(46, 204, 113, 0.4), 15, 0, 0, 0); -fx-cursor: hand;";
        pane.setStyle(normalStyle);
        pane.setOnMouseEntered(e -> pane.setStyle(hoverStyle));
        pane.setOnMouseExited(e -> pane.setStyle(normalStyle));

        return pane;
    }


    private void handleAddMaterial(int sectionId) {
        javafx.scene.control.TextInputDialog linkDialog = new javafx.scene.control.TextInputDialog("https://");
        linkDialog.setTitle("Attach Link");
        linkDialog.setHeaderText("Paste a Google Drive or YouTube Link:");

        linkDialog.showAndWait().ifPresent(link -> {
            javafx.scene.control.TextInputDialog titleDialog = new javafx.scene.control.TextInputDialog("Lecture Slides");
            titleDialog.setHeaderText("What is the title of this resource?");

            titleDialog.showAndWait().ifPresent(title -> {
                String jsonPayload = String.format(
                        "{\"sectionId\":\"%d\",\"type\":\"Resource\",\"title\":\"%s\",\"description\":\"\",\"fileLink\":\"%s\",\"dueDate\":\"\"}",
                        sectionId, title.replace("\"", "\\\""), link.replace("\"", "\\\"")
                );
                sendPostRequest("http://localhost:8080/api/modules", jsonPayload);
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

            // Reload UI seamlessly
            String currentCourse = SessionManager.getCurrentCourseCode();
            fetchDataAndRender(currentCourse);
        } catch (Exception e) { e.printStackTrace(); }
    }
}
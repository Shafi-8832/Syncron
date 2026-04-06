package com.syncron.controllers;

import com.syncron.models.Assessment;
import com.syncron.models.Module;
import com.syncron.models.Section;
import com.syncron.utils.DatabaseHandler;
import com.syncron.utils.NavigationManager;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class WeeklyTimelineController {

    @FXML private VBox timelineContainer;
    @FXML private Button addWeekBtn;

    private static final LocalDate SEMESTER_START = LocalDate.of(2026, 1, 15);
    private static final Map<String, String[]> FLAIR_DEFS = new LinkedHashMap<>();
    private int currentTotalWeeks = 0;

    static {
        FLAIR_DEFS.put("Online",     new String[]{"#1B5E20", "#A5D6A7"});
        FLAIR_DEFS.put("Offline",    new String[]{"#E65100", "#FFCC80"});
        FLAIR_DEFS.put("CT",         new String[]{"#0D47A1", "#90CAF9"});
        FLAIR_DEFS.put("Assignment", new String[]{"#4A148C", "#CE93D8"});
        FLAIR_DEFS.put("Quiz",       new String[]{"#BF360C", "#FFAB91"});
    }

    @FXML
    public void initialize() {
        String currentCourse = SessionManager.getCurrentCourseCode();
        if (currentCourse == null || currentCourse.isEmpty()) {
            NavigationManager.switchScreen("home.fxml");
            return;
        }

        // Show Admin controls if Teacher
        if ("TEACHER".equals(SessionManager.getCurrentUser().getRole())) {
            addWeekBtn.setVisible(true);
            addWeekBtn.setManaged(true);
        }

        List<Section> dbSections = DatabaseHandler.getSectionsForCourse(currentCourse);
        List<Assessment> allAssessments = DatabaseHandler.getAssessmentsForCourse(currentCourse);
        currentTotalWeeks = dbSections.size();

        generateDynamicTimeline(dbSections, allAssessments);
    }

    private void generateDynamicTimeline(List<Section> sections, List<Assessment> allAssessments) {
        timelineContainer.getChildren().clear();

        Map<Integer, List<Assessment>> assessmentsByWeek = new LinkedHashMap<>();
        for (Assessment a : allAssessments) {
            assessmentsByWeek.computeIfAbsent(a.getWeekNumber(), k -> new ArrayList<>()).add(a);
        }

        for (Section sec : sections) {
            List<Assessment> matchingAssessments = assessmentsByWeek.getOrDefault(sec.getWeekNumber(), new ArrayList<>());
            timelineContainer.getChildren().add(createWeekPane(sec, matchingAssessments));
        }

        if (sections.isEmpty()) {
            Label emptyLabel = new Label("No weeks have been created for this course yet.");
            emptyLabel.setStyle("-fx-text-fill: #90A4AE; -fx-font-style: italic; -fx-padding: 20;");
            timelineContainer.getChildren().add(emptyLabel);
        }
    }

    private TitledPane createWeekPane(Section section, List<Assessment> weeklyAssessments) {
        TitledPane pane = new TitledPane();
        pane.setExpanded(false);
        pane.setAnimated(true);

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(4, 8, 4, 0));

        // Dark green title text
        Label titleLabel = new Label(section.getTitle());
        titleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #117A65;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox flairsBox = new HBox(6);
        flairsBox.setAlignment(Pos.CENTER_RIGHT);

        if (section.getFlairType() != null && !section.getFlairType().isEmpty() && !section.getFlairType().equals("Standard")) {
            flairsBox.getChildren().add(createFlairLabel(section.getFlairType(), "#D5F5E3", "#1E8449"));
        }

        header.getChildren().addAll(titleLabel, spacer, flairsBox);
        pane.setGraphic(header);

        // --- CONTENT (Light Theme) ---
        VBox content = new VBox(12);
        content.setPadding(new Insets(16, 20, 16, 20));
        content.setStyle("-fx-background-color: #FFFFFF;"); // Clean white inside

        LocalDate weekStart = SEMESTER_START.plusWeeks(section.getWeekNumber() - 1);
        Label timeLabel = new Label("Time and Date: " + weekStart.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy")));
        timeLabel.setStyle("-fx-text-fill: #7F8C8D; -fx-font-size: 13px;");

        // Resources Area
        VBox resourcesSection = new VBox(8);
        Label resourcesHeader = new Label("Resources & Materials");
        resourcesHeader.setStyle("-fx-text-fill: #2C3E50; -fx-font-size: 14px; -fx-font-weight: bold;");
        resourcesSection.getChildren().add(resourcesHeader);

        if (section.getModules() != null && !section.getModules().isEmpty()) {
            for (Module m : section.getModules()) {
                Label resLabel = new Label("📄 " + m.getTitle());
                resLabel.setStyle("-fx-text-fill: #2980B9; -fx-font-size: 13px; -fx-cursor: hand; -fx-underline: true;");
                resLabel.setOnMouseClicked(e -> {
                    try { java.awt.Desktop.getDesktop().browse(new java.net.URI(m.getFileLink())); }
                    catch (Exception ex) { new Alert(Alert.AlertType.ERROR, "Invalid Link!").show(); }
                });
                resourcesSection.getChildren().add(resLabel);
            }
        } else {
            Label noRes = new Label("No resources attached.");
            noRes.setStyle("-fx-text-fill: #95A5A6; -fx-font-style: italic;");
            resourcesSection.getChildren().add(noRes);
        }

        if ("TEACHER".equals(SessionManager.getCurrentUser().getRole())) {
            Button addMatBtn = new Button("+ Add Resource Link");
            addMatBtn.setStyle("-fx-background-color: #27AE60; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand;");
            addMatBtn.setOnAction(e -> handleAddMaterial(section.getId()));
            resourcesSection.getChildren().add(addMatBtn);
        }

        content.getChildren().addAll(timeLabel, resourcesSection);
        pane.setContent(content);

        // --- THE GREEN GLOW & STYLE ---
        String normalStyle = "-fx-base: #E8F8F5; -fx-background-color: #E8F8F5; -fx-border-color: #A3E4D7; -fx-border-radius: 8; -fx-background-radius: 8;";
        String hoverStyle = normalStyle + " -fx-effect: dropshadow(three-pass-box, rgba(46, 204, 113, 0.4), 15, 0, 0, 0); -fx-cursor: hand;";

        pane.setStyle(normalStyle);
        pane.setOnMouseEntered(e -> pane.setStyle(hoverStyle));
        pane.setOnMouseExited(e -> pane.setStyle(normalStyle));

        return pane;
    }

    private Label createFlairLabel(String text, String bgColor, String textColor) {
        Label flair = new Label(text);
        flair.setStyle("-fx-background-color: " + bgColor + "; -fx-text-fill: " + textColor + "; -fx-font-size: 11px; -fx-padding: 2 10; -fx-background-radius: 100; -fx-border-radius: 100; -fx-border-color: " + textColor + ";");
        return flair;
    }

    @FXML
    private void handleAddWeek() {
        TextInputDialog dialog = new TextInputDialog("Week " + (currentTotalWeeks + 1) + ": New Topic");
        dialog.setTitle("Create New Week");
        dialog.setHeaderText("Add a new week to the syllabus timeline");
        dialog.setContentText("Title:");

        dialog.showAndWait().ifPresent(title -> {
            String jsonPayload = String.format("{\"courseCode\":\"%s\",\"title\":\"%s\",\"weekNumber\":\"%d\",\"flairType\":\"Standard\"}",
                    SessionManager.getCurrentCourseCode(), title.replace("\"", "\\\""), currentTotalWeeks + 1);
            sendPostRequest("http://localhost:8080/api/sections", jsonPayload);
        });
    }

    private void handleAddMaterial(int sectionId) {
        TextInputDialog linkDialog = new TextInputDialog("https://");
        linkDialog.setTitle("Attach Link");
        linkDialog.setHeaderText("Paste a Google Drive or YouTube Link:");

        linkDialog.showAndWait().ifPresent(link -> {
            TextInputDialog titleDialog = new TextInputDialog("Lecture Slides");
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

            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                initialize(); // Reload the UI seamlessly!
            } else {
                new Alert(Alert.AlertType.ERROR, "Cloud Error: Could not save.").show();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}
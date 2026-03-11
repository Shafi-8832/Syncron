package com.syncron.controllers;

import com.syncron.models.Assessment;
import com.syncron.utils.DatabaseHandler;
import com.syncron.utils.NavigationManager;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for the Weekly Timeline view.
 * Dynamically generates 14 TitledPane components representing course weeks.
 * Fetches live assessment data from the database via DatabaseHandler.
 */
public class WeeklyTimelineController {

    @FXML
    private VBox timelineContainer;

    private static final int TOTAL_WEEKS = 14;
    private static final LocalDate SEMESTER_START = LocalDate.of(2026, 1, 15);

    // Flair definitions: assessmentType -> {background color, text color}
    private static final Map<String, String[]> FLAIR_DEFS = new LinkedHashMap<>();
    static {
        FLAIR_DEFS.put("Online",     new String[]{"#1B5E20", "#A5D6A7"});
        FLAIR_DEFS.put("Offline",    new String[]{"#E65100", "#FFCC80"});
        FLAIR_DEFS.put("CT",         new String[]{"#0D47A1", "#90CAF9"});
        FLAIR_DEFS.put("Assignment", new String[]{"#4A148C", "#CE93D8"});
        FLAIR_DEFS.put("Quiz",       new String[]{"#BF360C", "#FFAB91"});
    }

    private List<Assessment> allAssessments;

    @FXML
    public void initialize() {
        allAssessments = DatabaseHandler.getAssessmentsForCourse("CSE 108");
        generateTimeline();
    }

    /**
     * Dynamically creates 14 TitledPane components and adds them to the timelineContainer.
     * Each pane represents one week with a styled header and expandable content.
     */
    private void generateTimeline() {
        // Page title
        Label pageTitle = new Label("Weekly Timeline");
        pageTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #ECEFF1;");
        pageTitle.setPadding(new Insets(0, 0, 8, 0));
        timelineContainer.getChildren().add(pageTitle);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d");

        // Pre-group assessments by week number for O(n) lookup
        Map<Integer, List<Assessment>> assessmentsByWeek = new LinkedHashMap<>();
        for (Assessment a : allAssessments) {
            assessmentsByWeek.computeIfAbsent(a.getWeekNumber(), k -> new ArrayList<>()).add(a);
        }

        for (int i = 0; i < TOTAL_WEEKS; i++) {
            LocalDate weekStart = SEMESTER_START.plusWeeks(i);
            LocalDate weekEnd = weekStart.plusDays(6);

            String weekTitle = "Week " + (i + 1) + ": "
                    + weekStart.format(formatter) + " - " + weekEnd.format(formatter);

            int weekNum = i + 1;
            List<Assessment> weeklyAssessments = assessmentsByWeek.getOrDefault(weekNum, new ArrayList<>());

            TitledPane pane = createWeekPane(weeklyAssessments, weekTitle);
            timelineContainer.getChildren().add(pane);
        }
    }

    /**
     * Creates a single TitledPane for a week with header graphic and expandable content.
     */
    private TitledPane createWeekPane(List<Assessment> weeklyAssessments, String weekTitle) {
        TitledPane pane = new TitledPane();
        pane.setExpanded(false);
        pane.setAnimated(true);

        // --- Header Graphic ---
        pane.setGraphic(createHeaderGraphic(weeklyAssessments, weekTitle));

        // --- Content ---
        pane.setContent(createWeekContent(weekIndex, weeklyAssessments));

        // --- Styling ---
        pane.getStyleClass().add("timeline-pane");
        applyPaneStyle(pane);

        return pane;
    }

    /**
     * Applies dark-themed styling to a TitledPane.
     */
    private void applyPaneStyle(TitledPane pane) {
        pane.setStyle(
                "-fx-base: #2A2A3D;"
                        + " -fx-background-color: #2A2A3D;"
                        + " -fx-text-fill: #ECEFF1;"
                        + " -fx-border-color: #3C3C55;"
                        + " -fx-border-width: 1;"
                        + " -fx-border-radius: 8;"
                        + " -fx-background-radius: 8;"
        );
    }

    /**
     * Creates the header graphic HBox for a TitledPane.
     * Contains a week title label and flair labels derived from assessments.
     */
    private HBox createHeaderGraphic(List<Assessment> weeklyAssessments, String weekTitle) {
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(4, 8, 4, 0));

        // Week title label
        Label titleLabel = new Label(weekTitle);
        titleLabel.setStyle(
                "-fx-font-size: 15px;"
                        + " -fx-font-weight: bold;"
                        + " -fx-text-fill: #ECEFF1;"
        );

        // Spacer to push flairs to the right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        spacer.setMinWidth(40);

        // Flairs HBox
        HBox flairsBox = new HBox(6);
        flairsBox.setAlignment(Pos.CENTER_RIGHT);

        // Collect unique assessment types in order
        LinkedHashMap<String, String[]> seenTypes = new LinkedHashMap<>();
        for (Assessment a : weeklyAssessments) {
            String type = a.getAssessmentType();
            if (!seenTypes.containsKey(type)) {
                String[] colors = FLAIR_DEFS.get(type);
                if (colors != null) {
                    seenTypes.put(type, colors);
                }
            }
        }
        for (Map.Entry<String, String[]> entry : seenTypes.entrySet()) {
            flairsBox.getChildren().add(createFlairLabel(entry.getKey(), entry.getValue()[0], entry.getValue()[1]));
        }

        header.getChildren().addAll(titleLabel, spacer, flairsBox);
        return header;
    }

    /**
     * Creates a small pill-style flair label with rounded borders and distinct colors.
     */
    private Label createFlairLabel(String text, String bgColor, String textColor) {
        Label flair = new Label(text);
        flair.setStyle(
                "-fx-background-color: " + bgColor + ";"
                        + " -fx-text-fill: " + textColor + ";"
                        + " -fx-font-size: 11px;"
                        + " -fx-padding: 2 10;"
                        + " -fx-background-radius: 100;"
                        + " -fx-border-radius: 100;"
                        + " -fx-border-color: " + textColor + ";"
                        + " -fx-border-width: 1;"
        );
        return flair;
    }

    /**
     * Creates the expandable content VBox for a week.
     * Contains time/date info, a resources section, and assessment buttons.
     */
    private VBox createWeekContent(int weekIndex, List<Assessment> weeklyAssessments) {
        VBox content = new VBox(12);
        content.setPadding(new Insets(16, 20, 16, 20));
        content.setStyle("-fx-background-color: #23233A;");

        // --- THE RESTORED TIME AND DATE ---
        // Rebuilding the dates using your original SEMESTER_START variable
        java.time.LocalDate weekStart = java.time.LocalDate.of(2026, 1, 15).plusWeeks(weekIndex);
        java.time.format.DateTimeFormatter fullFormatter = java.time.format.DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy");

        Label timeLabel = new Label("Time and Date: " + weekStart.format(fullFormatter)
                + "    Room: CSE-" + (301 + weekIndex));
        timeLabel.setStyle("-fx-text-fill: #B0BEC5; -fx-font-size: 13px;");
        timeLabel.setWrapText(true);

        // --- Resources Section ---
        VBox resourcesSection = new VBox(6);
        Label resourcesHeader = new Label("Resources");
        resourcesHeader.setStyle("-fx-text-fill: #ECEFF1; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 8 0 2 0;");
        Label resourceDesc = new Label("Lecture slides, notes, and reference materials.");
        resourceDesc.setStyle("-fx-text-fill: #90A4AE; -fx-font-size: 12px;");
        resourceDesc.setWrapText(true);
        resourcesSection.getChildren().addAll(resourcesHeader, resourceDesc);

        // --- Assessment Links ---
        VBox assessmentSection = new VBox(8);
        Label assessmentHeader = new Label("Assessments");
        assessmentHeader.setStyle("-fx-text-fill: #ECEFF1; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 8 0 2 0;");
        assessmentSection.getChildren().add(assessmentHeader);

        HBox linksBox = new HBox(10);
        linksBox.setAlignment(Pos.CENTER_LEFT);

        for (com.syncron.models.Assessment assessment : weeklyAssessments) {
            String type = assessment.getAssessmentType();
            String[] colors = FLAIR_DEFS.get(type);
            String bgColor = colors != null ? colors[0] : "#37474F";
            String textColor = colors != null ? colors[1] : "#ECEFF1";
            Button link = createAssessmentButton(assessment, bgColor, textColor);
            linksBox.getChildren().add(link);
        }
        assessmentSection.getChildren().add(linksBox);

        // --- ADD ALL THREE PIECES TO THE SCREEN ---
        content.getChildren().addAll(timeLabel, resourcesSection, assessmentSection);
        return content;
    }
    /**
     * Builds a common button style string with the given background and text colors.
     */
    private String buildButtonStyle(String bgColor, String textColor) {
        return "-fx-background-color: " + bgColor + ";"
                + " -fx-text-fill: " + textColor + ";"
                + " -fx-font-size: 12px;"
                + " -fx-padding: 6 16;"
                + " -fx-background-radius: 6;"
                + " -fx-border-radius: 6;"
                + " -fx-cursor: hand;";
    }

    /**
     * Creates a styled Button that acts as an assessment link.
     * Accepts an Assessment object and navigates to the detail view on click.
     */
    private Button createAssessmentButton(Assessment assessment, String bgColor, String textColor) {
        String text = assessment.getTitle();
        Button button = new Button(text);
        button.setStyle(buildButtonStyle(bgColor, textColor));
        button.setOnMouseEntered(e ->
                button.setStyle(buildButtonStyle("derive(" + bgColor + ", 20%)", "white")));
        button.setOnMouseExited(e ->
                button.setStyle(buildButtonStyle(bgColor, textColor)));
        button.setOnAction(e -> {
            AssessmentDetailController detailPage = NavigationManager.switchScreen("assessment_detail.fxml");

            NavigationManager.updateGlobalBreadcrumb("Weekly Timeline / " + assessment.getTitle());

            if (detailPage != null) {
                detailPage.initializeView("TEACHER", "ACTIVE");
            }
        });
        return button;
    }
}

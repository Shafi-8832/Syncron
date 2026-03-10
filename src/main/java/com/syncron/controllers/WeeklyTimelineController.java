package com.syncron.controllers;

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

/**
 * Controller for the Weekly Timeline view.
 * Dynamically generates 14 TitledPane components representing course weeks.
 * No database connections — uses placeholder data for layout demonstration.
 */
public class WeeklyTimelineController {

    @FXML
    private VBox timelineContainer;

    private static final int TOTAL_WEEKS = 14;
    private static final LocalDate SEMESTER_START = LocalDate.of(2026, 1, 15);

    // Flair definitions: label, background color, text color
    private static final String[][] FLAIR_DEFS = {
            {"Online",  "#1B5E20", "#A5D6A7"},
            {"Offline", "#E65100", "#FFCC80"},
            {"CT",      "#0D47A1", "#90CAF9"}
    };

    // Flair assignments per week (indices into FLAIR_DEFS)
    private static final int[][] WEEK_FLAIRS = {
            {0},       // Week 1: Online
            {0, 1},    // Week 2: Online, Offline
            {0},       // Week 3: Online
            {0, 2},    // Week 4: Online, CT
            {0, 1},    // Week 5: Online, Offline
            {0},       // Week 6: Online
            {0, 1, 2}, // Week 7: Online, Offline, CT
            {0},       // Week 8: Online
            {0, 1},    // Week 9: Online, Offline
            {0},       // Week 10: Online
            {0, 2},    // Week 11: Online, CT
            {0, 1},    // Week 12: Online, Offline
            {0},       // Week 13: Online
            {0, 1, 2}  // Week 14: Online, Offline, CT
    };

    @FXML
    public void initialize() {
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

        for (int i = 0; i < TOTAL_WEEKS; i++) {
            LocalDate weekStart = SEMESTER_START.plusWeeks(i);
            LocalDate weekEnd = weekStart.plusDays(6);

            String weekTitle = "Week " + (i + 1) + ": "
                    + weekStart.format(formatter) + " - " + weekEnd.format(formatter);

            TitledPane pane = createWeekPane(i, weekTitle);
            timelineContainer.getChildren().add(pane);
        }
    }

    /**
     * Creates a single TitledPane for a week with header graphic and expandable content.
     */
    private TitledPane createWeekPane(int weekIndex, String weekTitle) {
        TitledPane pane = new TitledPane();
        pane.setExpanded(false);
        pane.setAnimated(true);

        // --- Header Graphic ---
        pane.setGraphic(createHeaderGraphic(weekIndex, weekTitle));

        // --- Content ---
        pane.setContent(createWeekContent(weekIndex));

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
     * Contains a week title label and flair labels.
     */
    private HBox createHeaderGraphic(int weekIndex, String weekTitle) {
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

        int[] flairIndices = WEEK_FLAIRS[weekIndex];
        for (int idx : flairIndices) {
            flairsBox.getChildren().add(createFlairLabel(
                    FLAIR_DEFS[idx][0], FLAIR_DEFS[idx][1], FLAIR_DEFS[idx][2]));
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
     * Contains time/date info, a resources section, and assessment links.
     */
    private VBox createWeekContent(int weekIndex) {
        VBox content = new VBox(12);
        content.setPadding(new Insets(16, 20, 16, 20));
        content.setStyle("-fx-background-color: #23233A;");

        int weekNum = weekIndex + 1;
        LocalDate weekStart = SEMESTER_START.plusWeeks(weekIndex);
        DateTimeFormatter fullFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy");

        // --- Time and Date / Room ---
        Label timeLabel = new Label("Time and Date: " + weekStart.format(fullFormatter)
                + "    Room: CSE-" + (301 + weekIndex));
        timeLabel.setStyle(
                "-fx-text-fill: #B0BEC5;"
                        + " -fx-font-size: 13px;"
        );
        timeLabel.setWrapText(true);

        // --- Resources Section ---
        VBox resourcesSection = new VBox(6);
        Label resourcesHeader = new Label("Resources");
        resourcesHeader.setStyle(
                "-fx-text-fill: #ECEFF1;"
                        + " -fx-font-size: 14px;"
                        + " -fx-font-weight: bold;"
                        + " -fx-padding: 8 0 2 0;"
        );
        Label resourceDesc = new Label("Lecture slides, notes, and reference materials for Week " + weekNum + ".");
        resourceDesc.setStyle("-fx-text-fill: #90A4AE; -fx-font-size: 12px;");
        resourceDesc.setWrapText(true);
        resourcesSection.getChildren().addAll(resourcesHeader, resourceDesc);

        // --- Assessment Links ---
        VBox assessmentSection = new VBox(8);
        Label assessmentHeader = new Label("Assessments");
        assessmentHeader.setStyle(
                "-fx-text-fill: #ECEFF1;"
                        + " -fx-font-size: 14px;"
                        + " -fx-font-weight: bold;"
                        + " -fx-padding: 8 0 2 0;"
        );
        assessmentSection.getChildren().add(assessmentHeader);

        HBox linksBox = new HBox(10);
        linksBox.setAlignment(Pos.CENTER_LEFT);

        int[] flairIndices = WEEK_FLAIRS[weekIndex];
        for (int idx : flairIndices) {
            String type = FLAIR_DEFS[idx][0];
            String bgColor = FLAIR_DEFS[idx][1];
            String textColor = FLAIR_DEFS[idx][2];
            Button link = createAssessmentButton("Go to " + type + " " + weekNum, bgColor, textColor);
            linksBox.getChildren().add(link);
        }
        assessmentSection.getChildren().add(linksBox);

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
     * Click handling will be connected to navigation logic in a future update.
     */
    private Button createAssessmentButton(String text, String bgColor, String textColor) {
        Button button = new Button(text);
        button.setStyle(buildButtonStyle(bgColor, textColor));
        button.setOnMouseEntered(e ->
                button.setStyle(buildButtonStyle("derive(" + bgColor + ", 20%)", "white")));
        button.setOnMouseExited(e ->
                button.setStyle(buildButtonStyle(bgColor, textColor)));
        button.setOnAction(e -> { /* Navigation to be implemented with database integration */ });
        return button;
    }
}

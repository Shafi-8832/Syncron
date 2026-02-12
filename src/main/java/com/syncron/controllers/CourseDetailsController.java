package com.syncron.controllers;

import com.syncron.utils.DatabaseHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class CourseDetailsController {
    @FXML private Label courseTitleLabel;
    @FXML private VBox contentContainer;

    public void setCourseData(String courseCode) {
        // Clear previous content
        contentContainer.getChildren().clear();

        Button backButton = new Button("← Back to Courses");
        backButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #2980B9; -fx-font-weight: bold; -fx-cursor: hand;");

        // The Back Action : Re-load the course fxml page
        backButton.setOnAction(event -> {
            try {
                // Load the courses.fxml file
                Parent courseListView = FXMLLoader.load(getClass().getResource("/com/syncron/views/courses.fxml"));

                // Find the DashBoard and swap the center back
                BorderPane mainLayout = (BorderPane) contentContainer.getScene().getRoot();
                mainLayout.setCenter(courseListView);
            }
            catch (Exception e) {e.printStackTrace();}
        });

        contentContainer.getChildren().add(backButton);

        courseTitleLabel.setText("Course: " + courseCode);

        // 2. Clear the container (in case it was previously used)
        contentContainer.getChildren().clear();

        // 3. Fetch sections (weeks) from Database
        // Note : getSections method in DatabaseHandler next

        var sections = DatabaseHandler.getSectionsForCourse(courseCode);
        // 2. CHECK: If no data exists, show a "No Content" message
        if (sections.isEmpty()) {
            Label emptyMsg = new Label("No content added by teacher yet.");
            emptyMsg.setStyle("-fx-text-fill: grey; -fx-font-size: 14px;");
            contentContainer.getChildren().add(emptyMsg);
            return;
        }

        for (var section : sections) {
            // --- Create the week Container (The Box for each week) ---
            VBox weekBox = new VBox(10);
            weekBox.setStyle("-fx-background-color: white; -fx-padding: 15; " +
                    "-fx-background-radius: 10; -fx-border-color: #DCDDE1; " +
                    "-fx-border-radius: 10;");

            // --- Add the week Title ---
            Label title = new Label(section.getTitle());
            title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
            weekBox.getChildren().add(title);

            // --- Adding Reddit Style Flairs if exists
            if (!section.getFlairType().equals("none")) {
                Label flair = new Label(section.getFlairType().toUpperCase());

                // Set flair color based on type
                String color = section.getFlairType().equals("project") ? "#E74C3C" : "#9B59B6";
                flair.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; " +
                        "-fx-padding: 2 8; -fx-background-radius: 5; -fx-font-size: 10px;");

                weekBox.getChildren().add(flair);
            }

            // --- Adding the Modules (Items inside this week) ---
            for (var module : section.getModules()) {
                HBox itemRow = new HBox(10);
                itemRow.setAlignment(Pos.CENTER_LEFT);

                Label itemLabel = new Label("• " + module.getTitle());

                // If Offline, adding a Submit button
                if (module.getType().equals("Offline")) {
                    Button submitButton = new Button("Submit");
                    submitButton.setStyle("-fx-background-color: #3498DB; -fx-text-fill: white;");
                    itemRow.getChildren().addAll(itemLabel, submitButton);
                }
                else {
                    itemRow.getChildren().add(itemLabel);
                }

                weekBox.getChildren().add(itemRow);
            }

        }
    }
}

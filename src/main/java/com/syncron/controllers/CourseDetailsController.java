package com.syncron.controllers;

import com.syncron.utils.DatabaseHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.File;

public class CourseDetailsController {
    @FXML private Label courseTitleLabel;
    @FXML private VBox contentContainer;

    public void setCourseData(String courseCode) {
        // Clear previous content
        contentContainer.getChildren().clear();

        // Back Button START
        Button backButton = new Button("← Back to DashBoard");
        backButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #2980B9; -fx-font-weight: bold; -fx-cursor: hand;");

        // The Back Action : Re-load the course fxml page
        backButton.setOnAction(event -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/syncron/views/home.fxml"));
                Parent dashboardView = loader.load();

                Stage stage = (Stage) contentContainer.getScene().getWindow();

                stage.getScene().setRoot(dashboardView);
            }
            catch (Exception e) {e.printStackTrace();}
        });
        contentContainer.getChildren().add(backButton);
        // Back Button END


        Label titleLabel = new Label("Course: " + courseCode);
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-padding: 10 0 10 0;");
        contentContainer.getChildren().add(titleLabel);

        // 2. Clear the container (in case it was previously used)
//        contentContainer.getChildren().clear();

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
                HBox itemRow = new HBox(15); // 15px gap between text and button
                itemRow.setAlignment(Pos.CENTER_LEFT);

                // 1. Create a container just for the text (Stacks vertically)
                VBox textContainer = new VBox(3); // 3px gap between title and description

                // 2. The Title Label
                Label titleLabel2 = new Label("• " + module.getTitle());
                titleLabel2.setStyle("-fx-font-size: 14px; -fx-text-fill: #2C3E50;");
                textContainer.getChildren().add(titleLabel2);

                // 3. The Description Label (Only add it if the description exists!)
                if (module.getDescription() != null && !module.getDescription().isEmpty()) {
                    Label descLabel = new Label(module.getDescription());
                    // Make it grey, smaller, and indent it so it aligns nicely under the bullet point
                    descLabel.setStyle("-fx-text-fill: #7F8C8D; -fx-font-size: 11px; -fx-padding: 0 0 0 10;");
                    textContainer.getChildren().add(descLabel);
                }

                // 4. Add the text container and the button to the main row
                if (module.getType().equals("offline")) {
                    Button submitBtn = getSubmitBtn(module);
                    itemRow.getChildren().addAll(textContainer, submitBtn);
                } else {
                    itemRow.getChildren().add(textContainer);
                }

                weekBox.getChildren().add(itemRow);
            }

            contentContainer.getChildren().add(weekBox);

        }
    }

    private static Button getSubmitBtn(com.syncron.models.Module module) {
        Button submitBtn = new Button("Submit");
        submitBtn.setStyle("-fx-background-color: #3498DB; -fx-text-fill: white; -fx-cursor: hand;");

        // --- NEW: The Click Action ---
        submitBtn.setOnAction(event -> {
            // 1. Open a File Chooser window
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Select Assignment File");

            // (Optional) Only allow zip or pdf files
            fileChooser.getExtensionFilters().addAll(
                    new javafx.stage.FileChooser.ExtensionFilter("Assignment Files", "*.zip", "*.pdf", "*.cpp", "*.java")
            );

            // Show the window
            File selectedFile = fileChooser.showOpenDialog(submitBtn.getScene().getWindow());

            // 2. If they actually picked a file (didn't click cancel)
            if (selectedFile != null) {
                // 3. Save it to our project folder! (Assuming student ID is "STU001" for now)
                String savedPath = com.syncron.utils.FileHandler.saveFile(selectedFile, "STU001", module.getTitle());

                if (savedPath != null) {
                    // 4. Change the UI to look like a stamped Boarding Pass!
                    submitBtn.setText("✔ Submitted");
                    submitBtn.setStyle("-fx-background-color: #2ECC71; -fx-text-fill: white; -fx-font-weight: bold;");
                    submitBtn.setDisable(true); // Prevent submitting twice
                }
            }
        });
        return submitBtn;
    }
}

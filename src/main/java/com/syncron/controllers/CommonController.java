package com.syncron.controllers;

import com.syncron.models.User;
import com.syncron.utils.DatabaseHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.util.List;

public class CommonController {
    private static final String TEACHER_ROLE = "TEACHER";

    @FXML
    private VBox teacherResourcesContainer;

    @FXML
    public void initialize() {
        loadTeacherResources();
    }

    private void loadTeacherResources() {
        teacherResourcesContainer.getChildren().clear();

        // 1. Grab the current course from memory, then fetch the real teachers!
        String currentCourse = SessionManager.getCurrentCourseCode();
        List<User> realTeachers = DatabaseHandler.getTeachersByCourse(currentCourse);

        for (User teacher : realTeachers) {
            // 2. The Beautiful Box Container
            VBox teacherBox = new VBox(15);
            teacherBox.setPadding(new Insets(20));
            teacherBox.setStyle("-fx-background-color: #FFFCF8; -fx-border-color: #E0D5C7; -fx-border-radius: 12; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(74,44,26,0.05), 8, 0, 0, 3);");

            // --- Header Row (Photo + Name) ---
            HBox headerBox = new HBox(12);
            headerBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            Circle profilePic = new Circle(18, javafx.scene.paint.Color.web("#A0522D")); // Warm brown circle

            VBox nameCol = new VBox(2);
            Label nameLabel = new Label(teacher.getName());
            nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");
            Label roleLabel = new Label("Lecturer");
            roleLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #7F8C8D;");
            nameCol.getChildren().addAll(nameLabel, roleLabel);

            headerBox.getChildren().addAll(profilePic, nameCol);

            // --- Resources Area ---
            VBox resourcesArea = new VBox(8);
            resourcesArea.setStyle("-fx-padding: 15; -fx-background-color: #FFFFFF; -fx-border-color: #F0F0F0; -fx-border-radius: 8; -fx-background-radius: 8;");
            Label emptyRes = new Label("No resources uploaded yet.");
            emptyRes.setStyle("-fx-text-fill: #95A5A6; -fx-font-style: italic;");
            resourcesArea.getChildren().add(emptyRes);

            teacherBox.getChildren().addAll(headerBox, resourcesArea);

            // --- Add Resources Button (ONLY for Teachers) ---
            if (TEACHER_ROLE.equals(SessionManager.getCurrentUserRole())) {
                Button addResourcesButton = new Button("+ Add Material");
                addResourcesButton.setStyle("-fx-background-color: #D35400; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand;");

                addResourcesButton.setOnAction(event -> {
                    addResourcesButton.setText("Feature coming soon!");
                    addResourcesButton.setDisable(true);
                });

                HBox btnContainer = new HBox();
                btnContainer.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
                btnContainer.getChildren().add(addResourcesButton);
                teacherBox.getChildren().add(btnContainer);
            }

            teacherResourcesContainer.getChildren().add(teacherBox);
        }
    }
}
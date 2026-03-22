package com.syncron.controllers;

import com.syncron.models.User;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class CommonController {
    private static final String[] DUMMY_TEACHER_NAMES = {"Khaled Mahmud Shahriar", "Abdur Rafi", "Md. Mahfuzul Islam"};

    @FXML
    private VBox teacherResourcesContainer;

    @FXML
    public void initialize() {
        loadTeacherResources();
    }

    private void loadTeacherResources() {
        teacherResourcesContainer.getChildren().clear();

        //  ADDED: Safely verify the current user is a teacher from the Session Manager
        boolean isTeacher = false;
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser != null && "TEACHER".equalsIgnoreCase(currentUser.getRole())) {
            isTeacher = true;
        }

        for (String teacherName : DUMMY_TEACHER_NAMES) {
            VBox teacherBox = new VBox(10);

            // --- NEW SPLIT HEADER LOGIC ---
            HBox headerBox = new HBox();
            headerBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            Label prefix = new Label("Resources from ");
            prefix.setStyle("-fx-font-size: 16px; -fx-text-fill: #2C3E50;");

            Hyperlink nameLink = new Hyperlink(teacherName);
            nameLink.setStyle("-fx-font-size: 16px; -fx-text-fill: #3498DB; -fx-padding: 0;");

            headerBox.getChildren().addAll(prefix, nameLink);
            // ------------------------------

            Label uploadsLabel = new Label("Uploads: Links/Files");

            // Add the headerBox instead of the old single hyperlink
            teacherBox.getChildren().addAll(headerBox, uploadsLabel);

            //  FIX: If the boolean above passed, the button is generated
            if (isTeacher) {
                Button addResourcesButton = new Button("Add Resources");
                addResourcesButton.setOnAction(event -> {
                    if (!addResourcesButton.isDisable()) {
                        HBox uploadOptions = new HBox(10);
                        Button uploadFromComputerButton = new Button("Upload from Computer");
                        Button uploadFromDriveButton = new Button("Upload from Drive");
                        uploadOptions.getChildren().addAll(uploadFromComputerButton, uploadFromDriveButton);
                        teacherBox.getChildren().add(uploadOptions);
                        addResourcesButton.setDisable(true);
                    }
                });
                teacherBox.getChildren().add(addResourcesButton);
            }

            teacherResourcesContainer.getChildren().add(teacherBox);
        }
    }
}

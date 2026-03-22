package com.syncron.controllers;

import com.syncron.models.User;
import com.syncron.utils.NavigationManager;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

public class CTAssignmentsController {

    @FXML
    private Button createAssessmentBtn;

    @FXML
    private FlowPane assessmentContainer;

    @FXML
    public void initialize() {
        // FIX 1: Safely grab the actual User object from the SessionManager
        User currentUser = SessionManager.getCurrentUser();

        // FIX 2: Check if they are a Teacher. If not, hide the button
        if (currentUser != null && "TEACHER".equalsIgnoreCase(currentUser.getRole())) {
            createAssessmentBtn.setVisible(true);
            createAssessmentBtn.setManaged(true);

            // Programmatically apply the beautiful rust-orange style to the button
            if (!createAssessmentBtn.getStyleClass().contains("kernel-btn")) {
                createAssessmentBtn.getStyleClass().add("kernel-btn");
            }
        } else {
            // Hide for Students
            createAssessmentBtn.setVisible(false);
            createAssessmentBtn.setManaged(false);
        }

        createAssessmentBtn.setOnAction(event -> {
            if (MainController.instance != null) {
                MainController.instance.setBreadcrumbs("Dashboard", "My Courses", "CSE 105", "CT & Assignments", "New Assessment");
            }
            NavigationManager.switchScreen("assessment_detail.fxml");
        });

        loadDefaultAssessments();
    }

    private void loadDefaultAssessments() {
        assessmentContainer.getChildren().clear();
        assessmentContainer.getChildren().addAll(
                createAssessmentBox("Class Test 1"),
                createAssessmentBox("Class Test 2"),
                createAssessmentBox("Class Test 3"),
                createAssessmentBox("Class Test 4")
        );
    }

    private VBox createAssessmentBox(String title) {
        Label titleLabel = new Label(title);
        // UI FIX: Using the Serif font and brown text for the boxes
        titleLabel.setStyle("-fx-font-family: 'Georgia', serif; -fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #4A2C1A;");

        VBox box = new VBox(titleLabel);
        box.setPadding(new Insets(14));
        box.setPrefSize(170, 90);

        // UI FIX: Applying the warm cream background and subtle shadow
        String defaultStyle = "-fx-background-color: #FFFCF8; -fx-border-color: #E0D5C7; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(74,44,26,0.06), 6, 0, 0, 3);";
        String hoverStyle = "-fx-background-color: #F5EDE3; -fx-border-color: #E0D5C7; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(74,44,26,0.06), 6, 0, 0, 3);";

        box.setStyle(defaultStyle);
        box.setOnMouseEntered(e -> box.setStyle(hoverStyle));
        box.setOnMouseExited(e -> box.setStyle(defaultStyle));

        box.setOnMouseClicked(event -> {
            if (MainController.instance != null) {
                MainController.instance.setBreadcrumbs("Dashboard", "My Courses", "CSE 105", "CT & Assignments", title);
            }
            NavigationManager.switchScreen("assessment_detail.fxml");
        });
        return box;
    }
}
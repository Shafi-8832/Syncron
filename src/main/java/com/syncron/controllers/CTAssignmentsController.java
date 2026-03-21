package com.syncron.controllers;

import com.syncron.utils.NavigationManager;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

public class CTAssignmentsController {
    private static final String STUDENT_ROLE = "STUDENT";

    @FXML
    private Button createAssessmentBtn;

    @FXML
    private FlowPane assessmentContainer;

    @FXML
    public void initialize() {
        if (STUDENT_ROLE.equals(SessionManager.getCurrentUserRole())) {
            createAssessmentBtn.setVisible(false);
            createAssessmentBtn.setManaged(false);
        }

        createAssessmentBtn.setOnAction(event -> {
            MainController.instance.setBreadcrumbs("Dashboard", "My Courses", "CSE 105", "CT & Assignments", "New Assessment");
            NavigationManager.switchScreen("assessment_detail.fxml");
        });
        loadDefaultAssessments();
    }

    private void loadDefaultAssessments() {
        assessmentContainer.getChildren().clear();
        assessmentContainer.getChildren().addAll(
                createAssessmentBox("CT 1"),
                createAssessmentBox("CT 2"),
                createAssessmentBox("CT 3"),
                createAssessmentBox("CT 4")
        );
    }

    private VBox createAssessmentBox(String title) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

        VBox box = new VBox(titleLabel);
        box.setPadding(new Insets(14));
        box.setPrefSize(170, 90);
        box.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #DCDDE1; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand;");
        box.setOnMouseClicked(event -> {
            MainController.instance.setBreadcrumbs("Dashboard", "My Courses", "CSE 105", "CT & Assignments", title);
            NavigationManager.switchScreen("assessment_detail.fxml");
        });
        return box;
    }
}

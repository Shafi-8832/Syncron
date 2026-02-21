package com.syncron.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Controller for the persistent main layout (BorderPane).
 * Sidebar buttons replace only the center content via loadContentView().
 * Supports switching sidebar buttons based on course type (Theory vs Sessional).
 * Decoupled so a Teacher object can later be passed to content views.
 */
public class MainController {

    @FXML private BorderPane mainBorderPane;
    @FXML private Label breadcrumbLabel;
    @FXML private HBox breadcrumbBar;
    @FXML private VBox sidebar;
    @FXML private VBox sidebarButtonContainer;
    @FXML private StackPane contentArea;

    private String courseName = "";
    private String courseType = "theory"; // "theory" or "sessional"

    // Common sidebar items for both theory and sessional
    private static final String[] COMMON_BUTTONS = {"Common"};

    // Theory-specific sidebar items
    private static final String[] THEORY_BUTTONS = {"CT and Assignments", "Weekly Timeline", "Grades", "Participants"};

    // Sessional-specific sidebar items
    private static final String[] SESSIONAL_BUTTONS = {"Offlines", "Onlines", "Weekly Timeline", "Grades", "Participants"};

    // Mapping from button labels to FXML file paths
    private String getFxmlPathForButton(String buttonLabel) {
        return switch (buttonLabel) {
            case "Common" -> "common.fxml";
            case "CT and Assignments" -> "ct_assignments.fxml";
            case "Weekly Timeline" -> "weekly_timeline.fxml";
            case "Grades" -> "grades.fxml";
            case "Participants" -> "participants.fxml";
            case "Offlines" -> "offlines.fxml";
            case "Onlines" -> "onlines.fxml";
            default -> null;
        };
    }

    @FXML
    public void initialize() {
        // Default to theory sidebar; will be reconfigured when setCourseContext is called
        buildSidebar();
    }

    /**
     * Configure the layout for a specific course.
     * @param courseName the display name of the course (e.g., "CSE 108")
     * @param courseType "theory" or "sessional"
     */
    public void setCourseContext(String courseName, String courseType) {
        this.courseName = courseName;
        this.courseType = courseType != null ? courseType.toLowerCase() : "theory";
        buildSidebar();
        // Load the first sidebar item (Common) by default
        loadContentView("common.fxml");
        updateBreadcrumb("Common");
    }

    /**
     * Builds the sidebar buttons based on the current course type.
     */
    private void buildSidebar() {
        sidebarButtonContainer.getChildren().clear();

        // Add common buttons
        for (String label : COMMON_BUTTONS) {
            sidebarButtonContainer.getChildren().add(createSidebarButton(label));
        }

        // Add type-specific buttons
        String[] typeButtons = "sessional".equals(courseType) ? SESSIONAL_BUTTONS : THEORY_BUTTONS;
        for (String label : typeButtons) {
            sidebarButtonContainer.getChildren().add(createSidebarButton(label));
        }
    }

    /**
     * Creates a styled sidebar navigation button.
     */
    private Button createSidebarButton(String label) {
        Button button = new Button(label);
        button.getStyleClass().add("nav-btn");
        button.setMaxWidth(Double.MAX_VALUE);

        button.setOnAction(event -> {
            String fxmlPath = getFxmlPathForButton(label);
            if (fxmlPath != null) {
                loadContentView(fxmlPath);
                updateBreadcrumb(label);
            }
        });

        return button;
    }

    /**
     * Loads an FXML view into the center content area of the BorderPane.
     * Only replaces the center node — the sidebar and breadcrumb remain persistent.
     *
     * @param fxmlPath the FXML file name (relative to /com/syncron/views/)
     */
    public void loadContentView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/syncron/views/" + fxmlPath));
            Node view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            // Show error message in the content area
            Label errorLabel = new Label("Could not load view: " + fxmlPath);
            errorLabel.setStyle("-fx-text-fill: #E74C3C; -fx-font-size: 14px;");
            contentArea.getChildren().setAll(errorLabel);
            e.printStackTrace();
        }
    }

    /**
     * Updates the breadcrumb label to reflect the current navigation path.
     */
    private void updateBreadcrumb(String currentSection) {
        String path;
        if (courseName != null && !courseName.isEmpty()) {
            path = "Dashboard / My Courses / " + courseName + " / " + currentSection;
        } else {
            path = "Dashboard / " + currentSection;
        }
        breadcrumbLabel.setText(path);
    }

    /**
     * Handles the "Back to Home" button action.
     */
    @FXML
    private void handleBackToHome() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/syncron/views/home.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) mainBorderPane.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

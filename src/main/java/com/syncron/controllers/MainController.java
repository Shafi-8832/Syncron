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
    @FXML private HBox breadcrumbBar;
    @FXML private VBox sidebar;
    @FXML private VBox sidebarButtonContainer;
    @FXML private StackPane contentArea;
    @FXML private HBox courseHeaderBox;
    @FXML private Label courseHeaderLabel;
    @FXML private Label courseTypeFlair;

    private String courseName = "";
    private String courseType = "theory"; // "theory" or "sessional"
    private Button activeButton;

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
        updateCourseHeader();
        // Load the first sidebar item (Common) by default and mark it active
        if (!sidebarButtonContainer.getChildren().isEmpty()) {
            Button firstButton = (Button) sidebarButtonContainer.getChildren().get(0);
            setActiveButton(firstButton);
        }
        loadContentView("common.fxml");
        updateBreadcrumb("Common");
    }

    /**
     * Updates the persistent course header box with course code/name and type flair.
     */
    private void updateCourseHeader() {
        if (courseName != null && !courseName.isEmpty()) {
            courseHeaderLabel.setText(courseName);
            courseHeaderBox.setVisible(true);
            courseHeaderBox.setManaged(true);

            // Style the flair based on course type
            if ("sessional".equals(courseType)) {
                courseTypeFlair.setText("Sessional");
                courseTypeFlair.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 3 10; "
                        + "-fx-background-radius: 5; -fx-background-color: #9B59B6; -fx-text-fill: white;");
            } else {
                courseTypeFlair.setText("Theory");
                courseTypeFlair.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 3 10; "
                        + "-fx-background-radius: 5; -fx-background-color: #3498DB; -fx-text-fill: white;");
            }
        } else {
            courseHeaderBox.setVisible(false);
            courseHeaderBox.setManaged(false);
        }
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
                setActiveButton(button);
                loadContentView(fxmlPath);
                updateBreadcrumb(label);
            }
        });

        return button;
    }

    /**
     * Highlights the active sidebar button and removes highlight from the previous one.
     */
    private void setActiveButton(Button button) {
        if (activeButton != null) {
            activeButton.getStyleClass().remove("nav-btn-active");
        }
        button.getStyleClass().add("nav-btn-active");
        activeButton = button;
    }

    /**
     * Loads an FXML view into the center content area of the BorderPane.
     * Only replaces the center node — the sidebar, breadcrumb, and course header remain persistent.
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
     * Updates the breadcrumb bar with interactive, clickable segments.
     * Each segment acts like a button that navigates to that level.
     */
    private void updateBreadcrumb(String section) {
        breadcrumbBar.getChildren().clear();

        // Segment 1: "Dashboard" — always present, clickable → goes back to home
        breadcrumbBar.getChildren().add(createBreadcrumbSegment("Dashboard", this::handleBackToHome));

        if (courseName != null && !courseName.isEmpty()) {
            // Separator
            breadcrumbBar.getChildren().add(createBreadcrumbSeparator());

            // Segment 2: "My Courses" — clickable → goes back to home
            breadcrumbBar.getChildren().add(createBreadcrumbSegment("My Courses", this::handleBackToHome));

            // Separator
            breadcrumbBar.getChildren().add(createBreadcrumbSeparator());

            // Segment 3: Course Name — clickable → reloads Common view
            breadcrumbBar.getChildren().add(createBreadcrumbSegment(courseName, () -> {
                loadContentView("common.fxml");
                updateBreadcrumb("Common");
                // Highlight the Common button
                if (!sidebarButtonContainer.getChildren().isEmpty()) {
                    Button firstButton = (Button) sidebarButtonContainer.getChildren().get(0);
                    setActiveButton(firstButton);
                }
            }));

            if (section != null && !section.isEmpty()) {
                // Separator
                breadcrumbBar.getChildren().add(createBreadcrumbSeparator());

                // Segment 4: Current section — non-clickable (current page)
                Label sectionLabel = new Label(section);
                sectionLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #2C3E50; -fx-font-weight: bold;");
                breadcrumbBar.getChildren().add(sectionLabel);
            }
        }
    }

    /**
     * Creates a clickable breadcrumb segment label.
     */
    private Label createBreadcrumbSegment(String text, Runnable onClick) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 14px; -fx-text-fill: #3498DB; -fx-cursor: hand;");
        label.setOnMouseEntered(e -> label.setStyle("-fx-font-size: 14px; -fx-text-fill: #2980B9; -fx-cursor: hand; -fx-underline: true;"));
        label.setOnMouseExited(e -> label.setStyle("-fx-font-size: 14px; -fx-text-fill: #3498DB; -fx-cursor: hand;"));
        label.setOnMouseClicked(e -> onClick.run());
        return label;
    }

    /**
     * Creates a breadcrumb separator (" / ").
     */
    private Label createBreadcrumbSeparator() {
        Label sep = new Label(" / ");
        sep.setStyle("-fx-font-size: 14px; -fx-text-fill: #BDC3C7;");
        return sep;
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

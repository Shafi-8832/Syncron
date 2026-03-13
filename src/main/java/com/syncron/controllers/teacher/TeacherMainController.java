package com.syncron.controllers.teacher;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

import java.io.IOException;

/**
 * Controller for the Teacher Master Layout (teacher_main_layout.fxml).
 * Manages the persistent header and swaps sub-pages into the contentArea.
 */
public class TeacherMainController {

    @FXML private BorderPane rootPane;
    @FXML private StackPane contentArea;
    @FXML private Label teacherNameLabel;

    @FXML
    public void initialize() {
        // Load the Teacher Home page into the content area by default
        loadContent("/com/syncron/views/teacher/teacher_home.fxml");
    }

    /**
     * Loads an FXML view into the center contentArea StackPane.
     * @param fxmlPath absolute resource path to the FXML file
     */
    public void loadContent(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to load view: " + fxmlPath);
        }
    }

    /**
     * Sets the teacher name displayed in the header profile section.
     * @param name the teacher's display name
     */
    public void setTeacherName(String name) {
        if (teacherNameLabel != null && name != null) {
            teacherNameLabel.setText(name);
        }
    }
}

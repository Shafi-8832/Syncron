package com.syncron.controllers.teacher;

import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

/**
 * Controller for the Teacher Home page (teacher_home.fxml).
 * Manages the course overview cards and pinned items.
 */
public class TeacherHomeController {

    @FXML private VBox courseCardContainer;

    @FXML
    public void initialize() {
        // Course cards are currently defined as static FXML content.
        // This controller can be extended to dynamically populate
        // courseCardContainer from the database in the future.
    }
}

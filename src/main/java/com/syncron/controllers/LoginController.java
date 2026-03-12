package com.syncron.controllers;

import com.syncron.utils.DatabaseHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.io.IOException;

public class LoginController {

    // --- UI Elements ---
    @FXML private TextField idField;
    @FXML private PasswordField passwordField;
    @FXML private TextField visiblePasswordField; // For the "Show Password" toggle
    @FXML private CheckBox showPasswordCheck;
    @FXML private Label errorLabel;

    // --- Action Methods ---

    /**
     * The main gateway logic. Authenticates the user and routes them
     * to the correct dashboard based on their RBAC role.
     */
    @FXML
    private void handleLoginClick() {
        // You already know how to write this!
        // 1. Get ID and Password.
        // 2. Call DatabaseHandler.authenticateUser(id, password).
        // 3. Switch scene based on the returned role (ADMIN, TEACHER, STUDENT).
        String id = idField.getText();

        // Smart Check: read from whichever password field is currently active
        String password = showPasswordCheck.isSelected() ?
                visiblePasswordField.getText() :
                passwordField.getText();

        if (id.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please enter both ID and Password.");
            return;
        }

        // 1.  authentication: ask the DB for user's role
        String userRole = DatabaseHandler.authenticateUser(id, password);

        if (userRole != null) {
            // Routing
            try {
                String targetFxml = "";

                switch (userRole) {
                    case "ADMIN":
                        targetFxml = "/com/syncron/views/admin_dashboard.fxml"; // We will build this next!
                        break;
                    case "TEACHER":
                        targetFxml = "/com/syncron/views/teacher_main_layout.fxml";
                        break;
                    case "STUDENT":
                        targetFxml = "/com/syncron/views/home.fxml"; // Your existing student view
                        break;
                    default:
                        errorLabel.setText("System Error: Unknown Role.");
                        return;
                }
                // 3. Switch Scenes
                FXMLLoader loader = new FXMLLoader(getClass().getResource(targetFxml));
                Parent root = loader.load();

                Stage stage = (Stage) idField.getScene().getWindow();
                stage.setScene(new Scene(root, 1000, 700));
                stage.centerOnScreen();
            }
            catch (IOException e) {
                e.printStackTrace();
                errorLabel.setText("Error loading the dashboard UI.");
            }
        }
        else {
            // FAILURE: The Bouncer says no.
            errorLabel.setText("Invalid ID or Password!");
        }
    }

    /**
     * Toggles the visibility of the password between hidden (dots) and plain text.
     */
    @FXML
    private void handleShowPassword() {

        if (showPasswordCheck.isSelected()) {

            visiblePasswordField.setText(passwordField.getText());
            visiblePasswordField.setVisible(true);
            visiblePasswordField.setManaged(true);

            passwordField.setVisible(false);
            passwordField.setManaged(false);
        } else {
            passwordField.setText(visiblePasswordField.getText());
            passwordField.setVisible(true);
            passwordField.setManaged(true);

            visiblePasswordField.setVisible(false);
            visiblePasswordField.setManaged(false);
        }
    }

    /**
     * Placeholder for future functionality
     */
    @FXML
    private void handleForgetPassword() {
        errorLabel.setText("Forget Password feature coming soon!");
        errorLabel.setStyle("-fx-text-fill: #3498DB;"); // Make it blue instead of red for info
    }

    /**
     * Placeholder for future functionality
     */
    @FXML
    private void handleGoBack() {
        System.out.println("Returning to main portal...");
    }
}
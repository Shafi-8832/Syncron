package com.syncron.controllers;

import com.syncron.utils.DatabaseHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    @FXML private TextField idField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    @FXML
    private void handleLoginClick() {
        String id = idField.getText();
        String password = passwordField.getText();

        // 1. Ask the database if the credentials are valid
        if (DatabaseHandler.verifyLogin(id, password)) {
            // 2. SUCCESS! Load the dashboard
            try {
                // Grab the Dashboard FXML
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/syncron/views/dashboard.fxml"));
                Parent root = loader.load();

                // Get the current window (Stage) from the login button/text field
                Stage stage = (Stage) idField.getScene().getWindow();

                // Swap the scene to the Dashboard
                stage.setScene(new Scene(root, 900, 600));
                stage.centerOnScreen();

            } catch (IOException e) {
                e.printStackTrace();
                errorLabel.setText("Error loading dashboard.");
            }
        } else {
            // 3. FAILURE! Show an error message
            errorLabel.setText("Invalid ID or Password!");
        }
    }
}
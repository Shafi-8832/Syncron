package com.syncron.controllers.admin;

import com.syncron.utils.DatabaseHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class AdminController {

    // --- UI Elements ---
    @FXML private TextArea terminalLog;

    /**
     * This method is automatically called by JavaFX after the FXML is loaded.
     * Perfect for setting up our initial "boot sequence".
     */
    @FXML
    public void initialize() {
        terminalLog.setText("[SYS] KERNEL v1.0.0 INITIALIZED...\n");
        log("Secure SQLite Connection Established.");
        log("Root access granted. Awaiting commands.");
    }

    /**
     * A helper method to print timestamped messages to the hacker terminal.
     */
    private void log(String message) {
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        terminalLog.appendText("[" + time + "] " + message + "\n");
    }

    // --- Navigation Handlers ---

    @FXML
    private void handleDashboard() {
        log("CMD: Refreshing System Overview...");
        // Later: Code to refresh the stats counters
    }

    @FXML
    private void handleManageUsers() {
        log("CMD: Accessing User Registry... (UI Module pending)");
        // Later: Swap the center screen to the User Management table
    }

    @FXML
    private void handleManageCourses() {
        log("CMD: Accessing Course Database... (UI Module pending)");
        // Later: Swap the center screen to the Course Creation form
        // NOW
        String result = DatabaseHandler.injectLevel1Term2();
        log(result);
    }

    @FXML
    private void handleManageSemesters() {
        log("CMD: Accessing Semester Config... (UI Module pending)");
        // Later: Swap the center screen to the Semester Management form
    }

    @FXML
    private void handleLogout() {
        log("Disconnecting secure session...");
        try {
            // Load the login screen
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/syncron/views/authentication/login.fxml"));
            Parent root = loader.load();

            // Grab the current window using the terminalLog node
            Stage stage = (Stage) terminalLog.getScene().getWindow();
            stage.setScene(new Scene(root, 1000, 700));
            stage.centerOnScreen();

        } catch (IOException e) {
            log("ERR: Failed to disconnect cleanly. " + e.getMessage());
        }
    }
}
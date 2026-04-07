package com.syncron.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextInputDialog;
import java.awt.Desktop;
import java.net.URI;
import java.util.Optional;

public class SemesterDetailsController {

    @FXML private Button editLinksBtn;

    // In a real app, these fetch from DB. For the presentation, we store them in memory.
    private String routineUrl = "https://www.buet.ac.bd/web/#/home";
    private String calendarUrl = "https://www.buet.ac.bd/web/#/home";
    private String bookletUrl = "https://www.buet.ac.bd/web/#/home";

    @FXML
    public void initialize() {
        // Only show Edit button to Admins and Teachers
        if (SessionManager.getCurrentUser() != null) {
            String role = SessionManager.getCurrentUser().getRole();
            if ("TEACHER".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role)) {
                editLinksBtn.setVisible(true);
            }
        }
    }

    @FXML private void openRoutine() { openLink(routineUrl); }
    @FXML private void openCalendar() { openLink(calendarUrl); }
    @FXML private void openBooklet() { openLink(bookletUrl); }

    private void openLink(String url) {
        try { Desktop.getDesktop().browse(new URI(url)); }
        catch (Exception e) { System.out.println("Could not open: " + url); }
    }

    @FXML
    private void handleEditLinks() {
        // Simple sequential dialog for presentation speed
        routineUrl = promptForUrl("Class Routine URL", routineUrl);
        calendarUrl = promptForUrl("Academic Calendar URL", calendarUrl);
        bookletUrl = promptForUrl("Undergraduate Booklet URL", bookletUrl);
    }

    private String promptForUrl(String title, String currentUrl) {
        TextInputDialog dialog = new TextInputDialog(currentUrl);
        dialog.setTitle("Edit Resource Link");
        dialog.setHeaderText("Update the " + title);
        dialog.setContentText("URL:");
        Optional<String> result = dialog.showAndWait();
        return result.orElse(currentUrl);
    }

    @FXML
    private void goBackToDashboard(ActionEvent event) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/syncron/views/home.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
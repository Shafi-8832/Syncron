package com.syncron.controllers.shared;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;


// This class is for the semester details you see in a student dashboard
public class SemesterDetailsController {

    @FXML private Button backBtn;
    @FXML private Label daysLeftLabel;

    @FXML
    public void initialize() {
        // --- MOCK DATA (replace this with DatabaseHandler later) ---
        LocalDate today = LocalDate.now();
        LocalDate examDate = LocalDate.of(2026, 4, 25); // April 25, 2026

        // Calculate the difference
        long daysBetween = ChronoUnit.DAYS.between(today, examDate);

        // Prevent negative numbers if exam passed
        if (daysBetween < 0) daysBetween = 0;

        // Update the BIG TEXT
        daysLeftLabel.setText(String.valueOf(daysBetween));

        // --- BACK BUTTON LOGIC ---
        backBtn.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/syncron/views/student/home.fxml"));
                Parent root = loader.load();
                Stage stage = (Stage) backBtn.getScene().getWindow();
                stage.getScene().setRoot(root);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }
}
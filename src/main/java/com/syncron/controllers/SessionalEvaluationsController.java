package com.syncron.controllers;

import com.syncron.models.User;
import com.syncron.utils.DatabaseHandler;
import com.syncron.utils.NavigationManager;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class SessionalEvaluationsController {

    @FXML private Button offlineToggleBtn;
    @FXML private Button onlineToggleBtn;

    @FXML private TextField titleInput;
    @FXML private DatePicker datePicker;
    @FXML private TextField timeInput;       // Used for Due Time (Offline) or Opening Time (Online)
    @FXML private TextField endTimeInput;    // NEW: Used for Closing Time (Online)
    @FXML private TextField marksInput;
    @FXML private TextArea descriptionInput;

    @FXML private Label dateLabel;
    @FXML private Label timeLabel;

    @FXML private VBox endTimeBox;           // NEW: The container for Closing Time
    @FXML private VBox targetSectionContainer;
    @FXML private HBox sectionBox;

    @FXML private Label fileNameLabel;

    private String currentType = "OFFLINE";
    private File selectedPdfFile = null;

    @FXML
    public void initialize() {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null || !"TEACHER".equalsIgnoreCase(currentUser.getRole())) {
            NavigationManager.switchScreen("home.fxml");
            return;
        }
        setOfflineMode();
    }

    @FXML
    private void setOfflineMode() {
        currentType = "OFFLINE";
        offlineToggleBtn.getStyleClass().setAll("button", "kernel-toggle-active");
        onlineToggleBtn.getStyleClass().setAll("button", "kernel-toggle-inactive");

        dateLabel.setText("Deadline Date");
        timeLabel.setText("Due Time");

        // Hide Sections & End Time for Offlines
        targetSectionContainer.setVisible(false);
        targetSectionContainer.setManaged(false);
        endTimeBox.setVisible(false);
        endTimeBox.setManaged(false);
    }

    @FXML
    private void setOnlineMode() {
        currentType = "ONLINE";
        onlineToggleBtn.getStyleClass().setAll("button", "kernel-toggle-active");
        offlineToggleBtn.getStyleClass().setAll("button", "kernel-toggle-inactive");

        dateLabel.setText("Exam Date");
        timeLabel.setText("Opening Time");

        // Show Sections & End Time for Onlines
        targetSectionContainer.setVisible(true);
        targetSectionContainer.setManaged(true);
        endTimeBox.setVisible(true);
        endTimeBox.setManaged(true);
    }

    @FXML
    private void handleAttachFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Assessment Document");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

        Stage stage = (Stage) titleInput.getScene().getWindow();
        selectedPdfFile = fileChooser.showOpenDialog(stage);

        if (selectedPdfFile != null) {
            fileNameLabel.setText(selectedPdfFile.getName());
            fileNameLabel.setStyle("-fx-text-fill: #2ECC71; -fx-font-weight: bold;");
        } else {
            fileNameLabel.setText("No file selected");
            fileNameLabel.setStyle("-fx-text-fill: #95A5A6; -fx-font-style: italic;");
        }
    }

    @FXML
    private void handlePublish() {
        String title = titleInput.getText();
        String marks = marksInput.getText();
        LocalDate date = datePicker.getValue();
        String desc = descriptionInput.getText();

        String targetSections = "All";

        if (currentType.equals("ONLINE")) {
            StringBuilder sections = new StringBuilder();
            for (Node node : sectionBox.getChildren()) {
                if (node instanceof CheckBox && ((CheckBox) node).isSelected()) {
                    sections.append(((CheckBox) node).getText()).append(",");
                }
            }
            if (sections.length() > 0) {
                targetSections = sections.substring(0, sections.length() - 1);
            } else {
                new Alert(Alert.AlertType.WARNING, "Please select at least one Target Section for the Lab Test!").show();
                return;
            }
        }

        if (title.isEmpty() || date == null || timeInput.getText().isEmpty() || (currentType.equals("ONLINE") && endTimeInput.getText().isEmpty())) {
            new Alert(Alert.AlertType.WARNING, "Please fill out all required Date and Time fields!").show();
            return;
        }

        // 👉 TIME LOGIC ENGINE
        String startDateStr;
        String startTimeStr;
        String deadlineDateStr = date.toString();
        String deadlineTimeStr;

        if (currentType.equals("OFFLINE")) {
            // For Offlines, Opening Time is exactly NOW. Deadline is what they typed.
            startDateStr = LocalDate.now().toString();
            startTimeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            deadlineTimeStr = timeInput.getText();
        } else {
            // For Onlines, Opening Time is timeInput, Closing time is endTimeInput.
            startDateStr = date.toString(); // Exam happens on the same day
            startTimeStr = timeInput.getText();
            deadlineTimeStr = endTimeInput.getText();
        }

        String courseCode = SessionManager.getCurrentCourseCode();
        String teacherId = SessionManager.getCurrentUser().getId();
        String savedFilePath = "None";

        if (selectedPdfFile != null) {
            try {
                File uploadDir = new File("uploads");
                if (!uploadDir.exists()) uploadDir.mkdir();

                String newFileName = courseCode.replace(" ", "") + "_" + teacherId + "_" + System.currentTimeMillis() + ".pdf";
                File destFile = new File(uploadDir, newFileName);

                Files.copy(selectedPdfFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                savedFilePath = destFile.getPath();
            } catch (Exception e) {
                e.printStackTrace();
                new Alert(Alert.AlertType.ERROR, "Failed to upload file!").show();
                return;
            }
        }

        // 👉 DATABASE INJECTION
        try (java.sql.Connection conn = DatabaseHandler.connect();
             java.sql.Statement alterStmt = conn.createStatement()) {

            // Add the new start_date and start_time columns dynamically if they don't exist yet!
            try { alterStmt.execute("ALTER TABLE evaluations ADD COLUMN start_date TEXT"); } catch (Exception ignore) {}
            try { alterStmt.execute("ALTER TABLE evaluations ADD COLUMN start_time TEXT"); } catch (Exception ignore) {}
            try { alterStmt.execute("ALTER TABLE evaluations ADD COLUMN file_path TEXT"); } catch (Exception ignore) {}

            String insertSql = "INSERT INTO evaluations (course_code, title, type, target_sections, total_marks, start_date, start_time, deadline_date, deadline_time, description, creator_id, file_path) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            java.sql.PreparedStatement pstmt = conn.prepareStatement(insertSql);
            pstmt.setString(1, courseCode);
            pstmt.setString(2, title);
            pstmt.setString(3, currentType);
            pstmt.setString(4, targetSections);
            pstmt.setString(5, marks);
            pstmt.setString(6, startDateStr);   // Opening Date
            pstmt.setString(7, startTimeStr);   // Opening Time
            pstmt.setString(8, deadlineDateStr); // Closing Date
            pstmt.setString(9, deadlineTimeStr); // Closing Time
            pstmt.setString(10, desc);
            pstmt.setString(11, teacherId);
            pstmt.setString(12, savedFilePath);
            pstmt.executeUpdate();

            // Auto-Announce
            String announcementText = "🔔 New " + currentType + " Published: " + title + " (Due: " + deadlineDateStr + " " + deadlineTimeStr + ")";
            String announceSql = "INSERT INTO announcements (course_code, message, timestamp, creator_id) VALUES (?, ?, ?, ?)";
            java.sql.PreparedStatement astmt = conn.prepareStatement(announceSql);
            astmt.setString(1, courseCode);
            astmt.setString(2, announcementText);
            astmt.setString(3, LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")));
            astmt.setString(4, teacherId);
            astmt.executeUpdate();

            Alert success = new Alert(Alert.AlertType.INFORMATION, "Assessment Published Successfully!");
            success.showAndWait();

            // Route back
            if (currentType.equals("OFFLINE")) {
                NavigationManager.switchScreen("offlines.fxml");
            } else {
                NavigationManager.switchScreen("onlines.fxml");
            }

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to publish assessment. Database error.").show();
        }
    }
}
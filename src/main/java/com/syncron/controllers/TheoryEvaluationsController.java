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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class TheoryEvaluationsController {

    @FXML private TextField titleInput;
    @FXML private DatePicker datePicker;

    @FXML private VBox targetSectionContainer;
    @FXML private HBox sectionBox;

    @FXML private ComboBox<String> startHour, startMin, startAmPm;

    @FXML private VBox durationBox;        // Replaced endTimeBox
    @FXML private TextField durationInput; // The new 45 mins input

    @FXML private Button cancelBtn;
    @FXML private Button publishBtn;
    //

    @FXML private TextField marksInput;
    @FXML private TextArea descriptionInput;

    @FXML private Label dateLabel;
    @FXML private Label timeLabel;

    @FXML private VBox endTimeBox;           // NEW: The container for Closing Time

    @FXML private Button assignmentToggleBtn;
    @FXML private Button ctToggleBtn;
    // don't need section boxes for Theory

    @FXML private Label fileNameLabel;

    private String currentType = "ASSIGNMENT";
    private File selectedPdfFile = null;

    private String editId = null;


    @FXML
    public void initialize() {
        System.out.println("--- 🚀 THEORY ENGINE STARTING ---");
        try {
            setupTimeSpinners();

            com.syncron.models.User currentUser = SessionManager.getCurrentUser();
            if (currentUser == null || !"TEACHER".equalsIgnoreCase(currentUser.getRole())) {
                NavigationManager.switchScreen("home.fxml");
                return;
            }

            editId = SessionManager.getEditEvaluationId();
            System.out.println("🔍 Fetched Edit ID: [" + editId + "]");

            if (editId != null && !editId.trim().isEmpty()) {
                System.out.println("✅ Edit ID found! Entering Edit Mode.");

                // Safe checks to prevent NullPointerExceptions!
                if (cancelBtn != null) {
                    cancelBtn.setVisible(true);
                    cancelBtn.setManaged(true);
                } else {
                    System.out.println("❌ ERROR: cancelBtn is missing from FXML!");
                }

                if (publishBtn != null) publishBtn.setText("Update Assessment");

                loadExistingDataForEdit();
                SessionManager.setEditEvaluationId(null);
            } else {
                System.out.println("⚠️ No Edit ID. Entering Blank Create Mode.");
                setAssignmentMode();
            }
        } catch (Exception e) {
            System.out.println("❌ CRITICAL CRASH IN INITIALIZE:");
            e.printStackTrace();
        }
    }


    @FXML
    private void loadExistingDataForEdit() {
        try (Connection conn = DatabaseHandler.connect();
             PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM evaluations WHERE id = ?")) {

            pstmt.setString(1, editId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                // 1. Restore text fields
                titleInput.setText(rs.getString("title"));
                marksInput.setText(rs.getString("total_marks"));
                descriptionInput.setText(rs.getString("description"));

                String type = rs.getString("type");

                // 2. Set mode correctly
                if ("CT".equals(type)) {
                    setCtMode();
                } else {
                    setAssignmentMode();
                }

                // 3. 👉 RESTORE DATES AND TIMES (This was missing from your file!)
                try {
                    String dateStr = "CT".equals(type) ? rs.getString("start_date") : rs.getString("deadline_date");
                    if (dateStr != null && !dateStr.isEmpty()) {
                        datePicker.setValue(LocalDate.parse(dateStr));
                    }

                    String timeStr = "CT".equals(type) ? rs.getString("start_time") : rs.getString("deadline_time");
                    if (timeStr != null && !timeStr.isEmpty()) {
                        LocalTime time = LocalTime.parse(timeStr);
                        int hour = time.getHour();
                        String amPm = hour >= 12 ? "PM" : "AM";

                        if (hour == 0) hour = 12;
                        else if (hour > 12) hour -= 12;

                        startHour.setValue(String.format("%02d", hour));
                        startMin.setValue(String.format("%02d", time.getMinute()));
                        startAmPm.setValue(amPm);
                    }

                    // 4. Calculate duration for CTs
                    if ("CT".equals(type)) {
                        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                        LocalDateTime sTime = LocalDateTime.parse(rs.getString("start_date") + " " + rs.getString("start_time"), fmt);
                        LocalDateTime eTime = LocalDateTime.parse(rs.getString("deadline_date") + " " + rs.getString("deadline_time"), fmt);
                        long mins = java.time.temporal.ChronoUnit.MINUTES.between(sTime, eTime);
                        durationInput.setText(String.valueOf(mins));
                    }
                } catch (Exception timeEx) {
                    System.out.println("Could not parse dates properly, skipping autofill.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void setAssignmentMode() {
        currentType = "ASSIGNMENT";
        assignmentToggleBtn.getStyleClass().setAll("button", "kernel-toggle-active");
        ctToggleBtn.getStyleClass().setAll("button", "kernel-toggle-inactive");

        dateLabel.setText("Deadline Date");
        timeLabel.setText("Due Time");

        durationBox.setVisible(false);
        durationBox.setManaged(false);

        // set default values for assignments
        marksInput.setText("20");
        durationInput.clear();
    }

    @FXML
    private void setCtMode() {
        currentType = "CT";
        ctToggleBtn.getStyleClass().setAll("button", "kernel-toggle-active");
        assignmentToggleBtn.getStyleClass().setAll("button", "kernel-toggle-inactive");

        dateLabel.setText("Exam Date");
        timeLabel.setText("Opening Time");

        durationBox.setVisible(true);
        durationBox.setManaged(true);

        // set default values for class tests
        durationInput.setText("25");
        marksInput.setText("20");
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

    @FXML // Gemini
    private void handleCancel() {
        NavigationManager.switchScreen("ct_assignments.fxml");
    }

    @FXML
    private void goBack() {
        handleCancel();
    }

    @FXML
    private void handlePublish() {
        String title = titleInput.getText();
        String marks = marksInput.getText();
        LocalDate date = datePicker.getValue();
        String desc = descriptionInput.getText();

        // FIX 1: Must be initialized so the compiler doesn't panic on Assignments
        String targetSections = "All";

        if (currentType.equals("CT")) {
            StringBuilder sections = new StringBuilder();
            for (Node node : sectionBox.getChildren()) {
                if (node instanceof CheckBox && ((CheckBox) node).isSelected()) {
                    sections.append(((CheckBox) node).getText()).append(",");
                }
            }

            if (sections.length() > 0) {
                targetSections = sections.substring(0, sections.length() - 1);
            } else {
                new Alert(Alert.AlertType.WARNING, "Please select at least one Target Section!").show();
                return;
            }
        }

        StringBuilder missingFields = new StringBuilder();
        if (title.isEmpty()) missingFields.append("• Assessment Title\n");
        if (date == null) missingFields.append("• Deadline Date\n");

        boolean isStartMissing = startHour.getValue() == null || startMin.getValue() == null || startAmPm.getValue() == null;
        if (isStartMissing) {
            missingFields.append(currentType.equals("ASSIGNMENT") ? "• Due Time\n" : "• Opening Time\n");
        }

        if (currentType.equals("CT")) {
            if (durationInput.getText().isEmpty()) {
                missingFields.append("• Duration (Mins)\n");
            } else {
                try {
                    Integer.parseInt(durationInput.getText());
                } catch (NumberFormatException e) {
                    new Alert(Alert.AlertType.WARNING, "Duration must be a valid number!").show();
                    return;
                }
            }
        }

        if (missingFields.length() > 0) {
            new Alert(Alert.AlertType.WARNING, "Please fill out the following missing fields:\n\n" + missingFields.toString()).show();
            return;
        }

        String startDateStr;
        String startTimeStr;
        String deadlineDateStr;
        String deadlineTimeStr;

        if (currentType.equals("ASSIGNMENT")) {
            startDateStr = LocalDate.now().toString();
            startTimeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            deadlineDateStr = date.toString();
            deadlineTimeStr = get24HourTime(startHour.getValue(), startMin.getValue(), startAmPm.getValue());
        } else {
            startDateStr = date.toString();
            startTimeStr = get24HourTime(startHour.getValue(), startMin.getValue(), startAmPm.getValue());

            LocalDateTime startDT = LocalDateTime.of(date, LocalTime.parse(startTimeStr));
            int durationMins = Integer.parseInt(durationInput.getText());
            LocalDateTime endDT = startDT.plusMinutes(durationMins);

            deadlineDateStr = endDT.toLocalDate().toString();
            deadlineTimeStr = endDT.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));
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

        boolean successFlag = false;

        try (java.sql.Connection conn = DatabaseHandler.connect();
             java.sql.Statement alterStmt = conn.createStatement()) {

            try { alterStmt.execute("ALTER TABLE evaluations ADD COLUMN start_date TEXT"); } catch (Exception ignore) {}
            try { alterStmt.execute("ALTER TABLE evaluations ADD COLUMN start_time TEXT"); } catch (Exception ignore) {}
            try { alterStmt.execute("ALTER TABLE evaluations ADD COLUMN file_path TEXT"); } catch (Exception ignore) {}

            if (editId != null && !editId.isEmpty()) {
                String updateSql;
                if (selectedPdfFile != null) {
                    updateSql = "UPDATE evaluations SET title=?, type=?, target_sections=?, total_marks=?, start_date=?, start_time=?, deadline_date=?, deadline_time=?, description=?, file_path=? WHERE id=?";
                } else {
                    updateSql = "UPDATE evaluations SET title=?, type=?, target_sections=?, total_marks=?, start_date=?, start_time=?, deadline_date=?, deadline_time=?, description=? WHERE id=?";
                }

                java.sql.PreparedStatement pstmt = conn.prepareStatement(updateSql);
                pstmt.setString(1, title);
                pstmt.setString(2, currentType);
                pstmt.setString(3, targetSections);
                pstmt.setString(4, marks);
                pstmt.setString(5, startDateStr);
                pstmt.setString(6, startTimeStr);
                pstmt.setString(7, deadlineDateStr);
                pstmt.setString(8, deadlineTimeStr);
                pstmt.setString(9, desc);

                if (selectedPdfFile != null) {
                    pstmt.setString(10, savedFilePath);
                    pstmt.setString(11, editId);
                } else {
                    pstmt.setString(10, editId);
                }

                pstmt.executeUpdate();
                new Alert(Alert.AlertType.INFORMATION, "Assessment Updated Successfully!").showAndWait();
                successFlag = true;

            } else {
                String insertSql = "INSERT INTO evaluations (course_code, title, type, target_sections, total_marks, start_date, start_time, deadline_date, deadline_time, description, creator_id, file_path) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

                java.sql.PreparedStatement pstmt = conn.prepareStatement(insertSql);
                pstmt.setString(1, courseCode);
                pstmt.setString(2, title);
                pstmt.setString(3, currentType);
                pstmt.setString(4, targetSections);
                pstmt.setString(5, marks);
                pstmt.setString(6, startDateStr);
                pstmt.setString(7, startTimeStr);
                pstmt.setString(8, deadlineDateStr);
                pstmt.setString(9, deadlineTimeStr);
                pstmt.setString(10, desc);
                pstmt.setString(11, teacherId);
                pstmt.setString(12, savedFilePath);
                pstmt.executeUpdate();

                String announcementText = "🔔 New " + currentType + " Published: " + title + " (Due: " + deadlineDateStr + " " + deadlineTimeStr + ")";
                String announceSql = "INSERT INTO announcements (course_code, message, timestamp, creator_id) VALUES (?, ?, ?, ?)";
                try (java.sql.PreparedStatement astmt = conn.prepareStatement(announceSql)) {
                    astmt.setString(1, courseCode);
                    astmt.setString(2, announcementText);
                    astmt.setString(3, LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")));
                    astmt.setString(4, teacherId);
                    astmt.executeUpdate();
                }

                Alert success = new Alert(Alert.AlertType.INFORMATION, "Assessment Published Successfully!");
                success.showAndWait();
                successFlag = true;
            }

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to publish assessment. Database error.").show();
        }

        // FIX 2: Safely placed outside the try-catch block
        if (successFlag) {
            NavigationManager.switchScreen("ct_assignments.fxml");
        }
    }

    // INJECTS DATA INTO DROPDOWNS
    private void setupTimeSpinners() {
        for (int i = 1; i <= 12; i++) {
            String h = String.format("%02d", i);
            startHour.getItems().add(h);
        }
        for (int i = 0; i < 60; i+=5) {
            String m = String.format("%02d", i);
            startMin.getItems().add(m);
        }
        startAmPm.getItems().addAll("AM", "PM");
        startHour.setValue("11"); startMin.setValue("59"); startAmPm.setValue("PM");
    }

    // CONVERTS 1:53 PM to 13:53 FOR THE DATABASE
    private String get24HourTime(String h, String m, String amPm) {
        int hour = Integer.parseInt(h);
        if (amPm.equals("PM") && hour != 12) hour += 12;
        if (amPm.equals("AM") && hour == 12) hour = 0;
        return String.format("%02d:%s", hour, m);
    }
}
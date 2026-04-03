package com.syncron.controllers;

import com.syncron.models.User;
import com.syncron.utils.DatabaseHandler;
import com.syncron.utils.NavigationManager;
import com.syncron.utils.TimeEngine;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class EvaluationDetailsController {

    @FXML private Label titleLabel;
    @FXML private Label marksLabel;
    @FXML private Label descLabel;
    @FXML private Label statusClockLabel;
    @FXML private Button downloadQuestionBtn;

    // Student View
    @FXML private VBox studentViewBox;
    @FXML private Label submissionStatusLabel;
    @FXML private Label studentGradeLabel;
    @FXML private VBox studentFilesContainer;
    @FXML private Button selectFilesBtn;
    @FXML private Button saveChangesBtn;
    @FXML private Button removeSubBtn;

    // Teacher View
    @FXML private Label submissionCountLabel;
    @FXML private VBox gradingListContainer;

    private String evaluationId;
    private String attachedFilePath = "None";
    private String evaluationType = "OFFLINE";

    // Hyperlink to profile

    @FXML private javafx.scene.control.Hyperlink creatorLink;
    @FXML private VBox datesBox;
    @FXML private HBox teacherActionBox;
    @FXML private VBox teacherGradingBox;
    private String creatorId = null;


    // Edge Case Tracking
    private boolean hasSubmissions = false;
    private boolean isOnlineAndLocked = false;

    // opening time and closing time
    @FXML private Label openedTimeLabel;
    @FXML private Label closedTimeLabel;

    // The Staging Memory for Students
    private List<File> stagedFiles = new ArrayList<>();

    @FXML
    public void initialize() {
        evaluationId = SessionManager.getCurrentEvaluationId();
        if (evaluationId == null || evaluationId.isEmpty()) { goBack(); return; }

        loadEvaluationData();

        com.syncron.models.User currentUser = SessionManager.getCurrentUser();

        if ("TEACHER".equalsIgnoreCase(currentUser.getRole())) {
            // edit/delete buttons are always visible to teachers
            teacherActionBox.setVisible(true);
            teacherActionBox.setManaged(true);

            if ("CT".equals(evaluationType)) {
                teacherGradingBox.setVisible(false);
                teacherGradingBox.setManaged(false);
            } else {
                teacherGradingBox.setVisible(true);
                teacherGradingBox.setManaged(true);
                loadTeacherGradingList();
            }
        } else {
            if ("CT".equals(evaluationType)) {
                studentViewBox.setVisible(false);
                studentViewBox.setManaged(false);
            } else {
                studentViewBox.setVisible(true);
                studentViewBox.setManaged(true);
                loadStudentSubmissionStatus();
            }
        }
    }

    private void loadEvaluationData() {
        // updated query to fetch the creator's full name alongside the evaluation data
        String query = "SELECT e.*, u.name AS creator_name FROM evaluations e " +
                "JOIN users u ON e.creator_id = u.id WHERE e.id = ?";

        try (java.sql.Connection conn = DatabaseHandler.connect();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, evaluationId);
            java.sql.ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                titleLabel.setText(rs.getString("title"));
                marksLabel.setText("Marks: " + rs.getString("total_marks"));
                descLabel.setText(rs.getString("description"));

                evaluationType = rs.getString("type");

                // set the creator name and store their id for the profile link
                creatorLink.setText(rs.getString("creator_name"));
                creatorId = rs.getString("creator_id");

                // completely hide the opened/due dates if it is a ct
                if ("CT".equals(evaluationType)) {
                    datesBox.setVisible(false);
                    datesBox.setManaged(false);
                } else {
                    datesBox.setVisible(true);
                    datesBox.setManaged(true);
                }

                attachedFilePath = rs.getString("file_path");
                if (attachedFilePath != null && !attachedFilePath.equals("None")) {
                    File f = new File(attachedFilePath);
                    downloadQuestionBtn.setText("📄 " + f.getName());
                }

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                String startStr = rs.getString("start_date") + " " + rs.getString("start_time");
                String endStr = rs.getString("deadline_date") + " " + rs.getString("deadline_time");

                try {
                    LocalDateTime startTime = LocalDateTime.parse(startStr, formatter);
                    LocalDateTime endTime = LocalDateTime.parse(endStr, formatter);
                    evaluationType = rs.getString("type");

                    // 👉 ADDED: Display exact dates beautifully!
                    DateTimeFormatter displayFormat = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy, h:mm a");
                    openedTimeLabel.setText("Opened: " + startTime.format(displayFormat));
                    closedTimeLabel.setText("Due: " + endTime.format(displayFormat));

                    String engineType = (evaluationType.equals("CT") || evaluationType.equals("ONLINE")) ? "ONLINE" : "OFFLINE";
                    TimeEngine.startLiveCountdown(statusClockLabel, engineType, startTime, endTime);

                    // THE UX FIX: Make the lock obvious
                    if (evaluationType.equals("ONLINE") && LocalDateTime.now().isBefore(startTime)) {
                        if (!"TEACHER".equalsIgnoreCase(SessionManager.getCurrentUser().getRole())) {
                            downloadQuestionBtn.setText("🔒 Unlocks at " + startTime.format(DateTimeFormatter.ofPattern("hh:mm a")));
                            downloadQuestionBtn.setDisable(true);
                            downloadQuestionBtn.setStyle("-fx-text-fill: #F39C12; -fx-opacity: 1.0; -fx-font-weight: bold; -fx-underline: false; -fx-font-size: 14px;");

                            selectFilesBtn.setDisable(true);
                            submissionStatusLabel.setText("Submissions not open yet.");
                            submissionStatusLabel.setStyle("-fx-text-fill: #F39C12; -fx-font-weight: bold; -fx-font-size: 14px;");
                        }
                    }

                    if (LocalDateTime.now().isAfter(endTime)) {
                        selectFilesBtn.setDisable(true);
                        saveChangesBtn.setDisable(true);
                        removeSubBtn.setDisable(true);
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // STUDENT LOGIC (Multi-file, Staging, Deletion)

    private void loadStudentSubmissionStatus() {
        studentFilesContainer.getChildren().clear();
        stagedFiles.clear();
        saveChangesBtn.setDisable(true); // Disable until new files are chosen

        String studentId = SessionManager.getCurrentUser().getId();
        String query = "SELECT file_path, grade FROM submissions WHERE evaluation_id = ? AND student_id = ?";

        try (java.sql.Connection conn = DatabaseHandler.connect();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, evaluationId);
            pstmt.setString(2, studentId);
            java.sql.ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                submissionStatusLabel.setText("Submitted for grading");
                submissionStatusLabel.setStyle("-fx-text-fill: #2ECC71; -fx-font-weight: bold;");
                studentGradeLabel.setText(rs.getString("grade"));
                removeSubBtn.setVisible(true); // Show remove button!

                // Split the paths if there are multiple files (separated by ;)
                String paths = rs.getString("file_path");
                if (paths != null && !paths.isEmpty()) {
                    for (String path : paths.split(";")) {
                        File f = new File(path);
                        Label fileLbl = new Label("📎 " + f.getName());
                        fileLbl.setStyle("-fx-text-fill: #3498DB;");
                        studentFilesContainer.getChildren().add(fileLbl);
                    }
                }
            } else {
                submissionStatusLabel.setText("No submission");
                submissionStatusLabel.setStyle("-fx-text-fill: #E74C3C; -fx-font-weight: bold;");
                studentFilesContainer.getChildren().add(new Label("No files attached."));
                removeSubBtn.setVisible(false);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void handleSelectFiles() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Files");
        Stage stage = (Stage) titleLabel.getScene().getWindow();

        // 👉 NEW: Allow multiple file selection!
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(stage);

        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            stagedFiles.addAll(selectedFiles); // Stage them
            studentFilesContainer.getChildren().clear(); // Clear UI

            for (File f : stagedFiles) {
                Label lbl = new Label("⏳ " + f.getName() + " (Staged)");
                lbl.setStyle("-fx-text-fill: #E67E22; -fx-font-style: italic;");
                studentFilesContainer.getChildren().add(lbl);
            }
            saveChangesBtn.setDisable(false); // Enable saving!
        }
    }

    @FXML
    private void handleSaveChanges() {
        if (stagedFiles.isEmpty()) return;

        try {
            File uploadDir = new File("uploads/submissions");
            if (!uploadDir.exists()) uploadDir.mkdirs();

            String studentId = SessionManager.getCurrentUser().getId();
            StringBuilder allPaths = new StringBuilder();

            for (File file : stagedFiles) {
                String newFileName = evaluationId + "_" + studentId + "_" + System.currentTimeMillis() + "_" + file.getName();
                File destFile = new File(uploadDir, newFileName);
                Files.copy(file.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                allPaths.append(destFile.getPath()).append(";");
            }

            // Remove trailing semicolon
            String finalPathString = allPaths.substring(0, allPaths.length() - 1);

            // UPSERT LOGIC
            boolean exists = false;
            try (java.sql.Connection conn = DatabaseHandler.connect();
                 java.sql.PreparedStatement check = conn.prepareStatement("SELECT id FROM submissions WHERE evaluation_id=? AND student_id=?")) {
                check.setString(1, evaluationId); check.setString(2, studentId);
                exists = check.executeQuery().next();
            }

            String sql = exists
                    ? "UPDATE submissions SET file_path = ?, submission_time = ? WHERE evaluation_id = ? AND student_id = ?"
                    : "INSERT INTO submissions (file_path, submission_time, evaluation_id, student_id, grade) VALUES (?, ?, ?, ?, 'Not Graded')";

            try (java.sql.Connection conn = DatabaseHandler.connect();
                 java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, finalPathString);
                pstmt.setString(2, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                pstmt.setString(3, evaluationId);
                pstmt.setString(4, studentId);
                pstmt.executeUpdate();
            }

            new Alert(Alert.AlertType.INFORMATION, "Changes Saved!").show();
            loadStudentSubmissionStatus(); // Reload UI

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to upload submission.").show();
        }
    }

    @FXML
    private void handleRemoveSubmission() {
        String studentId = SessionManager.getCurrentUser().getId();
        String sql = "DELETE FROM submissions WHERE evaluation_id = ? AND student_id = ?";
        try (java.sql.Connection conn = DatabaseHandler.connect();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, evaluationId);
            pstmt.setString(2, studentId);
            pstmt.executeUpdate();

            new Alert(Alert.AlertType.INFORMATION, "Submission Removed.").show();
            loadStudentSubmissionStatus(); // Refresh UI to default
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ==============================================================
    // TEACHER LOGIC (Grading & CSV Import)
    // ==============================================================

    private void loadTeacherGradingList() {
        gradingListContainer.getChildren().clear();
        int count = 0;

        String query = "SELECT s.file_path, s.submission_time, s.grade, u.name, u.id as roll " +
                "FROM submissions s JOIN users u ON s.student_id = u.id " +
                "WHERE s.evaluation_id = ?";

        try (java.sql.Connection conn = DatabaseHandler.connect();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, evaluationId);
            java.sql.ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                count++;
                HBox row = new HBox(15);
                row.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #ECF0F1; -fx-padding: 15; -fx-border-radius: 6;");
                row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                VBox studentInfo = new VBox(5);
                Label nameLbl = new Label(rs.getString("name") + " (" + rs.getString("roll") + ")");
                nameLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #2C3E50;");

                // Show how many files were submitted
                String[] files = rs.getString("file_path").split(";");
                Label timeLbl = new Label("Submitted " + files.length + " file(s) on " + rs.getString("submission_time"));
                timeLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #7F8C8D;");
                studentInfo.getChildren().addAll(nameLbl, timeLbl);

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                // Show current grade from DB
                Label currentGrade = new Label("Grade: " + rs.getString("grade"));
                currentGrade.setStyle("-fx-font-weight: bold; -fx-text-fill: #D35400;");

                row.getChildren().addAll(studentInfo, spacer, currentGrade);
                gradingListContainer.getChildren().add(row);
            }
            submissionCountLabel.setText("Submissions (" + count + ")");

            hasSubmissions = (count > 0);

            if (count == 0) {
                gradingListContainer.getChildren().add(new Label("No submissions yet."));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // NEW: Excel (CSV) Grading Engine!
    @FXML
    private void handleImportCSV() {

        if (!hasSubmissions) {
            new Alert(Alert.AlertType.WARNING, "There are no submissions to grade yet.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Grades (CSV)");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        Stage stage = (Stage) titleLabel.getScene().getWindow();
        File csvFile = fileChooser.showOpenDialog(stage);

        if (csvFile != null) {
            try {
                List<String> lines = Files.readAllLines(csvFile.toPath());
                int updatedCount = 0;

                try (java.sql.Connection conn = DatabaseHandler.connect();
                     java.sql.PreparedStatement pstmt = conn.prepareStatement("UPDATE submissions SET grade = ? WHERE student_id = ? AND evaluation_id = ?")) {

                    for (String line : lines) {
                        String[] parts = line.split(",");
                        if (parts.length >= 2) {
                            String studentId = parts[0].trim();
                            String grade = parts[1].trim();

                            // Update the DB!
                            pstmt.setString(1, grade);
                            pstmt.setString(2, studentId);
                            pstmt.setString(3, evaluationId);
                            updatedCount += pstmt.executeUpdate();
                        }
                    }
                }
                new Alert(Alert.AlertType.INFORMATION, "Import Successful! " + updatedCount + " grades updated.").show();
                loadTeacherGradingList(); // Refresh the UI to show the new marks!
            } catch (Exception e) {
                e.printStackTrace();
                new Alert(Alert.AlertType.ERROR, "Error reading CSV file. Ensure format is: StudentID,Grade").show();
            }
        }
    }

    @FXML
    private void downloadQuestion() {
        if (attachedFilePath == null || attachedFilePath.equals("None")) return;
        try { Desktop.getDesktop().open(new File(attachedFilePath)); }
        catch (Exception e) { new Alert(Alert.AlertType.ERROR, "Could not open file.").show(); }
    }

    @FXML
    private void handleEdit() {
        SessionManager.setEditEvaluationId(this.evaluationId);

        // BUG FIXED: Dynamically route to the correct Creator Canvas
        if ("CT".equals(evaluationType) || "ASSIGNMENT".equals(evaluationType)) {
            NavigationManager.switchScreen("theory_evaluations.fxml");
        } else {
            NavigationManager.switchScreen("sessional_evaluations.fxml");
        }
    }

    @FXML
    private void handleDelete() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete this evaluation and all its submissions?", javafx.scene.control.ButtonType.YES, javafx.scene.control.ButtonType.NO);
        confirm.showAndWait();

        if (confirm.getResult() == javafx.scene.control.ButtonType.YES) {
            try (java.sql.Connection conn = DatabaseHandler.connect();
                 java.sql.Statement stmt = conn.createStatement()) {
                // Delete the evaluation AND all student submissions attached to it!
                stmt.execute("DELETE FROM evaluations WHERE id = " + evaluationId);
                stmt.execute("DELETE FROM submissions WHERE evaluation_id = " + evaluationId);

                new Alert(Alert.AlertType.INFORMATION, "Evaluation deleted.").show();
                goBack();
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    @FXML
    private void goBack() {
        if ("ONLINE".equals(evaluationType))
            NavigationManager.switchScreen("onlines.fxml");
        else if ("OFFLINE".equals(evaluationType)) NavigationManager.switchScreen("offlines.fxml");
        else NavigationManager.switchScreen("ct_assignments.fxml");
    }


    @FXML
    private void handleDownloadQuestion() {
        if (attachedFilePath == null || attachedFilePath.equals("None") || attachedFilePath.isEmpty()) {
            return; // No file to open
        }

        try {
            java.io.File file = new java.io.File(attachedFilePath);
            if (file.exists()) {
                // This opens the PDF beautifully using the user's default system viewer (e.g., Chrome, Edge, Adobe)
                java.awt.Desktop.getDesktop().open(file);
            } else {
                new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "The file could not be found!").show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Unable to open the file.").show();
        }
    }

    @FXML
    private void openCreatorProfile() {
        if (creatorId != null && !creatorId.isEmpty()) {
            SessionManager.setViewProfileId(creatorId);
            // route this to whatever your profile fxml is named
            NavigationManager.switchScreen("view_profile.fxml");
        }
    }

}
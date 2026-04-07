package com.syncron.controllers;

import com.syncron.models.User;
import com.syncron.utils.DatabaseHandler;
import com.syncron.utils.NavigationManager;
import com.syncron.utils.TimeEngine;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
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
import java.util.Map;

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


    // UI & Search Memory
    private List<Map<String, String>> allSubmissionsList = new ArrayList<>();
    private TextField searchSubmissionsField;
    private VBox submissionsWrapperBox;


    @FXML
    public void initialize() {
        evaluationId = SessionManager.getCurrentEvaluationId();
        if (evaluationId == null || evaluationId.isEmpty()) { goBack(); return; }

        loadEvaluationData();

        com.syncron.models.User currentUser = SessionManager.getCurrentUser();

        if ("TEACHER".equalsIgnoreCase(currentUser.getRole())) {
            // 1. Show Teacher UI
            if (teacherActionBox != null) { teacherActionBox.setVisible(true); teacherActionBox.setManaged(true); }

            // 2. HIDE Student UI
            if (studentViewBox != null) { studentViewBox.setVisible(false); studentViewBox.setManaged(false); }

            if ("CT".equals(evaluationType)) {
                if (teacherGradingBox != null) { teacherGradingBox.setVisible(false); teacherGradingBox.setManaged(false); }
            } else {
                if (teacherGradingBox != null) { teacherGradingBox.setVisible(true); teacherGradingBox.setManaged(true); }
                loadTeacherGradingList();
            }
        } else {
            // 1. HIDE Teacher UI
            if (teacherActionBox != null) { teacherActionBox.setVisible(false); teacherActionBox.setManaged(false); }
            if (teacherGradingBox != null) { teacherGradingBox.setVisible(false); teacherGradingBox.setManaged(false); }

            // 2. Show Student UI
            if ("CT".equals(evaluationType)) {
                if (studentViewBox != null) { studentViewBox.setVisible(false); studentViewBox.setManaged(false); }
            } else {
                if (studentViewBox != null) { studentViewBox.setVisible(true); studentViewBox.setManaged(true); }
                loadStudentSubmissionStatus();
            }
        }
    }

    private void loadEvaluationData() {
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://localhost:8080/api/evaluations/details/" + evaluationId))
                    .GET().build();

            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                com.google.gson.Gson gson = new com.google.gson.Gson();
                java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<java.util.Map<String, String>>(){}.getType();
                java.util.Map<String, String> rs = gson.fromJson(response.body(), type);

                titleLabel.setText(rs.get("title"));
                marksLabel.setText("Marks: " + rs.get("totalMarks"));
                descLabel.setText(rs.get("description"));

                evaluationType = rs.get("type");
                creatorLink.setText(rs.get("creatorName"));
                creatorId = rs.get("creatorId");

                if ("CT".equals(evaluationType)) {
                    datesBox.setVisible(false);
                    datesBox.setManaged(false);
                } else {
                    datesBox.setVisible(true);
                    datesBox.setManaged(true);
                }

                attachedFilePath = rs.get("file_path");
                if (attachedFilePath != null && !attachedFilePath.equals("None")) {
                    File f = new File(attachedFilePath);
                    downloadQuestionBtn.setText("📄 " + cleanFileName(f.getName())); // 👉 CLEANED!
                }

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                String startStr = rs.get("startDate") + " " + rs.get("startTime");
                String endStr = rs.get("deadlineDate") + " " + rs.get("deadlineTime");

                try {
                    LocalDateTime startTime = LocalDateTime.parse(startStr, formatter);
                    LocalDateTime endTime = LocalDateTime.parse(endStr, formatter);

                    DateTimeFormatter displayFormat = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy, h:mm a");
                    openedTimeLabel.setText("Opened: " + startTime.format(displayFormat));
                    closedTimeLabel.setText("Due: " + endTime.format(displayFormat));

                    String engineType = (evaluationType.equals("CT") || evaluationType.equals("ONLINE")) ? "ONLINE" : "OFFLINE";
                    TimeEngine.startLiveCountdown(statusClockLabel, engineType, startTime, endTime);

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
        if (studentFilesContainer != null) studentFilesContainer.getChildren().clear();
        stagedFiles.clear();
        if (saveChangesBtn != null) saveChangesBtn.setDisable(true);

        String studentId = SessionManager.getCurrentUser().getId();

        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://localhost:8080/api/submissions/" + evaluationId + "/" + studentId))
                    .GET()
                    .build();

            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                if (submissionStatusLabel != null) {
                    submissionStatusLabel.setText("Submitted to Cloud");
                    submissionStatusLabel.setStyle("-fx-text-fill: #2ECC71; -fx-font-weight: bold;");
                }
                if (removeSubBtn != null) removeSubBtn.setVisible(true);

                String body = response.body();
                String paths = extractJsonValue(body, "filePath");
                String grade = extractJsonValue(body, "grade");

                if (studentGradeLabel != null) studentGradeLabel.setText(grade);

                if (paths != null && !paths.isEmpty() && studentFilesContainer != null) {
                    for (String path : paths.split(";")) {
                        java.io.File f = new java.io.File(path);
                        // 👉 THE FIX: Wrap f.getName() inside cleanFileName()
                        javafx.scene.control.Label fileLbl = new javafx.scene.control.Label("📄 " + cleanFileName(f.getName()));
                        fileLbl.setStyle("-fx-text-fill: #3498DB;");
                        studentFilesContainer.getChildren().add(fileLbl);
                    }
                }

            } else {
                if (submissionStatusLabel != null) {
                    submissionStatusLabel.setText("No submission");
                    submissionStatusLabel.setStyle("-fx-text-fill: #E74C3C; -fx-font-weight: bold;");
                }
                if (studentFilesContainer != null) studentFilesContainer.getChildren().add(new javafx.scene.control.Label("No files attached."));
                if (removeSubBtn != null) removeSubBtn.setVisible(false);
                if (studentGradeLabel != null) studentGradeLabel.setText("Not Graded");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
            java.io.File uploadDir = new java.io.File("uploads/submissions");
            if (!uploadDir.exists()) uploadDir.mkdirs();

            String studentId = SessionManager.getCurrentUser().getId();
            StringBuilder allPaths = new StringBuilder();

            for (java.io.File file : stagedFiles) {
                String newFileName = evaluationId + "_" + studentId + "_" + System.currentTimeMillis() + "_" + file.getName();
                java.io.File destFile = new java.io.File(uploadDir, newFileName);
                java.nio.file.Files.copy(file.toPath(), destFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                allPaths.append(destFile.getPath()).append(";");
            }

            String finalPathString = allPaths.substring(0, allPaths.length() - 1);

            // 👉 THE CLOUD API CALL (No more Local DB!)
            String jsonPayload = String.format("{\"assessmentId\":\"%s\",\"studentId\":\"%s\",\"filePath\":\"%s\"}",
                    evaluationId, studentId, finalPathString.replace("\\", "\\\\"));

            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://localhost:8080/api/submissions"))
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION, "Changes Saved to Cloud!").show();
                loadStudentSubmissionStatus(); // Reloads securely from the Cloud!
            } else {
                new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Cloud Error!").show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Failed to upload submission.").show();
        }
    }

    @FXML
    private void handleRemoveSubmission() {
        String studentId = SessionManager.getCurrentUser().getId();
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://localhost:8080/api/submissions/" + evaluationId + "/" + studentId))
                    .DELETE()
                    .build();

            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION, "Submission Removed from Cloud.").show();
                loadStudentSubmissionStatus();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==============================================================
    // TEACHER LOGIC (Grading & CSV Import)
    // ==============================================================

    private void loadTeacherGradingList() {
        gradingListContainer.getChildren().clear();

        // 1. Build the Search Bar & Glowing Container ONCE
        searchSubmissionsField = new TextField();
        searchSubmissionsField.setPromptText("🔍 Search by Student Name or Roll...");
        searchSubmissionsField.setStyle("-fx-background-radius: 20; -fx-border-radius: 20; -fx-padding: 10 15; -fx-border-color: #BDC3C7; -fx-background-color: white; -fx-font-family: 'Inter', sans-serif;");
        searchSubmissionsField.textProperty().addListener((obs, old, newVal) -> renderSubmissions(newVal));

        submissionsWrapperBox = new VBox(15);
        // 👉 THE GLOWING LIGHT BLUE BOX!
        submissionsWrapperBox.setStyle("-fx-background-color: #F4FAFE; -fx-border-color: #85C1E9; -fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 20; -fx-effect: dropshadow(three-pass-box, rgba(52, 152, 219, 0.3), 15, 0, 0, 0);");

        gradingListContainer.getChildren().addAll(searchSubmissionsField, submissionsWrapperBox);

        // 2. Fetch Data
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://localhost:8080/api/evaluations/" + evaluationId + "/submissions"))
                    .GET().build();

            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                com.google.gson.Gson gson = new com.google.gson.Gson();
                java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<Map<String, String>>>(){}.getType();
                allSubmissionsList = gson.fromJson(response.body(), listType);

                int count = allSubmissionsList.size();
                hasSubmissions = (count > 0);
                submissionCountLabel.setText("Submissions (" + count + ")");

                renderSubmissions(""); // Render all initially
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // THE FILTERING & RENDERING ENGINE
    private void renderSubmissions(String query) {
        submissionsWrapperBox.getChildren().clear();

        if (allSubmissionsList.isEmpty()) {
            submissionsWrapperBox.getChildren().add(new Label("No submissions yet."));
            return;
        }

        String lowerQuery = query.toLowerCase();

        for (Map<String, String> rs : allSubmissionsList) {
            String stuName = rs.get("name");
            String stuRoll = rs.get("roll");

            // Search Filter
            if (!stuName.toLowerCase().contains(lowerQuery) && !stuRoll.toLowerCase().contains(lowerQuery)) continue;

            VBox row = new VBox(10);
            row.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #ECF0F1; -fx-padding: 15; -fx-border-radius: 8; -fx-background-radius: 8;");

            HBox topRow = new HBox(15);
            topRow.setAlignment(Pos.CENTER_LEFT);

            VBox studentInfo = new VBox(2);
            Label nameLbl = new Label(stuName + " (" + stuRoll + ")");
            nameLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #2C3E50; -fx-font-size: 14px;");

            // THE BEAUTIFUL DATE FORMATTER v.2
            String rawDate = rs.get("submissionTime");
            String niceDate = rawDate;
            try {
                LocalDateTime dt;
                // If it has a 'T', it's a Spring Boot ISO Timestamp. Otherwise, it's a raw SQL string.
                if (rawDate != null && rawDate.contains("T")) {
                    dt = LocalDateTime.parse(rawDate);
                } else {
                    dt = LocalDateTime.parse(rawDate, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                }
                niceDate = dt.format(DateTimeFormatter.ofPattern("d MMMM yyyy, hh:mm a"));
            } catch (Exception ignored) {}

            Label timeLbl = new Label("Submitted on: " + niceDate);
            timeLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #7F8C8D;");
            studentInfo.getChildren().addAll(nameLbl, timeLbl);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label currentGrade = new Label("Grade: " + rs.get("grade"));
            currentGrade.setStyle("-fx-font-weight: bold; -fx-text-fill: #D35400; -fx-font-size: 14px;");

            topRow.getChildren().addAll(studentInfo, spacer, currentGrade);
            row.getChildren().add(topRow);

            // THE PREMIUM DOWNLOAD BUTTONS FOR FILES
            String[] files = rs.get("filePath") != null ? rs.get("filePath").split(";") : new String[0];
            if (files.length > 0 && !files[0].isEmpty()) {
                HBox filesBox = new HBox(10);
                filesBox.setStyle("-fx-padding: 10 0 0 0;");
                for (String filePath : files) {
                    if (filePath.trim().isEmpty()) continue;

                    String cleanName = cleanFileName(new File(filePath).getName());
                    Button dlBtn = new Button("⬇ Download: " + cleanName);

                    String baseStyle = "-fx-background-color: transparent; -fx-text-fill: #3498DB; -fx-font-weight: bold; -fx-cursor: hand; -fx-border-color: #3498DB; -fx-border-radius: 20; -fx-padding: 4 12; -fx-font-size: 11px;";
                    String hoverStyle = baseStyle + " -fx-background-color: #3498DB; -fx-text-fill: white; -fx-effect: dropshadow(three-pass-box, rgba(52, 152, 219, 0.4), 10, 0, 0, 0);";

                    dlBtn.setStyle(baseStyle);
                    dlBtn.setOnMouseEntered(e -> dlBtn.setStyle(hoverStyle));
                    dlBtn.setOnMouseExited(e -> dlBtn.setStyle(baseStyle));

                    dlBtn.setOnAction(e -> downloadFile(filePath, cleanName));
                    filesBox.getChildren().add(dlBtn);
                }
                row.getChildren().add(filesBox);
            }

            submissionsWrapperBox.getChildren().add(row);
        }
    }


    // NEW: Excel (CSV) Grading Engine
    @FXML
    private void handleImportCSV() {
        if (!hasSubmissions) {
            new Alert(Alert.AlertType.WARNING, "There are no submissions to grade yet.").show();
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Grades (CSV)");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        Stage stage = (Stage) titleLabel.getScene().getWindow();
        File csvFile = fileChooser.showOpenDialog(stage);

        if (csvFile != null) {
            try {
                java.util.List<String> lines = Files.readAllLines(csvFile.toPath());
                StringBuilder jsonArray = new StringBuilder("[");

                for (String line : lines) {
                    String[] parts = line.split(",");
                    if (parts.length >= 2) {
                        String studentId = parts[0].trim();
                        String grade = parts[1].trim();
                        jsonArray.append(String.format("{\"evaluationId\":\"%s\", \"studentId\":\"%s\", \"grade\":\"%s\"},", evaluationId, studentId, grade));
                    }
                }

                // Remove trailing comma and close array
                if (jsonArray.length() > 1) {
                    jsonArray.setLength(jsonArray.length() - 1);
                }
                jsonArray.append("]");

                java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create("http://localhost:8080/api/submissions/grades"))
                        .header("Content-Type", "application/json")
                        .PUT(java.net.http.HttpRequest.BodyPublishers.ofString(jsonArray.toString()))
                        .build();

                java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    String count = extractJsonValue(response.body(), "count");
                    new Alert(Alert.AlertType.INFORMATION, "Import Successful! " + count + " grades updated to Cloud.").show();
                    loadTeacherGradingList();
                } else {
                    new Alert(Alert.AlertType.ERROR, "Cloud Error updating grades.").show();
                }
            } catch (Exception e) {
                e.printStackTrace();
                new Alert(Alert.AlertType.ERROR, "Error reading CSV or connecting to server.").show();
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

    // JSON Helper (Bulletproofed against spaces)
    private String extractJsonValue(String json, String key) {
        try {
            String search = "\"" + key + "\"";
            int start = json.indexOf(search);
            if (start == -1) return "";

            start = json.indexOf(":", start) + 1;
            while (json.charAt(start) == ' ' || json.charAt(start) == '\"') {
                start++;
            }

            int end = json.indexOf("\"", start);
            return end == -1 ? "" : json.substring(start, end);
        } catch (Exception e) {
            return "";
        }
    }


    private String cleanFileName(String rawName) {
        if (rawName == null) return "";
        return rawName.replaceFirst("^.*?_\\d{13}_", "");
    }

    private void downloadFile(String sourcePath, String cleanName) {
        File sourceFile = new File(sourcePath);
        if (!sourceFile.exists()) return;
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Submission");
        fileChooser.setInitialFileName(cleanName);
        File destFile = fileChooser.showSaveDialog(titleLabel.getScene().getWindow());
        if (destFile != null) {
            try { Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING); }
            catch (Exception e) { e.printStackTrace(); }
        }
    }

}
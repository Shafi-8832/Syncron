package com.syncron.controllers;

import com.syncron.models.User;
import com.syncron.utils.NavigationManager;
import com.syncron.utils.ServerConfig;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.application.Platform;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class TheoryEvaluationsController {

    @FXML private TextField titleInput;
    @FXML private DatePicker datePicker;

    @FXML private VBox targetSectionContainer;
    @FXML private HBox sectionBox;

    @FXML private ComboBox<String> startHour, startMin, startAmPm;

    @FXML private VBox durationBox;
    @FXML private TextField durationInput;

    @FXML private Button cancelBtn;
    @FXML private Button publishBtn;

    @FXML private TextField marksInput;
    @FXML private TextArea descriptionInput;

    @FXML private Label dateLabel;
    @FXML private Label timeLabel;

    @FXML private VBox endTimeBox;

    @FXML private Button assignmentToggleBtn;
    @FXML private Button ctToggleBtn;

    // 🔥 MODERNIZED FILE UPLOAD UI COMPONENTS
    @FXML private Label fileNameLabel;
    @FXML private Button attachFileBtn; // Make sure this exists or replace the existing one
    private Button removeFileBtn; // Dynamically created

    private String currentType = "ASSIGNMENT";
    private File selectedPdfFile = null;

    // Critical: Keep track of the old file path so we don't accidentally nuke it during an update!
    private String existingFilePath = "None";

    private String editId = null;

    @FXML
    public void initialize() {
        System.out.println("--- 🚀 THEORY ENGINE STARTING ---");
        try {
            setupTimeSpinners();
            setupModernUploadUI(); // 🔥 Inject premium styling

            com.syncron.models.User currentUser = SessionManager.getCurrentUser();
            if (currentUser == null || !"TEACHER".equalsIgnoreCase(currentUser.getRole())) {
                NavigationManager.switchScreen("home.fxml");
                return;
            }

            editId = SessionManager.getEditEvaluationId();
            System.out.println("🔍 Fetched Edit ID: [" + editId + "]");

            if (editId != null && !editId.trim().isEmpty()) {
                System.out.println("✅ Edit ID found! Entering Edit Mode.");

                if (cancelBtn != null) {
                    cancelBtn.setVisible(true);
                    cancelBtn.setManaged(true);
                }

                if (publishBtn != null) publishBtn.setText("Update Assessment");

                loadExistingDataForEdit();
                SessionManager.setEditEvaluationId(null);
            } else {
                System.out.println("⚠️ No Edit ID. Entering Blank Create Mode.");
                setAssignmentMode();
            }

            // Premium Button Styling
            if(publishBtn != null) applyPremiumGlowingStyle(publishBtn, "#27AE60", "#2ECC71");
            if(cancelBtn != null) applyPremiumGlowingStyle(cancelBtn, "#7F8C8D", "#95A5A6");

        } catch (Exception e) {
            System.out.println("❌ CRITICAL CRASH IN INITIALIZE:");
            e.printStackTrace();
        }
    }

    /* =========================================================================
     * 🔥 MODERN FILE UPLOAD UI
     * ========================================================================= */

    private void setupModernUploadUI() {
        if(fileNameLabel == null) return;

        HBox parent = (HBox) fileNameLabel.getParent();

        removeFileBtn = new Button("✖ Remove");
        String removeBase = "-fx-background-color: transparent; -fx-text-fill: #E74C3C; -fx-font-weight: bold; -fx-cursor: hand; -fx-border-color: #E74C3C; -fx-border-radius: 20; -fx-padding: 3 10; -fx-font-size: 11px;";
        String removeHover = removeBase + " -fx-background-color: #E74C3C; -fx-text-fill: white; -fx-effect: dropshadow(three-pass-box, rgba(231, 76, 60, 0.4), 10, 0, 0, 0);";

        removeFileBtn.setStyle(removeBase);
        removeFileBtn.setOnMouseEntered(e -> removeFileBtn.setStyle(removeHover));
        removeFileBtn.setOnMouseExited(e -> removeFileBtn.setStyle(removeBase));

        removeFileBtn.setVisible(false);
        removeFileBtn.setManaged(false);

        removeFileBtn.setOnAction(e -> {
            selectedPdfFile = null;
            existingFilePath = "None";
            updateFileLabelUI();
        });

        parent.getChildren().add(removeFileBtn);
    }

    private void updateFileLabelUI() {
        if (selectedPdfFile != null) {
            fileNameLabel.setText("📄 " + selectedPdfFile.getName());
            fileNameLabel.setStyle("-fx-text-fill: #2ECC71; -fx-font-weight: bold; -fx-font-size: 13px;");
            removeFileBtn.setVisible(true);
            removeFileBtn.setManaged(true);
        } else if (!existingFilePath.equals("None") && !existingFilePath.isEmpty()) {
            // Extract just the name from the path
            String cleanName = new File(existingFilePath).getName().replaceFirst("^.*?_\\d{13}_", "");
            fileNameLabel.setText("📄 " + cleanName + " (Existing)");
            fileNameLabel.setStyle("-fx-text-fill: #3498DB; -fx-font-weight: bold; -fx-font-size: 13px;");
            removeFileBtn.setVisible(true);
            removeFileBtn.setManaged(true);
        } else {
            fileNameLabel.setText("No file attached");
            fileNameLabel.setStyle("-fx-text-fill: #95A5A6; -fx-font-style: italic;");
            removeFileBtn.setVisible(false);
            removeFileBtn.setManaged(false);
        }
    }

    private void applyPremiumGlowingStyle(Button btn, String baseHex, String hoverHex) {
        String baseStyle = "-fx-background-color: " + baseHex + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 8; -fx-padding: 8 20;";
        String hoverStyle = baseStyle + " -fx-effect: dropshadow(three-pass-box, " + hoverHex + ", 15, 0.3, 0, 0);";

        btn.setStyle(baseStyle);
        btn.setOnMouseEntered(e -> btn.setStyle(hoverStyle));
        btn.setOnMouseExited(e -> btn.setStyle(baseStyle));
    }


    /* =========================================================================
     * THE REST API DATA FETCH (Fixes the missing description!)
     * ========================================================================= */

    @FXML
    private void loadExistingDataForEdit() {
        // We MUST fetch from the Cloud API, not local DB, so we get the real, current text!
        new Thread(() -> {
            try {
                java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create(ServerConfig.getBaseUrl() + "/api/evaluations/details/" + editId))
                        .GET().build();

                java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    com.google.gson.Gson gson = new com.google.gson.Gson();
                    java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<Map<String, String>>(){}.getType();
                    Map<String, String> rs = gson.fromJson(response.body(), type);

                    Platform.runLater(() -> {
                        // 1. Restore text fields securely
                        titleInput.setText(rs.get("title") != null ? rs.get("title") : "");
                        marksInput.setText(rs.get("totalMarks") != null ? rs.get("totalMarks") : "");

                        // FIX: Safely unescape the description from JSON format
                        // FIX: Safely unescape the description from JSON format
                        String desc = rs.get("description");
                        if(desc != null) {
                            desc = desc.replace("\\n", "\n").replace("\\\"", "\"");
                            descriptionInput.setText(desc);
                        }

                        // THE ROBUST FILE FETCH FIX
                        String fetchedPath = rs.get("file_path");
                        if (fetchedPath == null || fetchedPath.isEmpty() || fetchedPath.equals("null")) {
                            fetchedPath = rs.get("filePath"); // Fallback check
                        }

                        if (fetchedPath != null && !fetchedPath.equals("null") && !fetchedPath.equals("None")) {
                            existingFilePath = fetchedPath;
                            updateFileLabelUI();
                        }

                        String assessmentType = rs.get("type");

                        // 3. Set mode correctly
                        if ("CT".equals(assessmentType)) {
                            setCtMode();
                        } else {
                            setAssignmentMode();
                        }

                        // 4. RESTORE DATES AND TIMES
                        try {
                            String dateStr = "CT".equals(assessmentType) ? rs.get("startDate") : rs.get("deadlineDate");
                            if (dateStr != null && !dateStr.isEmpty() && !dateStr.equals("null")) {
                                datePicker.setValue(LocalDate.parse(dateStr));
                            }

                            String timeStr = "CT".equals(assessmentType) ? rs.get("startTime") : rs.get("deadlineTime");
                            if (timeStr != null && !timeStr.isEmpty() && !timeStr.equals("null")) {
                                LocalTime time = LocalTime.parse(timeStr);
                                int hour = time.getHour();
                                String amPm = hour >= 12 ? "PM" : "AM";

                                if (hour == 0) hour = 12;
                                else if (hour > 12) hour -= 12;

                                startHour.setValue(String.format("%02d", hour));
                                startMin.setValue(String.format("%02d", time.getMinute()));
                                startAmPm.setValue(amPm);
                            }

                            // Calculate duration for CTs
                            if ("CT".equals(assessmentType)) {
                                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                                LocalDateTime sTime = LocalDateTime.parse(rs.get("startDate") + " " + rs.get("startTime"), fmt);
                                LocalDateTime eTime = LocalDateTime.parse(rs.get("deadlineDate") + " " + rs.get("deadlineTime"), fmt);
                                long mins = java.time.temporal.ChronoUnit.MINUTES.between(sTime, eTime);
                                durationInput.setText(String.valueOf(mins));
                            }
                        } catch (Exception timeEx) {
                            System.out.println("Could not parse dates properly, skipping autofill.");
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
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

        updateFileLabelUI();
    }

    @FXML
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

        // THE ESCAPE FIX: Properly escape the old path so it doesn't break the JSON payload
        String savedFilePath = existingFilePath.replace("\\", "\\\\");

        if (selectedPdfFile != null) {
            try {
                File uploadDir = new File("uploads/assessments");
                if (!uploadDir.exists()) uploadDir.mkdirs();

                String newFileName = courseCode.replace(" ", "") + "_" + teacherId + "_" + System.currentTimeMillis() + "_" + selectedPdfFile.getName();
                File destFile = new File(uploadDir, newFileName);

                Files.copy(selectedPdfFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                savedFilePath = destFile.getPath().replace("\\", "\\\\"); // Escape for JSON
            } catch (Exception e) {
                e.printStackTrace();
                new Alert(Alert.AlertType.ERROR, "Failed to process file!").show();
                return;
            }
        }

        try {
            // Escape description text for JSON safety
            String safeDesc = desc.replace("\"", "\\\"").replace("\n", "\\n");

            // Build payload using the securely persisted savedFilePath
            String jsonPayload = String.format(
                    "{\"courseCode\":\"%s\",\"title\":\"%s\",\"type\":\"%s\",\"targetSections\":\"%s\",\"totalMarks\":\"%s\",\"startDate\":\"%s\",\"startTime\":\"%s\",\"deadlineDate\":\"%s\",\"deadlineTime\":\"%s\",\"description\":\"%s\",\"creatorId\":\"%s\",\"filePath\":\"%s\"}",
                    courseCode, title, currentType, targetSections, marks, startDateStr, startTimeStr, deadlineDateStr, deadlineTimeStr, safeDesc, teacherId, savedFilePath
            );

            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request;

            if (editId != null && !editId.isEmpty()) {
                request = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create(ServerConfig.getBaseUrl() + "/api/evaluations/" + editId))
                        .header("Content-Type", "application/json")
                        .PUT(java.net.http.HttpRequest.BodyPublishers.ofString(jsonPayload))
                        .build();
            } else {
                request = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create(ServerConfig.getBaseUrl() + "/api/evaluations"))
                        .header("Content-Type", "application/json")
                        .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonPayload))
                        .build();
            }

            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                new Alert(Alert.AlertType.INFORMATION, "Assessment Published to Cloud Successfully!").showAndWait();
                NavigationManager.switchScreen("ct_assignments.fxml");
            } else {
                new Alert(Alert.AlertType.ERROR, "Cloud Error: Could not publish assessment.").show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Network Error: Could not connect to Cloud Server.").show();
        }
    }

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

    private String get24HourTime(String h, String m, String amPm) {
        int hour = Integer.parseInt(h);
        if (amPm.equals("PM") && hour != 12) hour += 12;
        if (amPm.equals("AM") && hour == 12) hour = 0;
        return String.format("%02d:%s", hour, m);
    }
}
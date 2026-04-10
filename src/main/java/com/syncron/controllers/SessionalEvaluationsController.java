package com.syncron.controllers;

import com.syncron.models.User;
import com.syncron.utils.NavigationManager;
import com.syncron.utils.ServerConfig;
import javafx.application.Platform;
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
import java.util.Map;

public class SessionalEvaluationsController {

    @FXML private Button offlineToggleBtn;
    @FXML private Button onlineToggleBtn;

    @FXML private TextField titleInput;
    @FXML private DatePicker datePicker;

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
    @FXML private VBox targetSectionContainer;
    @FXML private HBox sectionBox;

    @FXML private Label fileNameLabel;

    private String currentType = "OFFLINE";
    private File selectedPdfFile = null;

    // start change — track existing file path for edits (was missing before)
    private String existingFilePath = "None";
    private Button removeFileBtn;
    // end change

    private String editId = null;


    @FXML
    public void initialize() {
        setupTimeSpinners();
        // start change — add modern file upload UI
        setupModernUploadUI();
        // end change

        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null || !"TEACHER".equalsIgnoreCase(currentUser.getRole())) {
            NavigationManager.switchScreen("home.fxml");
            return;
        }

        // Edit Mode Check
        editId = SessionManager.getEditEvaluationId();
        if (editId != null && !editId.isEmpty()) {
            cancelBtn.setVisible(true);
            cancelBtn.setManaged(true);
            publishBtn.setText("Update Assessment");
            loadExistingDataForEdit();
            SessionManager.setEditEvaluationId(null);
        }
        else setOfflineMode();
    }

    // =========================================================================
    // start change — MODERN FILE UPLOAD UI (matching TheoryEvaluationsController)
    // =========================================================================

    private void setupModernUploadUI() {
        if (fileNameLabel == null) return;

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

    // end change
    // =========================================================================

    // =========================================================================
    // start change — COMPLETE REWRITE: Use REST API instead of broken local DB
    // =========================================================================

    @FXML
    private void loadExistingDataForEdit() {
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
                        // 1. Restore text fields
                        titleInput.setText(rs.get("title") != null ? rs.get("title") : "");
                        marksInput.setText(rs.get("totalMarks") != null ? rs.get("totalMarks") : "");

                        // Safely unescape description
                        String desc = rs.get("description");
                        if (desc != null) {
                            desc = desc.replace("\\n", "\n").replace("\\\"", "\"");
                            descriptionInput.setText(desc);
                        }

                        // Restore attached file path
                        String fetchedPath = rs.get("file_path");
                        if (fetchedPath == null || fetchedPath.isEmpty() || fetchedPath.equals("null")) {
                            fetchedPath = rs.get("filePath");
                        }
                        if (fetchedPath != null && !fetchedPath.equals("null") && !fetchedPath.equals("None")) {
                            existingFilePath = fetchedPath;
                            updateFileLabelUI();
                        }

                        // 2. Set mode correctly
                        String assessmentType = rs.get("type");
                        if ("ONLINE".equals(assessmentType)) {
                            setOnlineMode();
                        } else {
                            setOfflineMode();
                        }

                        // 3. Restore dates and times
                        try {
                            String dateStr = "ONLINE".equals(assessmentType) ? rs.get("startDate") : rs.get("deadlineDate");
                            if (dateStr != null && !dateStr.isEmpty() && !dateStr.equals("null")) {
                                datePicker.setValue(LocalDate.parse(dateStr));
                            }

                            String timeStr = "ONLINE".equals(assessmentType) ? rs.get("startTime") : rs.get("deadlineTime");
                            if (timeStr != null && !timeStr.isEmpty() && !timeStr.equals("null")) {
                                LocalTime time = LocalTime.parse(timeStr);
                                int hour = time.getHour();
                                String amPm = hour >= 12 ? "PM" : "AM";

                                if (hour == 0) hour = 12;
                                else if (hour > 12) hour -= 12;

                                if (startHour != null) startHour.setValue(String.format("%02d", hour));
                                if (startMin != null) startMin.setValue(String.format("%02d", time.getMinute()));
                                if (startAmPm != null) startAmPm.setValue(amPm);
                            }

                            // Calculate duration for online lab tests
                            if ("ONLINE".equals(assessmentType) && durationInput != null) {
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

    // end change
    // =========================================================================


    @FXML
    private void setOfflineMode() {
        currentType = "OFFLINE";
        offlineToggleBtn.getStyleClass().setAll("button", "kernel-toggle-active");
        onlineToggleBtn.getStyleClass().setAll("button", "kernel-toggle-inactive");

        dateLabel.setText("Deadline Date");
        timeLabel.setText("Due Time");

        targetSectionContainer.setVisible(false);
        targetSectionContainer.setManaged(false);

        durationBox.setVisible(false);
        durationBox.setManaged(false);
    }

    @FXML
    private void setOnlineMode() {
        currentType = "ONLINE";
        onlineToggleBtn.getStyleClass().setAll("button", "kernel-toggle-active");
        offlineToggleBtn.getStyleClass().setAll("button", "kernel-toggle-inactive");

        dateLabel.setText("Exam Date");
        timeLabel.setText("Opening Time");

        targetSectionContainer.setVisible(true);
        targetSectionContainer.setManaged(true);

        durationBox.setVisible(true);
        durationBox.setManaged(true);
    }

    @FXML
    private void handleAttachFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Assessment Document");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

        Stage stage = (Stage) titleInput.getScene().getWindow();
        selectedPdfFile = fileChooser.showOpenDialog(stage);

        // start change — use the modern UI updater
        updateFileLabelUI();
        // end change
    }

    @FXML
    private void handleCancel() {
        if ("ONLINE".equals(currentType)) {
            NavigationManager.switchScreen("onlines.fxml");
        } else {
            NavigationManager.switchScreen("offlines.fxml");
        }
    }


    @FXML
    private void goBack() {handleCancel();}


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
            if (!sections.isEmpty()) {
                targetSections = sections.substring(0, sections.length() - 1);
            } else {
                new Alert(Alert.AlertType.WARNING, "Please select at least one Target Section for the Lab Test!").show();
                return;
            }
        }

        StringBuilder missingFields = new StringBuilder();
        if (title.isEmpty()) missingFields.append("• Assessment Title\n");
        if (date == null) missingFields.append("• Deadline Date\n");

        boolean isStartMissing = startHour.getValue() == null || startMin.getValue() == null || startAmPm.getValue() == null;
        if (isStartMissing) {
            missingFields.append(currentType.equals("OFFLINE") ? "• Due Time\n" : "• Opening Time\n");
        }

        if (currentType.equals("ONLINE")) {
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


        if (!missingFields.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Please fill out the following missing fields:\n\n" + missingFields.toString()).show();
            return;
        }

        String startDateStr;
        String startTimeStr;
        String deadlineDateStr;
        String deadlineTimeStr;

        if (currentType.equals("OFFLINE")) {
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

        // start change — preserve existing file path during edits
        String savedFilePath = existingFilePath.replace("\\", "\\\\");
        // end change

        if (selectedPdfFile != null) {
            try {
                File uploadDir = new File("uploads/assessments");
                if (!uploadDir.exists()) uploadDir.mkdirs();

                String newFileName = courseCode.replace(" ", "") + "_" + teacherId + "_" + System.currentTimeMillis() + "_" + selectedPdfFile.getName();
                File destFile = new File(uploadDir, newFileName);

                Files.copy(selectedPdfFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                savedFilePath = destFile.getPath().replace("\\", "\\\\");
            } catch (Exception e) {
                e.printStackTrace();
                new Alert(Alert.AlertType.ERROR, "Failed to process file!").show();
                return;
            }
        }

        try {
            String safeDesc = desc.replace("\"", "\\\"").replace("\n", "\\n");

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

                String routeTo = SessionManager.getLastSidebarTab();
                if ("ONLINE".equals(routeTo)) {
                    NavigationManager.switchScreen("onlines.fxml");
                } else {
                    NavigationManager.switchScreen("offlines.fxml");
                }
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
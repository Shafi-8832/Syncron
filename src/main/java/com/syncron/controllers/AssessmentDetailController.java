package com.syncron.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.HTMLEditor;
import javafx.stage.FileChooser;
import javafx.stage.Window;

public class AssessmentDetailController {

    // --- Header Section ---
    @FXML private Label titleLabel;
    @FXML private TextField titleField;
    @FXML private Label createdByLabel;
    @FXML private TextField durationField;
    @FXML private TextField timeField;
    @FXML private TextField roomField;
    @FXML private HTMLEditor syllabusEditor;
    @FXML private HBox uploadControlsBox;
    @FXML private Button uploadFileBtn;
    @FXML private Button addLinkBtn;
    @FXML private Object dateTimePickerController;
    @FXML private Button saveDetailsBtn;

    // --- Teacher Action Area ---
    @FXML private HBox teacherActionArea;
    @FXML private Button editButton;
    @FXML private Button deleteButton;

    // --- Student Action Area ---
    @FXML private VBox studentActionArea;
    @FXML private Button uploadSubmissionButton;
    @FXML private Button editSubmissionButton;
    @FXML private Button removeSubmissionButton;

    // --- Teacher Evaluation Area ---
    @FXML private VBox teacherEvaluationArea;
    @FXML private Label submissionCountLabel;
    @FXML private TableView<?> submissionsTable;
    @FXML private TableColumn<?, ?> colWho;
    @FXML private TableColumn<?, ?> colWhen;
    @FXML private TableColumn<?, ?> colFile;
    @FXML private TableColumn<?, ?> colGraded;

    @FXML
    public void initialize() {
        // Default state: hide all role-specific areas until initializeView is called
        teacherActionArea.setVisible(false);
        teacherActionArea.setManaged(false);
        studentActionArea.setVisible(false);
        studentActionArea.setManaged(false);
        teacherEvaluationArea.setVisible(false);
        teacherEvaluationArea.setManaged(false);

        titleField.textProperty().addListener((obs, oldValue, newValue) -> titleLabel.setText(newValue));

        createdByLabel.setText("Created by: " + getCurrentUserName());

        if (isStudentRole()) {
            titleField.setEditable(false);
            durationField.setEditable(false);
            timeField.setEditable(false);
            roomField.setEditable(false);
            syllabusEditor.setDisable(true);
            uploadControlsBox.setVisible(false);
            uploadControlsBox.setManaged(false);
            saveDetailsBtn.setVisible(false);
            saveDetailsBtn.setManaged(false);
        }
    }

    /**
     * Initializes the view based on the user's role and the assessment status.
     * This method controls the RBAC (Role-Based Access Control) visibility logic.
     *
     * @param role   The role of the current user ("TEACHER" or "STUDENT").
     * @param status The status of the assessment (e.g., "PAST_DEADLINE").
     */
    public void initializeView(String role, String status) {
        if (role == null || status == null) {
            throw new IllegalArgumentException("Role and status must not be null.");
        }

        if ("TEACHER".equals(role)) {
            // Show Teacher Action Area and Evaluation Area
            teacherActionArea.setVisible(true);
            teacherActionArea.setManaged(true);
            teacherEvaluationArea.setVisible(true);
            teacherEvaluationArea.setManaged(true);

            // Hide Student Action Area
            studentActionArea.setVisible(false);
            studentActionArea.setManaged(false);
        } else if ("STUDENT".equals(role)) {
            // Hide Teacher Action and Evaluation areas
            teacherActionArea.setVisible(false);
            teacherActionArea.setManaged(false);
            teacherEvaluationArea.setVisible(false);
            teacherEvaluationArea.setManaged(false);

            // Show Student Action Area
            studentActionArea.setVisible(true);
            studentActionArea.setManaged(true);

            // If past deadline, disable submission buttons
            if ("PAST_DEADLINE".equals(status)) {
                uploadSubmissionButton.setDisable(true);
                editSubmissionButton.setDisable(true);
                removeSubmissionButton.setDisable(true);
            }
        } else {
            throw new IllegalArgumentException("Unrecognized role: " + role);
        }
    }

    /**
     * Sets the header information for the assessment detail view.
     *
     * @param title    The assessment title (e.g., "Offline 1").
     * @param duration The duration text (e.g., "Duration: 1 Hour").
     * @param time     The time text (e.g., "Time: 10:00 AM - 11:00 AM").
     * @param room     The room text (e.g., "Room: 301").
     * @param syllabus The syllabus or questions content.
     */
    public void setHeaderInfo(String title, String duration, String time, String room, String syllabus) {
        titleField.setText(title);
        durationField.setText(duration);
        timeField.setText(time);
        roomField.setText(room);
        syllabusEditor.setHtmlText(syllabus != null ? syllabus : "");
    }

    /**
     * Unpacks the database POJO and populates the UI dynamically based on the assessment type.
     */
    public void setAssessmentData(Object assessment) {
        if (assessment == null) return;

        // 1. Set the shared parent data
        titleField.setText(getStringValue(assessment, "getTitle", "TBA"));
        timeField.setText(getStringValue(assessment, "getDateTime", "TBA"));
        roomField.setText(getStringValue(assessment, "getRoom", "TBA"));

        String className = assessment.getClass().getSimpleName();
        if ("CT".equals(className)) {
            syllabusEditor.setHtmlText("Syllabus:<br/>" + getStringValue(assessment, "getSyllabus", ""));
            durationField.setText(getStringValue(assessment, "getTotalMarks", ""));
        } else if ("Offline".equals(className) || "Assignment".equals(className)) {
            syllabusEditor.setHtmlText("Submission Link:<br/>" + getStringValue(assessment, "getSubmissionLink", ""));
            durationField.setText("Offline".equals(className) ? "Take-home Offline" : "Long-term Assignment");
        } else if ("Online".equals(className) || "Quiz".equals(className)) {
            syllabusEditor.setHtmlText("Online".equals(className)
                    ? "This is an online assessment. Be ready at the scheduled time."
                    : "Pop quiz/short assessment.");
            durationField.setText(getStringValue(assessment, "getDuration", ""));
        }
    }

    /**
     * Updates the submission count label in the Teacher Evaluation Area.
     *
     * @param submitted The number of submissions received.
     * @param total     The total number of expected submissions.
     */
    public void setSubmissionCount(int submitted, int total) {
        submissionCountLabel.setText("Submissions: " + submitted + " / " + total);
    }

    @FXML
    private void handleSaveDetails() {
        String htmlContent = syllabusEditor.getHtmlText() == null ? "" : syllabusEditor.getHtmlText().trim();
        if (isBlank(titleField.getText()) || isBlank(durationField.getText()) || isBlank(roomField.getText()) || isHtmlBlank(htmlContent)) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Please fill in all fields before saving.");
            alert.showAndWait();
            return;
        }
        System.out.println("Details saved to database");
    }

    @FXML
    private void handleFileUpload() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File to Upload");
        Window ownerWindow = uploadFileBtn != null && uploadFileBtn.getScene() != null ? uploadFileBtn.getScene().getWindow() : null;
        java.io.File selectedFile = fileChooser.showOpenDialog(ownerWindow);
        if (selectedFile != null) {
            System.out.println("Selected file: " + selectedFile.getAbsolutePath());
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean isHtmlBlank(String html) {
        String normalized = html.replaceAll("(?is)<(script|style)[^>]*>.*?</\\1>", "")
                .replaceAll("(?is)<[^>]+>", "")
                .replace("&nbsp;", " ")
                .trim();
        return normalized.isEmpty();
    }

    private boolean isStudentRole() {
        return "STUDENT".equals(getSessionValue("getCurrentUserRole"));
    }

    private String getCurrentUserName() {
        Object currentUser = getSessionValue("getCurrentUser");
        if (currentUser == null) {
            return "--";
        }
        try {
            Object name = currentUser.getClass().getMethod("getName").invoke(currentUser);
            return name != null ? name.toString() : "--";
        } catch (Exception ignored) {
            return "--";
        }
    }

    private Object getSessionValue(String methodName) {
        try {
            Class<?> sessionManager = Class.forName("com.syncron.controllers.SessionManager");
            return sessionManager.getMethod(methodName).invoke(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String getStringValue(Object target, String methodName, String fallback) {
        try {
            Object value = target.getClass().getMethod(methodName).invoke(target);
            if (value == null) {
                return fallback;
            }
            String text = value.toString();
            return text.isBlank() ? fallback : text;
        } catch (Exception ignored) {
            return fallback;
        }
    }
}

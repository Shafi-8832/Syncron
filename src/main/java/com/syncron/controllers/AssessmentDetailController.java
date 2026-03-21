package com.syncron.controllers;

import com.syncron.models.User;
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

import java.io.File;

import com.syncron.models.Assessment;
import com.syncron.models.CT;
import com.syncron.models.Offline;
import com.syncron.models.Online;
import com.syncron.models.Assignment;
import com.syncron.models.Quiz;

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
    @FXML private DateTimePickerComponent dateTimePickerController;
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

        User currentUser = SessionManager.getCurrentUser();
        String currentUserName = currentUser != null && currentUser.getName() != null ? currentUser.getName() : "--";
        createdByLabel.setText("Created by: " + currentUserName);

        if ("STUDENT".equals(SessionManager.getCurrentUserRole())) {
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
        syllabusEditor.setHtmlText(syllabus);
    }

    /**
     * Unpacks the database POJO and populates the UI dynamically based on the assessment type.
     */
    public void setAssessmentData(Assessment assessment) {
        if (assessment == null) return;

        // 1. Set the shared parent data
        titleField.setText(assessment.getTitle());
        timeField.setText(assessment.getDateTime() != null ? assessment.getDateTime() : "TBA");
        roomField.setText(assessment.getRoom() != null ? assessment.getRoom() : "TBA");

        // 2. Use Polymorphism to extract child-specific data!
        if (assessment instanceof CT ct) {
            syllabusEditor.setHtmlText("Syllabus:<br>" + toHtmlSafeText(ct.getSyllabus()));
            durationField.setText(String.valueOf(ct.getTotalMarks()));

        } else if (assessment instanceof Offline offline) {
            syllabusEditor.setHtmlText("Submission Link:<br>" + toHtmlSafeText(offline.getSubmissionLink()));
            durationField.setText("Take-home Offline");

        } else if (assessment instanceof Online online) {
            syllabusEditor.setHtmlText("This is an online assessment. Be ready at the scheduled time.");
            durationField.setText(online.getDuration());

        } else if (assessment instanceof Quiz quiz) {
            syllabusEditor.setHtmlText("Pop quiz/short assessment.");
            durationField.setText(quiz.getDuration());

        } else if (assessment instanceof Assignment assignment) {
            syllabusEditor.setHtmlText("Submission Link:<br>" + toHtmlSafeText(assignment.getSubmissionLink()));
            durationField.setText("Long-term Assignment");
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
        String htmlText = syllabusEditor.getHtmlText();
        String plainSyllabus = htmlText == null ? "" : htmlText
                .replaceAll("(?is)<[^>]*>", "")
                .replace("&nbsp;", " ")
                .trim();
        if (titleField.getText().trim().isEmpty()
                || durationField.getText().trim().isEmpty()
                || roomField.getText().trim().isEmpty()
                || plainSyllabus.isEmpty()) {
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
        fileChooser.setTitle("Select File");
        File selectedFile = fileChooser.showOpenDialog(uploadFileBtn.getScene().getWindow());
        if (selectedFile != null) {
            System.out.println("Selected file: " + selectedFile.getAbsolutePath());
        }
    }

    @FXML
    private void handleAddLink() {
        System.out.println("Add link clicked");
    }

    private String toHtmlSafeText(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;")
                .replace("\n", "<br>");
    }
}

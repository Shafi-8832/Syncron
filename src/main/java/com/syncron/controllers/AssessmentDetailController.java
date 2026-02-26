package com.syncron.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class AssessmentDetailController {

    // --- Header Section ---
    @FXML private Label titleLabel;
    @FXML private Label durationLabel;
    @FXML private Label timeLabel;
    @FXML private Label roomLabel;
    @FXML private TextArea syllabusArea;

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
    }

    /**
     * Initializes the view based on the user's role and the assessment status.
     * This method controls the RBAC (Role-Based Access Control) visibility logic.
     *
     * @param role   The role of the current user ("TEACHER" or "STUDENT").
     * @param status The status of the assessment (e.g., "PAST_DEADLINE").
     */
    public void initializeView(String role, String status) {
        if (role.equals("TEACHER")) {
            // Show Teacher Action Area and Evaluation Area
            teacherActionArea.setVisible(true);
            teacherActionArea.setManaged(true);
            teacherEvaluationArea.setVisible(true);
            teacherEvaluationArea.setManaged(true);

            // Hide Student Action Area
            studentActionArea.setVisible(false);
            studentActionArea.setManaged(false);
        } else if (role.equals("STUDENT")) {
            // Hide Teacher Action and Evaluation areas
            teacherActionArea.setVisible(false);
            teacherActionArea.setManaged(false);
            teacherEvaluationArea.setVisible(false);
            teacherEvaluationArea.setManaged(false);

            // Show Student Action Area
            studentActionArea.setVisible(true);
            studentActionArea.setManaged(true);

            // If past deadline, disable submission buttons
            if (status.equals("PAST_DEADLINE")) {
                uploadSubmissionButton.setDisable(true);
                editSubmissionButton.setDisable(true);
                removeSubmissionButton.setDisable(true);
            }
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
        titleLabel.setText(title);
        durationLabel.setText(duration);
        timeLabel.setText(time);
        roomLabel.setText(room);
        syllabusArea.setText(syllabus);
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
}

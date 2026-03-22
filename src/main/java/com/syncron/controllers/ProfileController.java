package com.syncron.controllers;

import com.syncron.models.User;
import com.syncron.utils.DatabaseHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class ProfileController {

    public static User viewingUser = null;
    private static final int MIN_PASSWORD_LENGTH = 8;
    private User loadedUser;

    // Main Boxes
    @FXML private VBox studentHistoryBox;
    @FXML private VBox teacherAssignedBox;
    @FXML private VBox passwordFormBox;

    // Top Profile Header
    @FXML private Label topHeaderNameLabel;
    @FXML private Label nameLabel;
    @FXML private Label roleLabel;
    @FXML private Label sectionLabel;

    // Chameleon Containers
    @FXML private HBox teacherInfoContainer;
    @FXML private VBox studentInfoContainer;

    // Teacher Specific Labels
    @FXML private Label teacherBioLabel;
    @FXML private Label teacherIdLabel;
    @FXML private Label teacherResearchLabel;
    @FXML private Label teacherContactLabel;
    @FXML private Label teacherGithubLabel;
    @FXML private Label teacherLinkedinLabel;
    @FXML private Label teacherFbLabel;
    @FXML private Label teacherRoomLabel;
    @FXML private Label teacherEmailLabel;

    // Student Specific Labels
    @FXML private Label studentBioLabel;
    @FXML private Label studentIdLabel;
    @FXML private Label studentSectionLabel;
    @FXML private Label studentClassroomLabel;
    @FXML private Label studentEmailLabel;
    @FXML private Label studentGithubLabel;
    @FXML private Label studentLinkedinLabel;
    @FXML private Label semesterBadgeLabel;

    // Password Fields
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label passwordErrorLabel;

    @FXML
    public void initialize() {
        User userToLoad = viewingUser != null ? viewingUser : SessionManager.getCurrentUser();
        if (viewingUser != null) {
            viewingUser = null;
        }
        loadUserData(userToLoad);
        hidePasswordForm();
    }

    private void loadUserData(User user) {
        if (user == null) return;
        this.loadedUser = user;

        // Set the universal info
        nameLabel.setText(user.getName());
        roleLabel.setText(user.getRole() != null ? user.getRole().toUpperCase() : "UNKNOWN");
        String email = user.getEmail() != null ? user.getEmail() : "--";
        String id = user.getId() != null ? user.getId() : "--";

        if (topHeaderNameLabel != null) {
            topHeaderNameLabel.setText(user.getName());
        }

        // --- THE PIXEL-PERFECT CHAMELEON LOGIC ---
        if ("STUDENT".equalsIgnoreCase(user.getRole())) {

            // Toggle containers
            studentInfoContainer.setVisible(true);
            studentInfoContainer.setManaged(true);
            teacherInfoContainer.setVisible(false);
            teacherInfoContainer.setManaged(false);

            studentHistoryBox.setVisible(true);
            studentHistoryBox.setManaged(true);
            teacherAssignedBox.setVisible(false);
            teacherAssignedBox.setManaged(false);

            // Populate student fields
            studentBioLabel.setText("Bio : Passionate CS student at BUET.");
            studentIdLabel.setText("Student ID : " + id);
            studentEmailLabel.setText("Email Address : " + email);
            studentGithubLabel.setText("GitHub : github.com/" + id);
            studentLinkedinLabel.setText("LinkedIn : linkedin.com/in/" + id);
            studentClassroomLabel.setText("Class Room : Room 402");

            if (user instanceof com.syncron.models.Student) {
                com.syncron.models.Student student = (com.syncron.models.Student) user;
                String sec = student.getSection() != null ? student.getSection() : "--";
                String subSec = student.getSubsection() != null ? student.getSubsection() : "--";
                studentSectionLabel.setText("Section : " + sec);
                sectionLabel.setText(subSec); // Badge next to name
            } else {
                studentSectionLabel.setText("Section : --");
                sectionLabel.setText("Student");
            }

            generateDynamicCourseList(studentHistoryBox, false);

        } else {

            // Toggle containers
            teacherInfoContainer.setVisible(true);
            teacherInfoContainer.setManaged(true);
            studentInfoContainer.setVisible(false);
            studentInfoContainer.setManaged(false);

            teacherAssignedBox.setVisible(true);
            teacherAssignedBox.setManaged(true);
            studentHistoryBox.setVisible(false);
            studentHistoryBox.setManaged(false);

            // Populate teacher fields
            teacherBioLabel.setText("Bio : ");
            teacherIdLabel.setText("Teacher ID : " + id);
            teacherResearchLabel.setText("Research Interest : ");
            teacherContactLabel.setText("Contact No. : ");
            teacherGithubLabel.setText("Github Link : ");
            teacherLinkedinLabel.setText("LinkedIn Link : ");
            teacherFbLabel.setText("FB Link : ");
            teacherRoomLabel.setText("Room No : ");
            teacherEmailLabel.setText("Email Address : " + email);
            sectionLabel.setText(id); // Badge next to name


            generateDynamicCourseList(teacherAssignedBox, true);
        }
    }

    // --- The Course Link Routing Method ---
    @FXML
    private void handleCourseClick(javafx.scene.input.MouseEvent event) {
        Label clickedLabel = (Label) event.getSource();
        String fullText = clickedLabel.getText();

        String courseCode = fullText;
        String courseTitle = "";
        if (fullText.contains(" — ")) {
            String[] parts = fullText.split(" — ");
            courseCode = parts[0].trim();
            courseTitle = parts[1].trim();
        }

        try {
            viewingUser = null;
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/syncron/views/main_layout.fxml"));
            javafx.scene.Parent root = loader.load();

            MainController controller = loader.getController();
            controller.setCourseContext(courseCode, courseTitle, "theory");

            javafx.stage.Stage stage = (javafx.stage.Stage) passwordFormBox.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleShowPasswordForm() {
        passwordFormBox.setVisible(true);
        passwordFormBox.setManaged(true);
        passwordErrorLabel.setText("");
    }

    @FXML
    private void handleCancelPasswordChange() {
        hidePasswordForm();
    }

    @FXML
    private void handleChangePassword() {
        String currentPass = currentPasswordField.getText() == null ? "" : currentPasswordField.getText();
        String newPass = newPasswordField.getText() == null ? "" : newPasswordField.getText();
        String confirmPass = confirmPasswordField.getText() == null ? "" : confirmPasswordField.getText();

        if (currentPass.isBlank() || newPass.isBlank() || confirmPass.isBlank()) {
            passwordErrorLabel.setStyle("-fx-text-fill: #D32F2F;");
            passwordErrorLabel.setText("All password fields are required");
            return;
        }

        if (!newPass.equals(confirmPass)) {
            passwordErrorLabel.setStyle("-fx-text-fill: #D32F2F;");
            passwordErrorLabel.setText("Passwords do not match");
            return;
        }

        if (newPass.length() < MIN_PASSWORD_LENGTH) {
            passwordErrorLabel.setStyle("-fx-text-fill: #D32F2F;");
            passwordErrorLabel.setText("New password must be at least " + MIN_PASSWORD_LENGTH + " characters");
            return;
        }

        if (loadedUser == null || loadedUser.getId() == null || loadedUser.getId().isBlank()) {
            passwordErrorLabel.setStyle("-fx-text-fill: #D32F2F;");
            passwordErrorLabel.setText("Unable to change password");
            return;
        }

        boolean updated = DatabaseHandler.updatePassword(loadedUser.getId(), currentPass, newPass);
        if (updated) {
            hidePasswordForm();
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText(null);
            alert.setContentText("Password changed successfully.");
            alert.showAndWait();
        } else {
            passwordErrorLabel.setStyle("-fx-text-fill: #D32F2F;");
            passwordErrorLabel.setText("Current password does not match");
        }
    }

    @FXML
    private void goBackToDashboard() {
        try {
            viewingUser = null;
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/syncron/views/home.fxml"));
            javafx.scene.Parent root = loader.load();

            javafx.stage.Stage stage = (javafx.stage.Stage) passwordFormBox.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout() {
        try {
            viewingUser = null;
            SessionManager.setCurrentUser(null);

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/syncron/views/login.fxml"));
            javafx.scene.Parent root = loader.load();

            javafx.stage.Stage stage = (javafx.stage.Stage) passwordFormBox.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    private void generateDynamicCourseList(VBox containerBox, boolean isTeacher) {
        // 1. Clear any existing children EXCEPT the first label ("Level 1 Term 2")
        if (containerBox.getChildren().size() > 1) {
            containerBox.getChildren().remove(1, containerBox.getChildren().size());
        }

        // 2. Fetch courses. (Eventually: DatabaseHandler.getUserCourses(loadedUser.getId()))
        // For right now, we grab all courses to prove the UI works!
        java.util.List<com.syncron.models.Course> courses = DatabaseHandler.getAllCourses();

        // 3. Create the 2-Column Structure
        HBox mainRow = new HBox(40);
        VBox theoryCol = new VBox(10);
        VBox sessionalCol = new VBox(10);
        javafx.scene.layout.HBox.setHgrow(theoryCol, javafx.scene.layout.Priority.ALWAYS);
        javafx.scene.layout.HBox.setHgrow(sessionalCol, javafx.scene.layout.Priority.ALWAYS);

        // 4. Create Headers
        Label tHeader = new Label("THEORY");
        tHeader.setStyle("-fx-text-fill: #D35400; -fx-font-weight: bold;");
        Label sHeader = new Label("SESSIONAL");
        sHeader.setStyle("-fx-text-fill: #D35400; -fx-font-weight: bold;");
        theoryCol.getChildren().add(tHeader);
        sessionalCol.getChildren().add(sHeader);

        // 5. Loop through Database courses and build the UI rows!
        for (com.syncron.models.Course c : courses) {
            String fullText = c.getCourseCode() + " — " + c.getCourseTitle();

            // Auto-detect if it's a Sessional (BUET theory courses usually end in odd numbers, sessionals in even)
            boolean isSessional = c.getCourseTitle().toLowerCase().contains("sessional") ||
                    c.getCourseCode().endsWith("2") || c.getCourseCode().endsWith("4") ||
                    c.getCourseCode().endsWith("6") || c.getCourseCode().endsWith("8");

            HBox courseRow = new HBox(10);
            courseRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            Label courseLabel = new Label(fullText);

            // Dynamic Colors: Teacher gets Orange, Students get Dark Blue / Gray
            String textColor = isTeacher ? "#E67E22" : (isSessional ? "#7F8C8D" : "#2C3E50");
            courseLabel.setStyle("-fx-text-fill: " + textColor + "; -fx-cursor: hand;");

            // Attach the router!
            courseLabel.setOnMouseClicked(this::handleCourseClick);
            courseRow.getChildren().add(courseLabel);

            if (isTeacher) {
                Label badge = new Label("L1T2");
                badge.getStyleClass().add("badge-outline-gray");
                courseRow.getChildren().add(badge);
            }

            if (isSessional) {
                sessionalCol.getChildren().add(courseRow);
            } else {
                theoryCol.getChildren().add(courseRow);
            }
        }

        mainRow.getChildren().addAll(theoryCol, sessionalCol);
        containerBox.getChildren().add(mainRow);
    }

    private void hidePasswordForm() {
        passwordFormBox.setVisible(false);
        passwordFormBox.setManaged(false);
        clearPasswordFields();
        passwordErrorLabel.setText("");
    }

    private void clearPasswordFields() {
        currentPasswordField.clear();
        newPasswordField.clear();
        confirmPasswordField.clear();
    }
}
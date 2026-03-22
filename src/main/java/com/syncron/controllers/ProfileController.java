package com.syncron.controllers;

import com.syncron.models.Student;
import com.syncron.models.Teacher;
import com.syncron.models.User;
import com.syncron.utils.DatabaseHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class ProfileController {

    public static User viewingUser = null;
    private static final int MIN_PASSWORD_LENGTH = 8;

    @FXML private GridPane infoGrid;
    @FXML private VBox studentHistoryBox;
    @FXML private VBox teacherAssignedBox;
    @FXML private VBox passwordFormBox;
    @FXML private Label nameLabel;
    @FXML private Label roleLabel;
    @FXML private Label sectionLabel;
    @FXML private Label emailValueLabel;
    @FXML private Label phoneValueLabel;
    @FXML private Label departmentValueLabel;
    @FXML private Label officeValueLabel;
    @FXML private Label joinedValueLabel;
    @FXML private Label advisingHoursValueLabel;
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label passwordErrorLabel;
    @FXML private Button changePasswordButton;

    private User loadedUser;

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
        loadedUser = user;
        if (user == null) {
            return;
        }

        nameLabel.setText(orDash(user.getName()));
        roleLabel.setText(orDash(user.getRole()));
        emailValueLabel.setText(orDash(user.getEmail()));
        phoneValueLabel.setText("--");
        departmentValueLabel.setText("--");
        officeValueLabel.setText("--");
        joinedValueLabel.setText("--");
        advisingHoursValueLabel.setText("--");

        String role = user.getRole() == null ? "" : user.getRole().toUpperCase();
        if ("STUDENT".equals(role)) {
            studentHistoryBox.setVisible(true);
            studentHistoryBox.setManaged(true);
            teacherAssignedBox.setVisible(false);
            teacherAssignedBox.setManaged(false);
            if (user instanceof Student student) {
                sectionLabel.setText(student.getSection() == null || student.getSection().isBlank()
                        ? "Student"
                        : "Sec: " + student.getSection());
            } else {
                sectionLabel.setText("Student");
            }
        } else if ("TEACHER".equals(role)) {
            teacherAssignedBox.setVisible(true);
            teacherAssignedBox.setManaged(true);
            studentHistoryBox.setVisible(false);
            studentHistoryBox.setManaged(false);
            if (user instanceof Teacher teacher) {
                sectionLabel.setText(teacher.getDesignation() == null || teacher.getDesignation().isBlank()
                        ? "Teacher"
                        : teacher.getDesignation());
            } else {
                sectionLabel.setText("Teacher");
            }
        } else {
            studentHistoryBox.setVisible(false);
            studentHistoryBox.setManaged(false);
            teacherAssignedBox.setVisible(false);
            teacherAssignedBox.setManaged(false);
            sectionLabel.setText("--");
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

    private String orDash(String value) {
        return value == null || value.isBlank() ? "--" : value;
    }
}

package com.syncron.controllers;

import com.syncron.models.User;
import com.syncron.utils.DatabaseHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class LoginController {

    @FXML private VBox loginBox;
    @FXML private VBox signupBox;

    // --- LOGIN FIELDS ---
    @FXML private Button loginStudentBtn, loginTeacherBtn, loginAdminBtn;
    @FXML private Label loginIdTitle, loginErrorLabel;
    @FXML private TextField loginIdField;
    @FXML private PasswordField loginPasswordField;
    private String currentLoginRole = "STUDENT";

    // --- SIGNUP FIELDS ---
    @FXML private Button signupStudentBtn, signupTeacherBtn, signupAdminBtn;
    @FXML private Label signupIdTitle, signupHelperLabel, signupErrorLabel;
    @FXML private TextField signupNameField, signupIdField;
    private String currentSignupRole = "STUDENT";

    @FXML
    public void initialize() {
        showLogin();
    }

    // --- VIEW TOGGLING ---
    @FXML
    private void showSignup() {
        loginBox.setVisible(false);
        signupBox.setVisible(true);
        loginErrorLabel.setVisible(false);
    }

    @FXML
    private void showLogin() {
        signupBox.setVisible(false);
        loginBox.setVisible(true);
        signupErrorLabel.setVisible(false);
    }

    // --- ROLE SELECTION LOGIC (LOGIN) ---
    @FXML private void setLoginRoleStudent() { updateLoginRoleUI("STUDENT", loginStudentBtn, "Student ID"); }
    @FXML private void setLoginRoleTeacher() { updateLoginRoleUI("TEACHER", loginTeacherBtn, "Email Address"); }
    @FXML private void setLoginRoleAdmin()   { updateLoginRoleUI("ADMIN", loginAdminBtn, "Admin ID"); }

    private void updateLoginRoleUI(String role, Button activeBtn, String titleText) {
        currentLoginRole = role;
        loginStudentBtn.getStyleClass().setAll("role-btn");
        loginTeacherBtn.getStyleClass().setAll("role-btn");
        loginAdminBtn.getStyleClass().setAll("role-btn");
        activeBtn.getStyleClass().setAll("role-btn-active");

        loginIdTitle.setText(titleText);
        loginIdField.setPromptText("Enter your " + titleText);
    }

    // --- ROLE SELECTION LOGIC (SIGNUP) ---
    @FXML private void setSignupRoleStudent() { updateSignupRoleUI("STUDENT", signupStudentBtn, "Student ID", "Type your name exactly as it appears in your Student ID Card"); }
    @FXML private void setSignupRoleTeacher() { updateSignupRoleUI("TEACHER", signupTeacherBtn, "Email Address", "Use your official university email address"); }

    @FXML
    private void setSignupRoleAdmin() {
        // Admins cannot sign up! Automatically flip them to the Login screen
        showLogin();
        setLoginRoleAdmin();
    }

    private void updateSignupRoleUI(String role, Button activeBtn, String titleText, String helperText) {
        currentSignupRole = role;
        signupStudentBtn.getStyleClass().setAll("role-btn");
        signupTeacherBtn.getStyleClass().setAll("role-btn");
        signupAdminBtn.getStyleClass().setAll("role-btn");
        activeBtn.getStyleClass().setAll("role-btn-active");

        signupIdTitle.setText(titleText);
        signupIdField.setPromptText("Enter your " + titleText);
        signupHelperLabel.setText(helperText);
    }

    // --- AUTHENTICATION ACTIONS ---
    @FXML
    private void handleLogin() {
        String id = loginIdField.getText();
        String password = loginPasswordField.getText();

        if (id == null || id.isBlank() || password == null || password.isBlank()) {
            showError(loginErrorLabel, "Please enter your ID and password.");
            return;
        }

        String dbRole = DatabaseHandler.authenticateUser(id, password);

        if (dbRole != null && dbRole.equalsIgnoreCase(currentLoginRole)) {

            String name = DatabaseHandler.getUserNameById(id);
            String email = id + "@buet.ac.bd"; // Fallback email
            User sessionUser;

            if ("STUDENT".equalsIgnoreCase(dbRole)) {
                sessionUser = new com.syncron.models.Student(id, name, email, password, false, "");
            } else if ("TEACHER".equalsIgnoreCase(dbRole)) {
                sessionUser = new com.syncron.models.Teacher(id, name, email, password, "Teacher");
            } else {
                sessionUser = new com.syncron.models.Teacher(id, name, email, password, "Admin");
            }

            SessionManager.setCurrentUser(sessionUser);

            try {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/syncron/views/home.fxml"));
                javafx.scene.Parent root = loader.load();
                javafx.stage.Stage stage = (javafx.stage.Stage) loginBox.getScene().getWindow();
                stage.getScene().setRoot(root);
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else if (dbRole != null) {
            showError(loginErrorLabel, "Invalid role. Please select " + dbRole);
        } else {
            showError(loginErrorLabel, "Invalid credentials or account is pending approval.");
        }
    }

    @FXML
    private void handleSignup() {
        String name = signupNameField.getText();
        String id = signupIdField.getText();

        if (name == null || name.isBlank() || id == null || id.isBlank()) {
            showError(signupErrorLabel, "All fields are required.");
            return;
        }

        // Add to database with PENDING status. Admin sets password later!
        String sql = "INSERT INTO users (id, name, email, password, role, status) VALUES (?, ?, ?, ?, ?, 'PENDING')";

        try (Connection conn = DatabaseHandler.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, id); // Works for both Student ID and Teacher Email
            pstmt.setString(2, name);
            pstmt.setString(3, currentSignupRole.equals("TEACHER") ? id : id + "@buet.ac.bd");
            pstmt.setString(4, "PENDING_APPROVAL_PASS"); // Dummy password so DB doesn't complain
            pstmt.setString(5, currentSignupRole);

            pstmt.executeUpdate();

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText("Request Submitted!");
            alert.setContentText("Your account has been requested. An Admin will approve it and provide your password.");
            alert.showAndWait();

            signupNameField.clear(); signupIdField.clear();
            showLogin();

        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE") || e.getMessage().contains("PRIMARY KEY")) {
                showError(signupErrorLabel, "An account with this ID or Email already exists.");
            } else {
                showError(signupErrorLabel, "Database Error: " + e.getMessage());
            }
        }
    }

    private void showError(Label label, String message) {
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);
    }
}
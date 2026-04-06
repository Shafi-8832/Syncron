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
        // EXACT match to your FXML variables
        String loginId = loginIdField.getText();
        String password = loginPasswordField.getText();

        if (loginId.isEmpty() || password.isEmpty()) {
            showError(loginErrorLabel, "Fields cannot be empty.");
            return;
        }

        try {
            // 1. Package credentials into JSON
            String jsonPayload = String.format("{\"email\":\"%s\",\"password\":\"%s\"}", loginId, password);

            // 2. Build the Network Request
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://localhost:8080/api/login"))
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            // 3. Shoot it over the Wi-Fi!
            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            // 4. Check the Server's Response
            if (response.statusCode() == 200) {
                String responseBody = response.body();

                // Extract data
                String id = extractJsonValue(responseBody, "id");
                String name = extractJsonValue(responseBody, "name");
                String role = extractJsonValue(responseBody, "role");
                String userEmail = extractJsonValue(responseBody, "email");


                // THE VAULT DOOR: Strict Role Verification
                if (!role.equalsIgnoreCase(currentLoginRole)) {
                    System.out.println("❌ Security Block: Tried to log in as " + role + " from the " + currentLoginRole + " tab.");
                    showError(loginErrorLabel, "Role mismatch! Please select the " + role + " tab.");
                    return; // ABORT THE LOGIN!
                }


                // PERFECT INSTANTIATION: Matching your exact Student and Teacher constructors!
                // We pass "" for password since the server authenticated it, and default values for isCR/designation
                com.syncron.models.User loggedInUser;

                if ("TEACHER".equalsIgnoreCase(role)) {
                    // Teacher(id, name, email, password, designation)
                    loggedInUser = new com.syncron.models.Teacher(id, name, userEmail, "", "Faculty");
                } else if ("ADMIN".equalsIgnoreCase(role)) {
                    // Assuming Admin extends User. (If you don't have an Admin class yet, use Teacher temporarily)
                    loggedInUser = new com.syncron.models.Teacher(id, name, userEmail, "", "System Admin");
                    loggedInUser.setRole("ADMIN");
                } else {
                    // Student(id, name, email, password, isCR)
                    loggedInUser = new com.syncron.models.Student(id, name, userEmail, "", false);
                }

                // Force the role to match perfectly
                loggedInUser.setRole(role.toUpperCase());

                // Save to session and teleport!
                com.syncron.controllers.SessionManager.setCurrentUser(loggedInUser);

                // HARD JUMP to the Home Dashboard
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/syncron/views/home.fxml"));
                javafx.scene.Parent root = loader.load();

                // Grab the current window using one of our fields, and swap the scene!
                javafx.stage.Stage stage = (javafx.stage.Stage) loginIdField.getScene().getWindow();
                stage.getScene().setRoot(root);

            } else {
                showError(loginErrorLabel, "Incorrect ID/Email or password.");
                System.out.println("Server Response Code: " + response.statusCode());
                System.out.println("Server Said: " + response.body());
                showError(loginErrorLabel, "Incorrect ID/Email or password.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            showError(loginErrorLabel, "❌ Could not connect to the Kernel Server.");
        }
    }

    // A tiny helper method to parse the JSON so we don't have to install new libraries today
    private String extractJsonValue(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) return "";
        start += search.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }

    @FXML
    private void handleSignup() {
        String name = signupNameField.getText();
        String id = signupIdField.getText();

        if (name == null || name.isBlank() || id == null || id.isBlank()) {
            showError(signupErrorLabel, "All fields are required.");
            return;
        }

        try {
            // 1. Figure out the proper email
            String email = currentSignupRole.equals("TEACHER") ? id : id + "@buet.ac.bd";

            // 2. Package the JSON payload
            String jsonPayload = String.format(
                    "{\"id\":\"%s\",\"name\":\"%s\",\"email\":\"%s\",\"role\":\"%s\"}",
                    id, name, email, currentSignupRole
            );

            // 3. Shoot it over the Wi-Fi!
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://localhost:8080/api/signup"))
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            // 4. Handle the Server's response
            if (response.statusCode() == 200) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setHeaderText("Request Submitted!");
                alert.setContentText("Your account has been requested. An Admin will approve it and provide your password.");
                alert.showAndWait();

                signupNameField.clear();
                signupIdField.clear();
                showLogin();
            } else if (response.statusCode() == 409) { // 409 Conflict = Already exists
                showError(signupErrorLabel, "An account with this ID or Email already exists.");
            } else {
                showError(signupErrorLabel, "Server Error: Could not create account.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            showError(signupErrorLabel, "❌ Network Error: Could not connect to the Kernel Server.");
        }
    }

    private void showError(Label label, String message) {
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);
    }
}
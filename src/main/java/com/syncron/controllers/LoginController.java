package com.syncron.controllers;

import com.syncron.models.User;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;

import java.util.Optional;

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

    // Glowing Colors
    private final String COLOR_STUDENT = "#2ECC71"; // Green
    private final String COLOR_TEACHER = "#3498DB"; // Blue
    private final String COLOR_ADMIN   = "#E74C3C"; // Red

    @FXML
    public void initialize() {
        showLogin();
        setLoginRoleStudent(); // Trigger initial glow
    }

    @FXML
    private void showSignup() {
        loginBox.setVisible(false);
        signupBox.setVisible(true);
        loginErrorLabel.setVisible(false);
        setSignupRoleStudent();
    }

    @FXML
    private void showLogin() {
        signupBox.setVisible(false);
        loginBox.setVisible(true);
        signupErrorLabel.setVisible(false);
        setLoginRoleStudent();
    }

    // --- GLOWING ROLE SELECTION (LOGIN) ---
    @FXML private void setLoginRoleStudent() { updateLoginRoleUI("STUDENT", loginStudentBtn, "Student ID", COLOR_STUDENT); }
    @FXML private void setLoginRoleTeacher() { updateLoginRoleUI("TEACHER", loginTeacherBtn, "Email Address", COLOR_TEACHER); }
    @FXML private void setLoginRoleAdmin()   { updateLoginRoleUI("ADMIN", loginAdminBtn, "Admin ID", COLOR_ADMIN); }

    private void updateLoginRoleUI(String role, Button activeBtn, String titleText, String glowColor) {
        currentLoginRole = role;

        // Reset all
        String baseStyle = "-fx-background-color: transparent; -fx-text-fill: #7F8C8D; -fx-font-weight: bold; -fx-cursor: hand;";
        loginStudentBtn.setStyle(baseStyle);
        loginTeacherBtn.setStyle(baseStyle);
        loginAdminBtn.setStyle(baseStyle);

        // Apply Premium Glow to active button
        activeBtn.setStyle("-fx-background-color: " + glowColor + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, " + glowColor + "80, 15, 0, 0, 4);");

        // Make input fields glow with the same color when focused!
        String fieldGlow = "-fx-background-color: #F8F9F9; -fx-border-color: " + glowColor + "; -fx-border-radius: 6; -fx-padding: 10; -fx-effect: dropshadow(three-pass-box, " + glowColor + "40, 10, 0, 0, 0);";
        String fieldNormal = "-fx-background-color: #F8F9F9; -fx-border-color: #BDC3C7; -fx-border-radius: 6; -fx-padding: 10;";

        loginIdField.setStyle(fieldNormal);
        loginPasswordField.setStyle(fieldNormal);

        loginIdField.focusedProperty().addListener((obs, old, isFocused) -> loginIdField.setStyle(isFocused ? fieldGlow : fieldNormal));
        loginPasswordField.focusedProperty().addListener((obs, old, isFocused) -> loginPasswordField.setStyle(isFocused ? fieldGlow : fieldNormal));

        loginIdTitle.setText(titleText);
        loginIdField.setPromptText("Enter your " + titleText);
    }

    // --- GLOWING ROLE SELECTION (SIGNUP) ---
    @FXML private void setSignupRoleStudent() { updateSignupRoleUI("STUDENT", signupStudentBtn, "Student ID", "Type your name exactly as it appears in your Student ID Card", COLOR_STUDENT); }
    @FXML private void setSignupRoleTeacher() { updateSignupRoleUI("TEACHER", signupTeacherBtn, "ID or Initials", "Your email will be auto-generated: fullname@cse.buet.ac.bd", COLOR_TEACHER); }
    @FXML private void setSignupRoleAdmin() { showLogin(); setLoginRoleAdmin(); }

    private void updateSignupRoleUI(String role, Button activeBtn, String titleText, String helperText, String glowColor) {
        currentSignupRole = role;

        String baseStyle = "-fx-background-color: transparent; -fx-text-fill: #7F8C8D; -fx-font-weight: bold; -fx-cursor: hand;";
        signupStudentBtn.setStyle(baseStyle);
        signupTeacherBtn.setStyle(baseStyle);
        signupAdminBtn.setStyle(baseStyle);

        activeBtn.setStyle("-fx-background-color: " + glowColor + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, " + glowColor + "80, 15, 0, 0, 4);");

        String fieldGlow = "-fx-background-color: #F8F9F9; -fx-border-color: " + glowColor + "; -fx-border-radius: 6; -fx-padding: 10; -fx-effect: dropshadow(three-pass-box, " + glowColor + "40, 10, 0, 0, 0);";
        String fieldNormal = "-fx-background-color: #F8F9F9; -fx-border-color: #BDC3C7; -fx-border-radius: 6; -fx-padding: 10;";

        signupNameField.setStyle(fieldNormal);
        signupIdField.setStyle(fieldNormal);

        signupNameField.focusedProperty().addListener((obs, old, isFocused) -> signupNameField.setStyle(isFocused ? fieldGlow : fieldNormal));
        signupIdField.focusedProperty().addListener((obs, old, isFocused) -> signupIdField.setStyle(isFocused ? fieldGlow : fieldNormal));

        signupIdTitle.setText(titleText);
        signupIdField.setPromptText("Enter your " + titleText);
        signupHelperLabel.setText(helperText);
    }

    // --- AUTHENTICATION ACTIONS ---
    @FXML
    private void handleLogin() {
        String loginId = loginIdField.getText();
        String password = loginPasswordField.getText();

        if (loginId.isEmpty() || password.isEmpty()) {
            showError(loginErrorLabel, "Fields cannot be empty.");
            return;
        }

        try {
            String jsonPayload = String.format("{\"email\":\"%s\",\"password\":\"%s\"}", loginId, password);
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://localhost:8080/api/login"))
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonPayload)).build();

            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String responseBody = response.body();
                String id = extractJsonValue(responseBody, "id");
                String name = extractJsonValue(responseBody, "name");
                String role = extractJsonValue(responseBody, "role");
                String userEmail = extractJsonValue(responseBody, "email");
                String status = extractJsonValue(responseBody, "status"); // Grab the status

                if (!role.equalsIgnoreCase(currentLoginRole)) {
                    showError(loginErrorLabel, "Role mismatch! Please select the " + role + " tab.");
                    return;
                }

                com.syncron.models.User loggedInUser;
                if ("TEACHER".equalsIgnoreCase(role)) {
                    loggedInUser = new com.syncron.models.Teacher(id, name, userEmail, "", "Faculty");
                } else if ("ADMIN".equalsIgnoreCase(role)) {
                    loggedInUser = new com.syncron.models.Teacher(id, name, userEmail, "", "System Admin");
                    loggedInUser.setRole("ADMIN");
                } else {
                    loggedInUser = new com.syncron.models.Student(id, name, userEmail, "", false);
                }

                loggedInUser.setRole(role.toUpperCase());
                com.syncron.controllers.SessionManager.setCurrentUser(loggedInUser);

                //
                String targetFxml = "/com/syncron/views/home.fxml"; // Default

                if ("ADMIN".equalsIgnoreCase(role)) {
                    targetFxml = "/com/syncron/views/admin_dashboard.fxml";
                } else if ("PENDING".equalsIgnoreCase(status)) {
                    targetFxml = "/com/syncron/views/pending_dashboard.fxml"; // Fixed typo here
                }

                // THE FIX: Use targetFxml instead of the hardcoded string
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource(targetFxml));
                javafx.scene.Parent root = loader.load();
                javafx.stage.Stage stage = (javafx.stage.Stage) loginIdField.getScene().getWindow();
                stage.getScene().setRoot(root);

            } else {
                showError(loginErrorLabel, "Incorrect ID/Email or password.");
            } //

        } catch (Exception e) {
            showError(loginErrorLabel, "❌ Could not connect to the Kernel Server.");
        }
    }

    @FXML
    private void handleSignup() {
        String name = signupNameField.getText().trim();
        String id = signupIdField.getText().trim();

        if (name.isEmpty() || id.isEmpty()) {
            showError(signupErrorLabel, "All fields are required.");
            return;
        }

        try {
            //  THE UNIQUE TEACHER EMAIL GENERATOR
            String email;
            if (currentSignupRole.equals("TEACHER")) {
                // Converts "Md Nurul Muttakin" -> "md.nurul.muttakin@cse.buet.ac.bd"
                email = name.toLowerCase().replaceAll("\\s+", ".") + "@cse.buet.ac.bd";
            } else {
                email = id + "@buet.ac.bd";
            }

            String jsonPayload = String.format("{\"id\":\"%s\",\"name\":\"%s\",\"email\":\"%s\",\"role\":\"%s\"}", id, name, email, currentSignupRole);
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://localhost:8080/api/signup"))
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonPayload)).build();

            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setHeaderText("Request Submitted!");
                alert.setContentText(currentSignupRole.equals("TEACHER") ?
                        "Your teacher account has been requested.\nGenerated Email: " + email :
                        "Your student account has been requested.");
                alert.showAndWait();
                showLogin();
            } else {
                showError(signupErrorLabel, "Account already exists or server error.");
            }
        } catch (Exception e) {
            showError(signupErrorLabel, "❌ Network Error.");
        }
    }

    // --- THE MODERN FORGOT PASSWORD SYSTEM ---
    @FXML
    private void handleForgotPassword() {
        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle("Password Recovery");
        dialog.setHeaderText(null);

        VBox content = new VBox(15);
        content.setStyle("-fx-padding: 20; -fx-background-color: #FFFFFF;");

        Label titleLbl = new Label("🔒 Reset Your Password");
        titleLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

        TextField emailField = new TextField();
        emailField.setPromptText("Enter your Student ID or Email");
        emailField.setStyle("-fx-background-color: #F8F9F9; -fx-border-color: #D5DBDB; -fx-border-radius: 6; -fx-padding: 10; -fx-pref-width: 300;");

        PasswordField newPassField = new PasswordField();
        newPassField.setPromptText("Enter new secure password");
        newPassField.setStyle("-fx-background-color: #F8F9F9; -fx-border-color: #D5DBDB; -fx-border-radius: 6; -fx-padding: 10;");

        content.getChildren().addAll(titleLbl, new Label("Account Identifier:"), emailField, new Label("New Password:"), newPassField);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setStyle("-fx-background-color: #D35400; -fx-text-fill: white; -fx-font-weight: bold;");

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK && !emailField.getText().isEmpty() && !newPassField.getText().isEmpty()) {
                return new String[]{emailField.getText(), newPassField.getText()};
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            try {
                String json = String.format("{\"email\":\"%s\", \"newPassword\":\"%s\"}", result[0], result[1]);
                java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
                java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create("http://localhost:8080/api/reset-password"))
                        .header("Content-Type", "application/json")
                        .POST(java.net.http.HttpRequest.BodyPublishers.ofString(json)).build();

                java.net.http.HttpResponse<String> res = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());

                if (res.statusCode() == 200) {
                    new Alert(Alert.AlertType.INFORMATION, "Password securely reset! You may now log in.").show();
                } else {
                    new Alert(Alert.AlertType.ERROR, "User not found. Please check your ID/Email.").show();
                }
            } catch (Exception e) {
                new Alert(Alert.AlertType.ERROR, "Network error during password reset.").show();
            }
        });
    }

    private String extractJsonValue(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) return "";
        start += search.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }

    private void showError(Label label, String message) {
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);
    }
}
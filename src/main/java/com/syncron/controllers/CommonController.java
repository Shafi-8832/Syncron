package com.syncron.controllers;

import com.syncron.models.User;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class CommonController {

    @FXML private VBox resourceHubContainer;

    private User currentUser;
    private String currentCourse;

    @FXML
    public void initialize() {
        currentUser = SessionManager.getCurrentUser();
        currentCourse = SessionManager.getCurrentCourseCode();

        if (currentCourse == null || currentCourse.isEmpty()) return;
        fetchAndRenderResources();
    }

    private void fetchAndRenderResources() {
        resourceHubContainer.getChildren().clear();

        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://localhost:8080/api/resources/" + currentCourse.replace(" ", "%20")))
                    .GET().build();

            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                com.google.gson.Gson gson = new com.google.gson.Gson();
                java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<Map<String, String>>>(){}.getType();
                List<Map<String, String>> allResources = gson.fromJson(response.body(), listType);

                Map<String, List<Map<String, String>>> teacherMap = new HashMap<>();
                Map<String, String> teacherNames = new HashMap<>();

                for (Map<String, String> res : allResources) {
                    String tid = res.get("uploaderId");
                    teacherMap.putIfAbsent(tid, new ArrayList<>());
                    teacherMap.get(tid).add(res);
                    teacherNames.put(tid, res.get("uploaderName"));
                }

                if ("TEACHER".equals(currentUser.getRole())) {
                    teacherMap.putIfAbsent(currentUser.getId(), new ArrayList<>());
                    teacherNames.put(currentUser.getId(), currentUser.getName());
                }

                List<String> sortedTeacherIds = new ArrayList<>(teacherMap.keySet());
                sortedTeacherIds.sort((id1, id2) -> {
                    if (id1.equals(currentUser.getId())) return -1;
                    if (id2.equals(currentUser.getId())) return 1;
                    return teacherNames.get(id1).compareToIgnoreCase(teacherNames.get(id2));
                });

                for (String tid : sortedTeacherIds) {
                    String boxTitle = teacherNames.get(tid) + "'s Resources";
                    if (tid.equals(currentUser.getId())) boxTitle = "Your Resources (Uploads)";

                    resourceHubContainer.getChildren().add(buildResourceBox(boxTitle, tid, teacherMap.get(tid)));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private VBox buildResourceBox(String title, String ownerId, List<Map<String, String>> resources) {
        VBox box = new VBox(15);
        box.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #EAECEE; -fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 25; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.06), 15, 0, 0, 5);");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");
        box.getChildren().add(titleLabel);

        if (resources == null || resources.isEmpty()) {
            Label empty = new Label("No resources uploaded yet.");
            empty.setStyle("-fx-text-fill: #95A5A6; -fx-font-style: italic; -fx-padding: 10 0;");
            box.getChildren().add(empty);
        } else {
            for (Map<String, String> res : resources) {
                HBox row = new HBox(12);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle("-fx-background-color: #F8F9F9; -fx-background-radius: 6; -fx-padding: 12;");

                String type = res.get("resourceType");
                Label icon = new Label("FILE".equals(type) ? "📄" : "🔗");
                icon.setStyle("-fx-font-size: 18px; -fx-text-fill: #3498DB;");

                VBox info = new VBox(2);
                Label resTitle = new Label(res.get("title"));
                resTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");
                Label resDate = new Label("Uploaded: " + res.get("timestamp"));
                resDate.setStyle("-fx-font-size: 11px; -fx-text-fill: #7F8C8D;");
                info.getChildren().addAll(resTitle, resDate);

                // 👉 THE GLOWING DOWNLOAD BUTTON UPGRADE
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                HBox actionButtons = new HBox(10);
                actionButtons.setAlignment(Pos.CENTER_RIGHT);

                Button openBtn = new Button("Open");
                openBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #2980B9; -fx-cursor: hand; -fx-font-weight: bold;");
                openBtn.setOnAction(e -> openResource(res.get("resourceUrl"), type));
                actionButtons.getChildren().add(openBtn);

                if ("FILE".equals(type)) {
                    Button dlBtn = new Button("⬇ Download");
                    String baseStyle = "-fx-background-color: transparent; -fx-text-fill: #1ABC9C; -fx-font-weight: bold; -fx-cursor: hand; -fx-border-color: #1ABC9C; -fx-border-radius: 20; -fx-padding: 4 12;";
                    String hoverStyle = baseStyle + " -fx-background-color: #1ABC9C; -fx-text-fill: white; -fx-effect: dropshadow(three-pass-box, rgba(26, 188, 156, 0.4), 10, 0, 0, 0);";

                    dlBtn.setStyle(baseStyle);
                    dlBtn.setOnMouseEntered(e -> dlBtn.setStyle(hoverStyle));
                    dlBtn.setOnMouseExited(e -> dlBtn.setStyle(baseStyle));

                    dlBtn.setOnAction(e -> downloadFile(res.get("resourceUrl"), cleanFileName(new File(res.get("resourceUrl")).getName())));
                    actionButtons.getChildren().add(dlBtn);
                }

                row.getChildren().addAll(icon, info, spacer, actionButtons);

                if ("TEACHER".equals(currentUser.getRole()) && (currentUser.getId().equals(res.get("uploaderId")))) {
                    Button delBtn = new Button("Trash");
                    delBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #E74C3C; -fx-cursor: hand;");
                    delBtn.setOnAction(e -> deleteResource(res.get("id")));
                    row.getChildren().add(delBtn);
                }
                box.getChildren().add(row);
            }
        }

        if ("TEACHER".equals(currentUser.getRole()) && currentUser.getId().equals(ownerId)) {
            HBox uploadArea = new HBox(15);
            uploadArea.setAlignment(Pos.CENTER);
            uploadArea.setStyle("-fx-padding: 15; -fx-border-color: #D5DBDB; -fx-border-style: dashed; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-color: #FAFCFC;");

            Button uploadFileBtn = new Button("☁ Upload File");
            uploadFileBtn.setStyle("-fx-background-color: #27AE60; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 20; -fx-cursor: hand;");
            uploadFileBtn.setOnAction(e -> handleFileUpload());

            Label orLbl = new Label("or");
            orLbl.setStyle("-fx-text-fill: #95A5A6; -fx-font-weight: bold;");

            Button addLinkBtn = new Button("🔗 Add Web Link");
            addLinkBtn.setStyle("-fx-background-color: #2980B9; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 20; -fx-cursor: hand;");
            addLinkBtn.setOnAction(e -> handleLinkUpload());

            uploadArea.getChildren().addAll(uploadFileBtn, orLbl, addLinkBtn);
            box.getChildren().add(uploadArea);
        }

        return box;
    }

    private void handleLinkUpload() {
        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle("Attach Web Link");
        dialog.setHeaderText(null);

        VBox content = new VBox(15);
        content.setStyle("-fx-padding: 20; -fx-background-color: #FFFFFF;");

        Label titleLbl = new Label("🔗 Add a Web Resource");
        titleLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

        TextField urlField = new TextField();
        urlField.setPromptText("https:// (e.g., Drive, YouTube)");
        urlField.setStyle("-fx-background-color: #F8F9F9; -fx-border-color: #D5DBDB; -fx-border-radius: 6; -fx-padding: 12; -fx-font-size: 14px; -fx-pref-width: 350;");

        TextField nameField = new TextField();
        nameField.setPromptText("Display Title (e.g., Chapter 1 Slides)");
        nameField.setStyle("-fx-background-color: #F8F9F9; -fx-border-color: #D5DBDB; -fx-border-radius: 6; -fx-padding: 12; -fx-font-size: 14px; -fx-pref-width: 350;");

        content.getChildren().addAll(titleLbl, new Label("URL Link:"), urlField, new Label("Resource Title:"), nameField);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setStyle("-fx-background-color: #2980B9; -fx-text-fill: white; -fx-font-weight: bold;");

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK && !urlField.getText().isEmpty() && !nameField.getText().isEmpty()) {
                return new String[]{nameField.getText(), urlField.getText()};
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> sendToCloud(result[0], result[1], "LINK"));
    }

    private void handleFileUpload() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Upload Resource Document");
        File file = fileChooser.showOpenDialog(resourceHubContainer.getScene().getWindow());

        if (file != null) {
            Dialog<String> dialog = new Dialog<>();
            dialog.setTitle("Confirm Upload");
            dialog.setHeaderText(null);

            VBox content = new VBox(15);
            content.setStyle("-fx-padding: 20; -fx-background-color: #FFFFFF;");

            Label titleLbl = new Label("☁ Upload File");
            titleLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

            Label fileLbl = new Label("File: " + file.getName());
            fileLbl.setStyle("-fx-text-fill: #7F8C8D; -fx-font-style: italic;");

            TextField nameField = new TextField(file.getName());
            nameField.setPromptText("Display Title");
            nameField.setStyle("-fx-background-color: #F8F9F9; -fx-border-color: #D5DBDB; -fx-border-radius: 6; -fx-padding: 12; -fx-font-size: 14px; -fx-pref-width: 350;");

            content.getChildren().addAll(titleLbl, fileLbl, new Label("Resource Title:"), nameField);

            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
            okButton.setStyle("-fx-background-color: #27AE60; -fx-text-fill: white; -fx-font-weight: bold;");

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == ButtonType.OK && !nameField.getText().isEmpty()) return nameField.getText();
                return null;
            });

            dialog.showAndWait().ifPresent(title -> {
                try {
                    File uploadDir = new File("uploads/resources");
                    if (!uploadDir.exists()) uploadDir.mkdirs();
                    String newName = System.currentTimeMillis() + "_" + file.getName();
                    File dest = new File(uploadDir, newName);
                    Files.copy(file.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);

                    sendToCloud(title, dest.getPath().replace("\\", "\\\\"), "FILE");
                } catch (Exception ex) { ex.printStackTrace(); }
            });
        }
    }

    private void sendToCloud(String title, String url, String type) {
        try {
            String json = String.format("{\"courseCode\":\"%s\", \"uploaderId\":\"%s\", \"uploaderName\":\"%s\", \"title\":\"%s\", \"resourceUrl\":\"%s\", \"resourceType\":\"%s\"}",
                    currentCourse, currentUser.getId(), currentUser.getName(), title.replace("\"", "\\\""), url.replace("\"", "\\\""), type);

            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://localhost:8080/api/resources"))
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(json)).build();
            client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            fetchAndRenderResources();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void openResource(String url, String type) {
        try {
            if ("FILE".equals(type)) java.awt.Desktop.getDesktop().open(new File(url));
            else java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Could not open resource!").show();
        }
    }

    private void deleteResource(String id) {
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://localhost:8080/api/resources/" + id))
                    .DELETE().build();
            client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            fetchAndRenderResources();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private String cleanFileName(String rawName) {
        if (rawName == null) return "";
        return rawName.replaceFirst("^.*?_\\d{13}_", "");
    }

    private void downloadFile(String sourcePath, String cleanName) {
        File sourceFile = new File(sourcePath);
        if (!sourceFile.exists()) {
            new Alert(Alert.AlertType.ERROR, "File not found on server!").show();
            return;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save File");
        fileChooser.setInitialFileName(cleanName);
        File destFile = fileChooser.showSaveDialog(resourceHubContainer.getScene().getWindow());
        if (destFile != null) {
            try {
                Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) { e.printStackTrace(); }
        }
    }
}
package com.syncron.controllers;

import com.syncron.models.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
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
    @FXML private Label resourceCountLabel;

    // Upload progress UI
    @FXML private VBox uploadProgressBox;
    @FXML private Label uploadProgressLabel;
    @FXML private ProgressBar uploadProgressBar;

    private User currentUser;
    private String currentCourse;

    // ========================================================================
    //  INITIALIZATION
    // ========================================================================

    @FXML
    public void initialize() {
        currentUser = SessionManager.getCurrentUser();
        currentCourse = SessionManager.getCurrentCourseCode();

        if (currentCourse == null || currentCourse.isEmpty()) return;
        fetchAndRenderResources();
    }

    // ========================================================================
    //  CORE DATA FETCH & RENDER ENGINE
    // ========================================================================

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

                // Group resources by uploader
                Map<String, List<Map<String, String>>> teacherMap = new LinkedHashMap<>();
                Map<String, String> teacherNames = new LinkedHashMap<>();

                for (Map<String, String> res : allResources) {
                    String tid = res.get("uploaderId");
                    teacherMap.putIfAbsent(tid, new ArrayList<>());
                    teacherMap.get(tid).add(res);
                    teacherNames.put(tid, res.get("uploaderName"));
                }

                // Ensure the current teacher always has a box (even if empty)
                if ("TEACHER".equals(currentUser.getRole())) {
                    teacherMap.putIfAbsent(currentUser.getId(), new ArrayList<>());
                    teacherNames.putIfAbsent(currentUser.getId(), currentUser.getName());
                }

                // Sort: current user's box first, then alphabetical
                List<String> sortedTeacherIds = new ArrayList<>(teacherMap.keySet());
                sortedTeacherIds.sort((id1, id2) -> {
                    if (id1.equals(currentUser.getId())) return -1;
                    if (id2.equals(currentUser.getId())) return 1;
                    return teacherNames.getOrDefault(id1, "").compareToIgnoreCase(teacherNames.getOrDefault(id2, ""));
                });

                // Update resource count
                if (resourceCountLabel != null) {
                    resourceCountLabel.setText(allResources.size() + " resource" + (allResources.size() != 1 ? "s" : "") + " available");
                }

                // Render each teacher's box
                for (String tid : sortedTeacherIds) {
                    boolean isOwner = tid.equals(currentUser.getId());
                    String boxTitle = isOwner ? "Your Resources" : teacherNames.get(tid) + "'s Resources";
                    resourceHubContainer.getChildren().add(
                            buildResourceBox(boxTitle, tid, teacherMap.get(tid), isOwner)
                    );
                }

            } else {
                showEmptyState("Could not load resources. Server returned status " + response.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
            showEmptyState("Could not connect to the server. Make sure Kernel Server is running.");
        }
    }

    private void showEmptyState(String message) {
        VBox emptyBox = new VBox(10);
        emptyBox.setAlignment(Pos.CENTER);
        emptyBox.setPadding(new Insets(40));
        emptyBox.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #EAECEE; -fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 40;");

        Label icon = new Label("📂");
        icon.setStyle("-fx-font-size: 36px;");
        Label msg = new Label(message);
        msg.setStyle("-fx-text-fill: #95A5A6; -fx-font-size: 14px; -fx-font-style: italic;");
        msg.setWrapText(true);

        emptyBox.getChildren().addAll(icon, msg);
        resourceHubContainer.getChildren().add(emptyBox);
    }

    // ========================================================================
    //  RESOURCE BOX BUILDER (Per-Teacher)
    // ========================================================================

    private VBox buildResourceBox(String title, String ownerId, List<Map<String, String>> resources, boolean isOwner) {
        VBox box = new VBox(0);

        // --- Card styling: owner gets a subtle highlight ---
        String cardStyle;
        if (isOwner && "TEACHER".equals(currentUser.getRole())) {
            cardStyle = "-fx-background-color: #FFFFFF; -fx-border-color: #3498DB; -fx-border-width: 1.5; -fx-border-radius: 14; -fx-background-radius: 14; -fx-effect: dropshadow(three-pass-box, rgba(52,152,219,0.12), 20, 0, 0, 6);";
        } else {
            cardStyle = "-fx-background-color: #FFFFFF; -fx-border-color: #E8E8E8; -fx-border-width: 1; -fx-border-radius: 14; -fx-background-radius: 14; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.04), 12, 0, 0, 4);";
        }
        box.setStyle(cardStyle);

        // --- Header ---
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 25, 15, 25));

        // Avatar circle
        Label avatar = new Label(isOwner ? "👤" : "👨‍🏫");
        avatar.setStyle("-fx-font-size: 22px; -fx-background-color: " + (isOwner ? "#EBF5FB" : "#F5EEF8") + "; -fx-background-radius: 50; -fx-min-width: 42; -fx-min-height: 42; -fx-alignment: center;");

        VBox headerText = new VBox(2);
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-family: 'Georgia', serif; -fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

        int fileCount = resources != null ? (int) resources.stream().filter(r -> "FILE".equals(r.get("resourceType"))).count() : 0;
        int linkCount = resources != null ? (int) resources.stream().filter(r -> "LINK".equals(r.get("resourceType"))).count() : 0;
        String subtitle = fileCount + " file" + (fileCount != 1 ? "s" : "") + ", " + linkCount + " link" + (linkCount != 1 ? "s" : "");
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setStyle("-fx-text-fill: #95A5A6; -fx-font-size: 12px;");
        headerText.getChildren().addAll(titleLabel, subtitleLabel);

        header.getChildren().addAll(avatar, headerText);
        box.getChildren().add(header);

        // --- Divider ---
        Region divider = new Region();
        divider.setStyle("-fx-background-color: #F0F0F0; -fx-min-height: 1; -fx-max-height: 1;");
        box.getChildren().add(divider);

        // --- Resource Items ---
        VBox itemsContainer = new VBox(0);
        itemsContainer.setPadding(new Insets(8, 12, 8, 12));

        if (resources == null || resources.isEmpty()) {
            Label empty = new Label("No resources uploaded yet.");
            empty.setStyle("-fx-text-fill: #BDC3C7; -fx-font-size: 13px; -fx-font-style: italic; -fx-padding: 15 15;");
            itemsContainer.getChildren().add(empty);
        } else {
            for (Map<String, String> res : resources) {
                itemsContainer.getChildren().add(buildResourceRow(res, isOwner));
            }
        }
        box.getChildren().add(itemsContainer);

        // --- Upload Area (only for the owner teacher) ---
        if ("TEACHER".equals(currentUser.getRole()) && isOwner) {
            box.getChildren().add(buildUploadArea());
        }

        return box;
    }

    // ========================================================================
    //  INDIVIDUAL RESOURCE ROW
    // ========================================================================

    private HBox buildResourceRow(Map<String, String> res, boolean isOwner) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 15, 12, 15));

        // Hover effect
        String rowBase = "-fx-background-color: transparent; -fx-background-radius: 10;";
        String rowHover = "-fx-background-color: #F8FAFB; -fx-background-radius: 10;";
        row.setStyle(rowBase);
        row.setOnMouseEntered(e -> row.setStyle(rowHover));
        row.setOnMouseExited(e -> row.setStyle(rowBase));

        String type = res.get("resourceType");
        boolean isFile = "FILE".equals(type);

        // --- File type icon ---
        String iconText;
        String iconColor;
        if (isFile) {
            String fileName = res.get("title") != null ? res.get("title").toLowerCase() : "";
            String urlName = res.get("resourceUrl") != null ? res.get("resourceUrl").toLowerCase() : "";
            String combined = fileName + urlName;

            if (combined.endsWith(".pdf")) { iconText = "📕"; iconColor = "#E74C3C"; }
            else if (combined.endsWith(".pptx") || combined.endsWith(".ppt")) { iconText = "📙"; iconColor = "#E67E22"; }
            else if (combined.endsWith(".docx") || combined.endsWith(".doc")) { iconText = "📘"; iconColor = "#2980B9"; }
            else if (combined.endsWith(".xlsx") || combined.endsWith(".csv")) { iconText = "📗"; iconColor = "#27AE60"; }
            else if (combined.endsWith(".zip") || combined.endsWith(".rar")) { iconText = "📦"; iconColor = "#8E44AD"; }
            else if (combined.endsWith(".jpg") || combined.endsWith(".png") || combined.endsWith(".jpeg")) { iconText = "🖼"; iconColor = "#1ABC9C"; }
            else if (combined.endsWith(".java") || combined.endsWith(".cpp") || combined.endsWith(".py") || combined.endsWith(".c")) { iconText = "💻"; iconColor = "#34495E"; }
            else { iconText = "📄"; iconColor = "#3498DB"; }
        } else {
            iconText = "🔗";
            iconColor = "#9B59B6";
        }

        // Icon container
        StackPane iconPane = new StackPane();
        iconPane.setMinSize(42, 42);
        iconPane.setMaxSize(42, 42);
        iconPane.setStyle("-fx-background-color: " + iconColor + "15; -fx-background-radius: 10;");
        Label iconLabel = new Label(iconText);
        iconLabel.setStyle("-fx-font-size: 20px;");
        iconPane.getChildren().add(iconLabel);

        // --- Info ---
        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label resTitle = new Label(res.get("title"));
        resTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");
        resTitle.setMaxWidth(400);
        resTitle.setEllipsisString("...");

        String dateText = res.get("timestamp") != null ? res.get("timestamp") : "Unknown date";
        Label resDate = new Label(dateText);
        resDate.setStyle("-fx-font-size: 11px; -fx-text-fill: #95A5A6;");

        info.getChildren().addAll(resTitle, resDate);

        // --- Action Buttons ---
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button openBtn = createActionButton("Open", "#2980B9", false);
        openBtn.setOnAction(e -> openResource(res.get("resourceUrl"), type));
        actions.getChildren().add(openBtn);

        if (isFile) {
            Button dlBtn = createActionButton("↓ Download", "#1ABC9C", true);
            dlBtn.setOnAction(e -> downloadFile(res.get("resourceUrl"), cleanFileName(res.get("resourceUrl"))));
            actions.getChildren().add(dlBtn);
        }

        // Trash button (only for owner's own resources)
        if ("TEACHER".equals(currentUser.getRole()) && isOwner) {
            Button delBtn = new Button("✕");
            delBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #BDC3C7; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 4 8; -fx-background-radius: 50;");
            delBtn.setOnMouseEntered(e -> delBtn.setStyle("-fx-background-color: #FDEDEC; -fx-text-fill: #E74C3C; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 4 8; -fx-background-radius: 50;"));
            delBtn.setOnMouseExited(e -> delBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #BDC3C7; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 4 8; -fx-background-radius: 50;"));
            delBtn.setOnAction(e -> confirmAndDeleteResource(res.get("id"), res.get("title")));
            actions.getChildren().add(delBtn);
        }

        row.getChildren().addAll(iconPane, info, actions);
        return row;
    }

    private Button createActionButton(String text, String color, boolean outlined) {
        Button btn = new Button(text);
        String baseStyle, hoverStyle;

        if (outlined) {
            baseStyle = "-fx-background-color: transparent; -fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-cursor: hand; -fx-border-color: " + color + "; -fx-border-radius: 20; -fx-padding: 4 14; -fx-font-size: 12px; -fx-background-radius: 20;";
            hoverStyle = "-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-border-color: " + color + "; -fx-border-radius: 20; -fx-padding: 4 14; -fx-font-size: 12px; -fx-background-radius: 20; -fx-effect: dropshadow(three-pass-box, " + color + "60, 8, 0, 0, 2);";
        } else {
            baseStyle = "-fx-background-color: transparent; -fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 12px; -fx-padding: 4 10;";
            hoverStyle = baseStyle + " -fx-underline: true;";
        }

        btn.setStyle(baseStyle);
        btn.setOnMouseEntered(e -> btn.setStyle(hoverStyle));
        btn.setOnMouseExited(e -> btn.setStyle(baseStyle));
        return btn;
    }

    // ========================================================================
    //  UPLOAD AREA (DRAG-AND-DROP STYLE)
    // ========================================================================

    private VBox buildUploadArea() {
        VBox uploadArea = new VBox(12);
        uploadArea.setAlignment(Pos.CENTER);
        uploadArea.setPadding(new Insets(20, 25, 20, 25));

        String areaBase = "-fx-border-color: #D5DBDB; -fx-border-style: dashed; -fx-border-width: 2; -fx-border-radius: 10; -fx-background-color: #FCFDFD; -fx-background-radius: 0 0 14 14;";
        String areaHover = "-fx-border-color: #3498DB; -fx-border-style: dashed; -fx-border-width: 2; -fx-border-radius: 10; -fx-background-color: #EBF5FB; -fx-background-radius: 0 0 14 14;";
        uploadArea.setStyle(areaBase);
        uploadArea.setOnMouseEntered(e -> uploadArea.setStyle(areaHover));
        uploadArea.setOnMouseExited(e -> uploadArea.setStyle(areaBase));

        Label uploadIcon = new Label("☁");
        uploadIcon.setStyle("-fx-font-size: 28px; -fx-text-fill: #BDC3C7;");

        Label uploadHint = new Label("Upload files or add links for your students");
        uploadHint.setStyle("-fx-text-fill: #95A5A6; -fx-font-size: 13px;");

        HBox buttonRow = new HBox(15);
        buttonRow.setAlignment(Pos.CENTER);

        Button uploadFileBtn = new Button("📎 Upload File");
        uploadFileBtn.setStyle("-fx-background-color: #27AE60; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 24; -fx-background-radius: 25; -fx-cursor: hand; -fx-font-size: 13px; -fx-effect: dropshadow(three-pass-box, rgba(39, 174, 96, 0.3), 8, 0, 0, 2);");
        uploadFileBtn.setOnMouseEntered(e -> uploadFileBtn.setStyle("-fx-background-color: #2ECC71; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 24; -fx-background-radius: 25; -fx-cursor: hand; -fx-font-size: 13px; -fx-effect: dropshadow(three-pass-box, rgba(46, 204, 113, 0.5), 12, 0, 0, 3);"));
        uploadFileBtn.setOnMouseExited(e -> uploadFileBtn.setStyle("-fx-background-color: #27AE60; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 24; -fx-background-radius: 25; -fx-cursor: hand; -fx-font-size: 13px; -fx-effect: dropshadow(three-pass-box, rgba(39, 174, 96, 0.3), 8, 0, 0, 2);"));
        uploadFileBtn.setOnAction(e -> handleFileUpload());

        Label orLbl = new Label("or");
        orLbl.setStyle("-fx-text-fill: #BDC3C7; -fx-font-weight: bold;");

        Button addLinkBtn = new Button("🔗 Add Web Link");
        addLinkBtn.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #2980B9; -fx-font-weight: bold; -fx-padding: 10 24; -fx-background-radius: 25; -fx-cursor: hand; -fx-font-size: 13px; -fx-border-color: #2980B9; -fx-border-radius: 25;");
        addLinkBtn.setOnMouseEntered(e -> addLinkBtn.setStyle("-fx-background-color: #2980B9; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 24; -fx-background-radius: 25; -fx-cursor: hand; -fx-font-size: 13px; -fx-border-color: #2980B9; -fx-border-radius: 25; -fx-effect: dropshadow(three-pass-box, rgba(41, 128, 185, 0.4), 10, 0, 0, 2);"));
        addLinkBtn.setOnMouseExited(e -> addLinkBtn.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #2980B9; -fx-font-weight: bold; -fx-padding: 10 24; -fx-background-radius: 25; -fx-cursor: hand; -fx-font-size: 13px; -fx-border-color: #2980B9; -fx-border-radius: 25;"));
        addLinkBtn.setOnAction(e -> handleLinkUpload());

        buttonRow.getChildren().addAll(uploadFileBtn, orLbl, addLinkBtn);
        uploadArea.getChildren().addAll(uploadIcon, uploadHint, buttonRow);
        return uploadArea;
    }

    // ========================================================================
    //  UPLOAD HANDLERS
    // ========================================================================

    private void handleLinkUpload() {
        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle("Add Web Resource");
        dialog.setHeaderText(null);

        VBox content = new VBox(15);
        content.setStyle("-fx-padding: 25; -fx-background-color: #FFFFFF;");

        Label titleLbl = new Label("🔗 Add a Web Resource");
        titleLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

        Label urlLabel = new Label("URL Link");
        urlLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #7F8C8D;");
        TextField urlField = new TextField();
        urlField.setPromptText("https:// (e.g., Google Drive, YouTube, Website)");
        urlField.setStyle("-fx-background-color: #F8F9F9; -fx-border-color: #D5DBDB; -fx-border-radius: 8; -fx-padding: 12; -fx-font-size: 14px; -fx-pref-width: 380;");

        Label nameLabel = new Label("Display Title");
        nameLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #7F8C8D;");
        TextField nameField = new TextField();
        nameField.setPromptText("e.g., Chapter 1 Slides, Video Tutorial");
        nameField.setStyle("-fx-background-color: #F8F9F9; -fx-border-color: #D5DBDB; -fx-border-radius: 8; -fx-padding: 12; -fx-font-size: 14px; -fx-pref-width: 380;");

        content.getChildren().addAll(titleLbl, urlLabel, urlField, nameLabel, nameField);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setText("Add Link");
        okButton.setStyle("-fx-background-color: #2980B9; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 6;");

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK && !urlField.getText().trim().isEmpty() && !nameField.getText().trim().isEmpty()) {
                return new String[]{nameField.getText().trim(), urlField.getText().trim()};
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> sendToCloud(result[0], result[1], "LINK"));
    }

    private void handleFileUpload() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Upload Resource");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All Files", "*.*"),
                new FileChooser.ExtensionFilter("Documents", "*.pdf", "*.docx", "*.doc", "*.pptx", "*.ppt", "*.xlsx", "*.csv", "*.txt"),
                new FileChooser.ExtensionFilter("Code Files", "*.java", "*.cpp", "*.c", "*.py", "*.js", "*.html", "*.css"),
                new FileChooser.ExtensionFilter("Archives", "*.zip", "*.rar", "*.7z"),
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        File file = fileChooser.showOpenDialog(resourceHubContainer.getScene().getWindow());

        if (file != null) {
            Dialog<String> dialog = new Dialog<>();
            dialog.setTitle("Confirm Upload");
            dialog.setHeaderText(null);

            VBox content = new VBox(15);
            content.setStyle("-fx-padding: 25; -fx-background-color: #FFFFFF;");

            Label titleLbl = new Label("📎 Upload File");
            titleLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

            // File info row
            String fileSize = formatFileSize(file.length());
            Label fileLbl = new Label("📄 " + file.getName() + "  (" + fileSize + ")");
            fileLbl.setStyle("-fx-text-fill: #27AE60; -fx-font-weight: bold; -fx-font-size: 13px; -fx-background-color: #EAFAF1; -fx-padding: 8 12; -fx-background-radius: 6;");

            Label nameLabel = new Label("Display Title");
            nameLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #7F8C8D;");

            // Auto-generate a clean display name from the file
            String autoName = file.getName();
            // Remove common timestamp prefixes
            autoName = autoName.replaceFirst("^\\d{10,}_", "");
            // Remove file extension for display
            int dotIndex = autoName.lastIndexOf('.');
            String extension = dotIndex > 0 ? autoName.substring(dotIndex) : "";
            if (dotIndex > 0) autoName = autoName.substring(0, dotIndex);
            // Replace underscores with spaces
            autoName = autoName.replace("_", " ").replace("-", " ");

            TextField nameField = new TextField(autoName);
            nameField.setPromptText("Display Title");
            nameField.setStyle("-fx-background-color: #F8F9F9; -fx-border-color: #D5DBDB; -fx-border-radius: 8; -fx-padding: 12; -fx-font-size: 14px; -fx-pref-width: 380;");

            content.getChildren().addAll(titleLbl, fileLbl, nameLabel, nameField);

            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
            okButton.setText("Upload");
            okButton.setStyle("-fx-background-color: #27AE60; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 6;");

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == ButtonType.OK && !nameField.getText().trim().isEmpty()) return nameField.getText().trim();
                return null;
            });

            dialog.showAndWait().ifPresent(title -> {
                // Show progress
                showUploadProgress("Uploading " + file.getName() + "...");

                new Thread(() -> {
                    try {
                        File uploadDir = new File("uploads/resources");
                        if (!uploadDir.exists()) uploadDir.mkdirs();

                        // Unique filename: courseCode_teacherId_timestamp_originalName
                        String safeCourse = currentCourse.replace(" ", "");
                        String newName = safeCourse + "_" + currentUser.getId() + "_" + System.currentTimeMillis() + "_" + file.getName();
                        File dest = new File(uploadDir, newName);
                        Files.copy(file.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);

                        // Send metadata to cloud
                        String filePath = dest.getPath().replace("\\", "\\\\");
                        Platform.runLater(() -> {
                            sendToCloud(title, filePath, "FILE");
                            hideUploadProgress();
                        });
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        Platform.runLater(() -> {
                            hideUploadProgress();
                            new Alert(Alert.AlertType.ERROR, "Failed to upload file. Please try again.").show();
                        });
                    }
                }).start();
            });
        }
    }

    // ========================================================================
    //  CLOUD API CALLS
    // ========================================================================

    private void sendToCloud(String title, String url, String type) {
        try {
            // Escape special characters in JSON
            String safeTitle = title.replace("\\", "\\\\").replace("\"", "\\\"");
            String safeUrl = url.replace("\\", "\\\\").replace("\"", "\\\"");
            String safeName = currentUser.getName().replace("\"", "\\\"");

            String json = String.format(
                    "{\"courseCode\":\"%s\", \"uploaderId\":\"%s\", \"uploaderName\":\"%s\", \"title\":\"%s\", \"resourceUrl\":\"%s\", \"resourceType\":\"%s\"}",
                    currentCourse, currentUser.getId(), safeName, safeTitle, safeUrl, type
            );

            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://localhost:8080/api/resources"))
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(json)).build();

            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                fetchAndRenderResources(); // Refresh the entire view
            } else {
                new Alert(Alert.AlertType.ERROR, "Server rejected the upload. Status: " + response.statusCode()).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Network error while uploading. Check your connection.").show();
        }
    }

    // ========================================================================
    //  RESOURCE ACTIONS
    // ========================================================================

    private void openResource(String url, String type) {
        try {
            if ("FILE".equals(type)) {
                File file = new File(url);
                if (file.exists()) {
                    java.awt.Desktop.getDesktop().open(file);
                } else {
                    new Alert(Alert.AlertType.ERROR, "File not found at:\n" + url + "\n\nThe file may have been moved or deleted.").show();
                }
            } else {
                // Ensure the URL has a protocol
                String safeUrl = url;
                if (!safeUrl.startsWith("http://") && !safeUrl.startsWith("https://")) {
                    safeUrl = "https://" + safeUrl;
                }
                java.awt.Desktop.getDesktop().browse(new java.net.URI(safeUrl));
            }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Could not open resource.").show();
        }
    }

    private void confirmAndDeleteResource(String id, String title) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete \"" + (title != null ? title : "this resource") + "\"?\nThis action cannot be undone.",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Confirm Deletion");
        confirm.showAndWait();

        if (confirm.getResult() == ButtonType.YES) {
            deleteResource(id);
        }
    }

    private void deleteResource(String id) {
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://localhost:8080/api/resources/" + id))
                    .DELETE().build();

            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                fetchAndRenderResources();
            } else {
                new Alert(Alert.AlertType.ERROR, "Could not delete the resource. Server returned: " + response.statusCode()).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Network error during deletion.").show();
        }
    }

    private void downloadFile(String sourcePath, String cleanName) {
        if (sourcePath == null || sourcePath.isEmpty()) {
            new Alert(Alert.AlertType.ERROR, "No file path available.").show();
            return;
        }

        File sourceFile = new File(sourcePath);
        if (!sourceFile.exists()) {
            new Alert(Alert.AlertType.ERROR, "File not found on this machine.\nPath: " + sourcePath + "\n\nIf you're on a different computer than the uploader, the file may only exist on their machine.").show();
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save File As");
        fileChooser.setInitialFileName(cleanName);
        File destFile = fileChooser.showSaveDialog(resourceHubContainer.getScene().getWindow());

        if (destFile != null) {
            try {
                Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                new Alert(Alert.AlertType.INFORMATION, "File saved successfully!").show();
            } catch (Exception e) {
                e.printStackTrace();
                new Alert(Alert.AlertType.ERROR, "Could not save the file.").show();
            }
        }
    }

    // ========================================================================
    //  UTILITY METHODS
    // ========================================================================

    private String cleanFileName(String rawPath) {
        if (rawPath == null || rawPath.isEmpty()) return "download";
        String name = new File(rawPath).getName();
        // Remove the prefix pattern: courseCode_teacherId_timestamp_
        // e.g., "CSE105_teacher1_1712345678901_Lecture1.pdf" -> "Lecture1.pdf"
        name = name.replaceFirst("^[A-Za-z]+\\d+_[^_]+_\\d{13}_", "");
        // Also handle old format: just timestamp_
        name = name.replaceFirst("^\\d{13}_", "");
        return name.isEmpty() ? "download" : name;
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private void showUploadProgress(String message) {
        if (uploadProgressBox != null) {
            uploadProgressLabel.setText(message);
            uploadProgressBox.setVisible(true);
            uploadProgressBox.setManaged(true);
        }
    }

    private void hideUploadProgress() {
        if (uploadProgressBox != null) {
            uploadProgressBox.setVisible(false);
            uploadProgressBox.setManaged(false);
        }
    }
}
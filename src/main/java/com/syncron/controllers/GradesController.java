package com.syncron.controllers;

import com.syncron.utils.ServerConfig;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.net.URI;
import java.util.List;
import java.util.Map;

public class GradesController {

    @FXML private VBox weightageContainer;
    @FXML private VBox gradingScaleContainer;
    @FXML private VBox performanceTableBody;
    @FXML private Label courseInfoBadge;

    private boolean isTeacher = false;
    private boolean isSessional = false;
    private String courseCode;

    @FXML
    public void initialize() {
        courseCode = SessionManager.getCurrentCourseCode();
        if (courseCode == null) courseCode = "CSE 105";

        String courseType = SessionManager.getCurrentCourseType();
        isSessional = "sessional".equalsIgnoreCase(courseType);
        isTeacher = "TEACHER".equalsIgnoreCase(SessionManager.getCurrentUser().getRole());

        buildEvaluationScheme();
        buildGradingScale();
        fetchAndBuildPerformanceTable();
    }

    private void buildEvaluationScheme() {
        weightageContainer.getChildren().clear();

        if (isSessional) {
            courseInfoBadge.setText("Sessional • 1.5 Credits • 150 Marks • 1.5 hrs/week");
            weightageContainer.getChildren().addAll(
                    createWeightBox("Attendance", "10%", "#27AE60"),
                    createWeightBox("Online / Offline / Project", "70%", "#E74C3C"),
                    createWeightBox("Quiz / Viva", "20%", "#D35400")
            );
        } else {
            courseInfoBadge.setText("Theory • 3.0 Credits • 300 Marks • 3 hrs/week");
            weightageContainer.getChildren().addAll(
                    createWeightBox("Attendance", "10%", "#27AE60"),
                    createWeightBox("Class Tests / Assignments", "20%", "#D35400"),
                    createWeightBox("Term Final", "70%", "#E74C3C")
            );
        }
    }

    private HBox createWeightBox(String title, String weight, String edgeColor) {
        HBox box = new HBox();
        box.setAlignment(Pos.CENTER_LEFT);
        box.setStyle("-fx-background-color: #FDFEFE; -fx-border-color: #ECF0F1; -fx-border-width: 1 1 1 5; -fx-border-left-color: " + edgeColor + "; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 12 20;");

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #34495E;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label weightLbl = new Label(weight);
        weightLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + edgeColor + ";");

        box.getChildren().addAll(titleLbl, spacer, weightLbl);
        return box;
    }

    // =========================================================================
    // THE REAL PERFORMANCE TABLE — Fetches evaluations from Cloud
    // =========================================================================

    private void fetchAndBuildPerformanceTable() {
        performanceTableBody.getChildren().clear();

        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            com.google.gson.Gson gson = new com.google.gson.Gson();
            java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<Map<String, String>>>(){}.getType();

            if (isSessional) {
                // Fetch ONLINE evaluations
                List<Map<String, String>> onlines = fetchEvaluations(client, gson, listType, "ONLINE");
                if (!onlines.isEmpty()) {
                    performanceTableBody.getChildren().add(createSectionHeader("Online Lab Tests"));
                    int idx = 1;
                    for (Map<String, String> eval : onlines) {
                        performanceTableBody.getChildren().add(createEvalRow("Online " + idx + " — " + eval.get("title"), eval, idx % 2 == 0));
                        idx++;
                    }
                }

                // Fetch OFFLINE evaluations
                List<Map<String, String>> offlines = fetchEvaluations(client, gson, listType, "OFFLINE");
                if (!offlines.isEmpty()) {
                    performanceTableBody.getChildren().add(createSectionHeader("Offline Assignments"));
                    int idx = 1;
                    for (Map<String, String> eval : offlines) {
                        performanceTableBody.getChildren().add(createEvalRow("Offline " + idx + " — " + eval.get("title"), eval, idx % 2 == 0));
                        idx++;
                    }
                }
            } else {
                // Theory: Fetch CT evaluations
                List<Map<String, String>> cts = fetchEvaluations(client, gson, listType, "CT");
                if (!cts.isEmpty()) {
                    performanceTableBody.getChildren().add(createSectionHeader("Class Tests"));
                    int idx = 1;
                    for (Map<String, String> eval : cts) {
                        performanceTableBody.getChildren().add(createEvalRow("CT " + idx + " — " + eval.get("title"), eval, idx % 2 == 0));
                        idx++;
                    }
                }

                // Fetch ASSIGNMENT evaluations
                List<Map<String, String>> assignments = fetchEvaluations(client, gson, listType, "ASSIGNMENT");
                if (!assignments.isEmpty()) {
                    performanceTableBody.getChildren().add(createSectionHeader("Assignments"));
                    int idx = 1;
                    for (Map<String, String> eval : assignments) {
                        performanceTableBody.getChildren().add(createEvalRow("Assignment " + idx + " — " + eval.get("title"), eval, idx % 2 == 0));
                        idx++;
                    }
                }
            }

            // Static rows for non-evaluation components
            performanceTableBody.getChildren().add(createSectionHeader("Other Components"));
            performanceTableBody.getChildren().add(createStaticRow("Attendance (10%)", true));
            if (isSessional) {
                performanceTableBody.getChildren().add(createStaticRow("Quiz / Viva (20%)", false));
            } else {
                performanceTableBody.getChildren().add(createStaticRow("Term Final (70%)", false));
            }

            if (performanceTableBody.getChildren().isEmpty()) {
                Label empty = new Label("No evaluations published yet for this course.");
                empty.setStyle("-fx-text-fill: #95A5A6; -fx-font-style: italic; -fx-padding: 15;");
                performanceTableBody.getChildren().add(empty);
            }

        } catch (Exception e) {
            e.printStackTrace();
            Label err = new Label("Could not load evaluation data.");
            err.setStyle("-fx-text-fill: #E74C3C; -fx-padding: 15;");
            performanceTableBody.getChildren().add(err);
        }
    }

    private List<Map<String, String>> fetchEvaluations(java.net.http.HttpClient client, com.google.gson.Gson gson,
                                                       java.lang.reflect.Type listType, String evalType) {
        try {
            java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(ServerConfig.getBaseUrl() + "/api/evaluations/course/" + courseCode.replace(" ", "%20") + "/" + evalType))
                    .GET().build();
            java.net.http.HttpResponse<String> res = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (res.statusCode() == 200) {
                return gson.fromJson(res.body(), listType);
            }
        } catch (Exception ignored) {}
        return new java.util.ArrayList<>();
    }

    private Label createSectionHeader(String text) {
        Label header = new Label(text);
        header.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #7F8C8D; -fx-padding: 15 0 5 5; -fx-border-color: #ECF0F1; -fx-border-width: 0 0 1 0;");
        header.setMaxWidth(Double.MAX_VALUE);
        return header;
    }

    private HBox createEvalRow(String displayTitle, Map<String, String> eval, boolean isAlternate) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 15, 10, 15));
        row.setStyle(isAlternate ? "-fx-background-color: #FAFCFC;" : "");

        // Hover
        String baseRowStyle = row.getStyle();
        row.setOnMouseEntered(e -> row.setStyle(baseRowStyle + " -fx-background-color: #F0F7FF;"));
        row.setOnMouseExited(e -> row.setStyle(baseRowStyle));

        // Title (clickable — navigates to evaluation details)
        Label titleLbl = new Label(displayTitle);
        titleLbl.setStyle("-fx-text-fill: #2C3E50; -fx-font-weight: bold; -fx-font-size: 13px; -fx-cursor: hand;");
        titleLbl.setPrefWidth(350);
        titleLbl.setOnMouseEntered(e -> titleLbl.setStyle("-fx-text-fill: #2980B9; -fx-font-weight: bold; -fx-font-size: 13px; -fx-cursor: hand; -fx-underline: true;"));
        titleLbl.setOnMouseExited(e -> titleLbl.setStyle("-fx-text-fill: #2C3E50; -fx-font-weight: bold; -fx-font-size: 13px; -fx-cursor: hand;"));
        titleLbl.setOnMouseClicked(e -> {
            SessionManager.setCurrentEvaluationId(eval.get("id"));
            String type = eval.get("type");
            String parentTab = "Common";
            if ("CT".equalsIgnoreCase(type) || "ASSIGNMENT".equalsIgnoreCase(type)) parentTab = "CT and Assignments";
            else if ("ONLINE".equalsIgnoreCase(type)) parentTab = "Onlines";
            else if ("OFFLINE".equalsIgnoreCase(type)) parentTab = "Offlines";

            MainController.instance.forceSidebarSelection(parentTab);
            MainController.instance.updateBreadcrumb(parentTab + " / " + eval.get("title"));
            com.syncron.utils.NavigationManager.switchScreen("evaluation_details.fxml");
        });

        // Total marks label
        String totalMarks = eval.get("totalMarks") != null ? eval.get("totalMarks") : "—";
        Label totalLbl = new Label("/ " + totalMarks);
        totalLbl.setStyle("-fx-text-fill: #95A5A6; -fx-font-size: 13px;");
        totalLbl.setPrefWidth(60);

        // Marks input field (teacher only) or display
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        if (isTeacher) {
            TextField marksField = new TextField();
            marksField.setPromptText("—");
            marksField.setPrefWidth(70);
            marksField.setStyle("-fx-background-color: #F9F9F9; -fx-border-color: #E0E0E0; -fx-border-radius: 6; -fx-padding: 5 8; -fx-font-size: 13px; -fx-alignment: center;");

            // Note: In a full implementation, you'd load existing grades and save on change.
            // For now, the field is ready for teacher input.

            row.getChildren().addAll(titleLbl, spacer, marksField, totalLbl);
        } else {
            Label marksLbl = new Label("—");
            marksLbl.setStyle("-fx-text-fill: #D35400; -fx-font-weight: bold; -fx-font-size: 14px;");
            marksLbl.setPrefWidth(50);

            row.getChildren().addAll(titleLbl, spacer, marksLbl, totalLbl);
        }

        return row;
    }

    private HBox createStaticRow(String title, boolean isAlternate) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 15, 10, 15));
        row.setStyle(isAlternate ? "-fx-background-color: #FAFCFC;" : "");

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-text-fill: #2C3E50; -fx-font-weight: bold; -fx-font-size: 13px;");
        titleLbl.setPrefWidth(350);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        if (isTeacher) {
            TextField marksField = new TextField();
            marksField.setPromptText("—");
            marksField.setPrefWidth(70);
            marksField.setStyle("-fx-background-color: #F9F9F9; -fx-border-color: #E0E0E0; -fx-border-radius: 6; -fx-padding: 5 8; -fx-font-size: 13px; -fx-alignment: center;");

            Label totalLbl = new Label("/ —");
            totalLbl.setStyle("-fx-text-fill: #95A5A6; -fx-font-size: 13px;");
            totalLbl.setPrefWidth(60);

            row.getChildren().addAll(titleLbl, spacer, marksField, totalLbl);
        } else {
            Label marksLbl = new Label("—");
            marksLbl.setStyle("-fx-text-fill: #D35400; -fx-font-weight: bold; -fx-font-size: 14px;");
            row.getChildren().addAll(titleLbl, spacer, marksLbl);
        }

        return row;
    }

    private void buildGradingScale() {
        String[][] scale = {
                {"80% and above", "A+", "4.00"}, {"75% to < 80%", "A", "3.75"},
                {"70% to < 75%", "A-", "3.50"}, {"65% to < 70%", "B+", "3.25"},
                {"60% to < 65%", "B", "3.00"}, {"55% to < 60%", "B-", "2.75"},
                {"50% to < 55%", "C+", "2.50"}, {"45% to < 50%", "C", "2.25"},
                {"40% to < 45%", "D", "2.00"}, {"Less than 40%", "F", "0.00"}
        };

        for (String[] tier : scale) {
            HBox row = new HBox();
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-padding: 4 0; -fx-border-color: rgba(52, 152, 219, 0.2); -fx-border-width: 0 0 1 0;");

            Label marks = new Label(tier[0]); marks.setStyle("-fx-text-fill: #34495E; -fx-font-size: 13px;"); marks.setPrefWidth(110);
            Label grade = new Label("➜  " + tier[1]); grade.setStyle("-fx-text-fill: #2980B9; -fx-font-weight: bold; -fx-font-size: 13px;"); grade.setPrefWidth(60);
            Label gpa = new Label(tier[2]); gpa.setStyle("-fx-text-fill: #E67E22; -fx-font-weight: bold; -fx-font-size: 13px;");

            row.getChildren().addAll(marks, grade, gpa);
            gradingScaleContainer.getChildren().add(row);
        }
    }

    @FXML
    private void openBIIS() {
        try { Desktop.getDesktop().browse(new URI("https://biis.buet.ac.bd/")); }
        catch (Exception e) { System.out.println("Could not open BIIS."); }
    }
}
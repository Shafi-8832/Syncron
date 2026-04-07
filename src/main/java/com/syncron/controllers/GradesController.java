package com.syncron.controllers;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.net.URI;

public class GradesController {

    @FXML private VBox weightageContainer;
    @FXML private VBox gradingScaleContainer;
    @FXML private VBox performanceTableBody;
    @FXML private Label courseInfoBadge;

    @FXML
    public void initialize() {
        String courseCode = SessionManager.getCurrentCourseCode();
        if (courseCode == null) courseCode = "CSE 105"; // Fallback for testing

        boolean isSessional = courseCode.toLowerCase().contains("sessional") || courseCode.matches(".*[02468]$");

        buildEvaluationScheme(isSessional);
        buildPerformanceTable(isSessional);
        buildGradingScale();
    }

    private void buildEvaluationScheme(boolean isSessional) {
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

    // Creates the beautiful boxes with the colored left edge flair
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

    private void buildPerformanceTable(boolean isSessional) {
        performanceTableBody.getChildren().clear();

        // Mock Data based on user prompt
        if (isSessional) {
            performanceTableBody.getChildren().add(createTableRow("Online 1", "25/30", "30%", "25", true));
            performanceTableBody.getChildren().add(createTableRow("Online 2", "28/30", "30%", "28", false));
            performanceTableBody.getChildren().add(createTableRow("Quiz", "18/20", "20%", "18", true));
            performanceTableBody.getChildren().add(createTableRow("Attendance", "10/10", "10%", "10", false));
        } else {
            performanceTableBody.getChildren().add(createTableRow("CT", "18/20", "20%", "18", true));
            performanceTableBody.getChildren().add(createTableRow("Lab", "25/30", "30%", "25", false));
            performanceTableBody.getChildren().add(createTableRow("Final", "70/100", "50%", "35", true));
        }
    }

    private HBox createTableRow(String comp, String marks, String weight, String contrib, boolean isAlternate) {
        HBox row = new HBox();
        row.setStyle("-fx-padding: 12; -fx-border-color: #ECF0F1; -fx-border-width: 0 0 1 0;" + (isAlternate ? "" : " -fx-background-color: #FAFCFC;"));

        Label cLbl = new Label(comp); cLbl.setPrefWidth(150); cLbl.setStyle("-fx-text-fill: #2C3E50; -fx-font-weight: bold;");
        Label mLbl = new Label(marks); mLbl.setPrefWidth(100); mLbl.setStyle("-fx-text-fill: #7F8C8D;");
        Label wLbl = new Label(weight); wLbl.setPrefWidth(100); wLbl.setStyle("-fx-text-fill: #7F8C8D;");
        Label ctLbl= new Label(contrib); ctLbl.setPrefWidth(100); ctLbl.setStyle("-fx-text-fill: #D35400; -fx-font-weight: bold;");

        row.getChildren().addAll(cLbl, mLbl, wLbl, ctLbl);
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
        try {
            // 👉 Jumps straight to the browser!
            Desktop.getDesktop().browse(new URI("https://biis.buet.ac.bd/"));
        } catch (Exception e) {
            System.out.println("Could not open browser. BIIS Link: https://biis.buet.ac.bd/");
        }
    }
}
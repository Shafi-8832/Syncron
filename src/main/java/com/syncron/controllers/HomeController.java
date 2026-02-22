package com.syncron.controllers;

import com.syncron.models.Course;
import com.syncron.models.Module;
import com.syncron.utils.DatabaseHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class HomeController {

    @FXML private ProgressBar semesterProgressBar;
    @FXML private VBox courseCardContainer;
    @FXML private VBox urgentContainer;

    @FXML private Button detailsBtn;

    @FXML
    public void initialize() throws SQLException {
        // 1. Set the green progress bar
        semesterProgressBar.setStyle("-fx-accent: #2ECC71;");

        // 2. Load the real courses
        loadCourseCards();
        // 3. Load the urgent deadlines
        loadUrgentDeadlines();

        // 2. Add the clicking Event
        if (detailsBtn != null) {
            detailsBtn.setOnAction(e -> openSemesterDetails());
        }
    }

    private void loadCourseCards() {
        // Fetch list from Database
        var courseList = DatabaseHandler.getAllCourses();

        for (Course course : courseList) {
            // --- BUILD THE CARD UI (Replicating your FXML design) ---
            HBox card = new HBox();
            card.setSpacing(10);
            card.setPadding(new Insets(15));
            // The exact shadow and style from your FXML
            card.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                    "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 2); " +
                    "-fx-cursor: hand;");

            // Left Side: Course Info
            VBox infoBox = new VBox();
            Label codeLabel = new Label(course.getCourseCode());
            codeLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #2C3E50;");

            Label titleLabel = new Label(course.getCourseTitle());
            titleLabel.setStyle("-fx-text-fill: #7F8C8D;");

            infoBox.getChildren().addAll(codeLabel, titleLabel);

            // Spacer (Pushes arrow to the right)
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            // Right Side: The Arrow
            Label arrowLabel = new Label("➜");
            arrowLabel.setStyle("-fx-text-fill: #BDC3C7; -fx-font-size: 18px;");

            // Combine everything into the card
            card.getChildren().addAll(infoBox, spacer, arrowLabel);

            // --- THE CLICK ACTION (Connects to Yesterday's Work) ---
            card.setOnMouseClicked(event -> openCoursePortal(course.getCourseCode(), course.getCourseTitle()));

            // Add card to the screen
            courseCardContainer.getChildren().add(card);
        }
    }

    private void openCoursePortal(String courseCode, String courseTitle) {
        try {
            // 1. Load the persistent MainLayout
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/syncron/views/main_layout.fxml"));
            Parent root = loader.load();

            // 2. Pass the course context to MainController
            MainController controller = loader.getController();
            // Default to "theory" — can be extended later to read type from DB
            controller.setCourseContext(courseCode, courseTitle, "theory");

            // 3. Switch Scenes
            Stage stage = (Stage) courseCardContainer.getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 3. The Navigation Down here
    private void openSemesterDetails() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/syncron/views/semester_details.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) courseCardContainer.getScene().getWindow();
            stage.getScene().setRoot(root);
        }
        catch (IOException e) {
         e.printStackTrace();
        }
    }

    // 4. Load urgent deadlines with this method
    private void loadUrgentDeadlines() throws SQLException {
        // 1. Ask the DB for the next 7 days of task.
        List<com.syncron.models.Module> urgentTasks = DatabaseHandler.getUpcomingDeadlines();

        // 2. If there's no homework
        if (urgentTasks.isEmpty()) {
            Label emptyMsg = new Label("No dues left. Relax! ☕");
            emptyMsg.setStyle("-fx-text-fill: #7F8C8D; -fx-font-style: italic;");
            urgentContainer.getChildren().add(emptyMsg);
            return;
        }

        // 3. Not empty : Loop through the tasks and build the UI
        for (Module task : urgentTasks) {
            VBox taskBox = new VBox(2); // 2px spacing

            // Task Title
            Label titleLabel = new Label("• " + task.getTitle());
            titleLabel.setStyle("-fx-text-fill: #2C3E50; -fx-font-weight: bold; -fx-font-size: 12px;");
            titleLabel.setWrapText(true);

            // Task Date
            Label dateLabel = new Label("Due: " + task.getDueDate());
            dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #E74C3C; -fx-padding: 0 0 0 10;");

            taskBox.getChildren().addAll(titleLabel, dateLabel);

            urgentContainer.getChildren().add(taskBox);
        }
    }
}
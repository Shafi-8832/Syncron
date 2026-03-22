package com.syncron.controllers;

import com.syncron.models.Course;
import com.syncron.models.Module;
import com.syncron.models.User;
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

    @FXML private Label topHeaderNameLabel;
    @FXML private Label welcomeLabel;

    @FXML private HBox profileBtn;
    @FXML private ProgressBar semesterProgressBar;
    @FXML private VBox courseCardContainer;
    @FXML private VBox urgentContainer;

    @FXML private Button detailsBtn;

    @FXML private Label semesterTitleLabel;
    @FXML private Label semesterStatusLabel;
    @FXML private Label semesterProgressText;
    @FXML private Label termFinalLabel;
    @FXML private Label daysRemainingLabel;

    @FXML
    public void initialize() throws SQLException {
        // 1. Set the green progress bar
        semesterProgressBar.setStyle("-fx-accent: #2ECC71;");

        // 2. Load the real courses
        loadCourseCards();
        // 3. Load the urgent deadlines
        loadUrgentDeadlines();
        // 4. load current semester's data
        loadSemesterData();
        
        if (profileBtn != null) {
            profileBtn.setOnMouseClicked(e -> openProfile());
        }

        // 2. Add the clicking Event
        if (detailsBtn != null) {
            detailsBtn.setOnAction(e -> openSemesterDetails());
        }

        User currentUser = SessionManager.getCurrentUser();
        if (currentUser != null && topHeaderNameLabel != null && welcomeLabel != null) {
            topHeaderNameLabel.setText(currentUser.getName());

            String FirstName = currentUser.getName().split(" ")[0];
            welcomeLabel.setText("Welcome Back, " + FirstName + "!");
        }
    }

    private void loadSemesterData() {
        // will be updated later
        semesterTitleLabel.setText("Semester : Level 1 Term 2");
        semesterStatusLabel.setText("PRESENT");
        semesterProgressBar.setProgress(0.35); // 35% Progress
        semesterProgressText.setText("35% Completed");
        termFinalLabel.setText("📅 Term Final: 15 August 2026");
        daysRemainingLabel.setText("⏳ Days Remaining: 145");
    }

    private void loadCourseCards() {
        var courseList = DatabaseHandler.getAllCourses();

        for (Course course : courseList) {
            HBox card = new HBox();
            card.setSpacing(10);
            card.setPadding(new Insets(18));
            // Apply the warm card style!
            card.setStyle("-fx-background-color: #FFFCF8; -fx-background-radius: 10; " +
                    "-fx-border-color: #E0D5C7; -fx-border-radius: 10; " +
                    "-fx-effect: dropshadow(three-pass-box, rgba(74,44,26,0.06), 6, 0, 0, 3); " +
                    "-fx-cursor: hand;");

            VBox infoBox = new VBox();
            Label codeLabel = new Label(course.getCourseCode());
            // Serif bold font for course code
            codeLabel.setStyle("-fx-font-family: 'Georgia', serif; -fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #4A2C1A;");

            Label titleLabel = new Label(course.getCourseTitle());
            // Modern sans-serif for course title
            titleLabel.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 13px; -fx-text-fill: #8C7A6B;");

            infoBox.getChildren().addAll(codeLabel, titleLabel);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label arrowLabel = new Label("➜");
            arrowLabel.setStyle("-fx-text-fill: #C4A882; -fx-font-size: 18px;"); // Warm gold arrow

            card.getChildren().addAll(infoBox, spacer, arrowLabel);

            // Hover effect
            card.setOnMouseEntered(e -> card.setStyle(card.getStyle() + "-fx-background-color: #F5EDE3;"));
            card.setOnMouseExited(e -> card.setStyle(card.getStyle() + "-fx-background-color: #FFFCF8;"));

            card.setOnMouseClicked(event -> openCoursePortal(course.getCourseCode(), course.getCourseTitle()));

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


    private void openProfile() {
        try {
            // Tell the ProfileController to load the logged-in user
            ProfileController.viewingUser = null;

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/syncron/views/profile.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) profileBtn.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
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
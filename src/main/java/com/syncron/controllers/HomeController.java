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
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class HomeController {

    @FXML private Label topHeaderNameLabel;
    @FXML private Label welcomeLabel;

    @FXML private HBox profileBtn;

    @FXML private VBox courseCardContainer;
    @FXML private VBox urgentContainer;

    @FXML private Button detailsBtn;

    @FXML private Label semesterTitleLabel;
    @FXML private Label semesterStatusLabel;

    @FXML private Label termFinalLabel;
    @FXML private Label daysRemainingLabel;

    @FXML private TextField searchField;
    private List<Course> allCourses = new ArrayList<>();

    @FXML
    public void initialize() throws SQLException {
        // 1. Load the real courses once from the database into memory
        allCourses = DatabaseHandler.getAllCourses();
        renderCourses(allCourses);

        // 2. 👉 THE SEARCH ENGINE LISTENER
        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                filterCourses(newValue); // Instantly triggers when you type!
            });
        }

        // 3. Load the urgent deadlines
        loadUrgentDeadlines();

        // 4. Load current semester's data
        loadSemesterData();

        if (profileBtn != null) profileBtn.setOnMouseClicked(e -> openProfile());
        if (detailsBtn != null) detailsBtn.setOnAction(e -> openSemesterDetails());

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
        termFinalLabel.setText("📅 Term Final: 15 August 2026");
        daysRemainingLabel.setText("⏳ Days Remaining: 145");
    }

    private void filterCourses(String query) {
        // If the search bar is empty, show everything!
        if (query == null || query.trim().isEmpty()) {
            renderCourses(allCourses);
            return;
        }

        String lowerQuery = query.toLowerCase();
        java.util.List<Course> filteredList = new java.util.ArrayList<>();

        // Search through course codes AND course titles
        for (Course course : allCourses) {
            if (course.getCourseCode().toLowerCase().contains(lowerQuery) ||
                    course.getCourseTitle().toLowerCase().contains(lowerQuery)) {
                filteredList.add(course);
            }
        }
        renderCourses(filteredList);
    }

    private void renderCourses(java.util.List<Course> coursesToRender) {
        courseCardContainer.getChildren().clear();

        if (coursesToRender.isEmpty()) {
            Label noMatch = new Label("No courses found matching your search.");
            noMatch.setStyle("-fx-text-fill: #7F8C8D; -fx-font-style: italic; -fx-padding: 10 0;");
            courseCardContainer.getChildren().add(noMatch);
            return;
        }

        for (Course course : coursesToRender) {
            HBox card = new HBox();
            card.setSpacing(10);
            card.setPadding(new Insets(18));
            // Crisp white card to pop against the light-blue background
            card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 10; " +
                    "-fx-border-color: #E0D5C7; -fx-border-radius: 10; " +
                    "-fx-effect: dropshadow(three-pass-box, rgba(74,44,26,0.04), 4, 0, 0, 2); " +
                    "-fx-cursor: hand;");

            VBox infoBox = new VBox();
            Label codeLabel = new Label(course.getCourseCode());
            codeLabel.setStyle("-fx-font-family: 'Georgia', serif; -fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #4A2C1A;");

            Label titleLabel = new Label(course.getCourseTitle());
            titleLabel.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 13px; -fx-text-fill: #8C7A6B;");

            infoBox.getChildren().addAll(codeLabel, titleLabel);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label arrowLabel = new Label("➜");
            arrowLabel.setStyle("-fx-text-fill: #3498DB; -fx-font-size: 18px;"); // Beautiful blue arrow to match the theme

            card.getChildren().addAll(infoBox, spacer, arrowLabel);

            card.setOnMouseEntered(e -> card.setStyle(card.getStyle() + "-fx-background-color: #F9FAFC;"));
            card.setOnMouseExited(e -> card.setStyle(card.getStyle().replace("-fx-background-color: #F9FAFC;", "-fx-background-color: #FFFFFF;")));

            card.setOnMouseClicked(event -> openCoursePortal(course));

            courseCardContainer.getChildren().add(card);
        }
    }

    // pass the full Course object to grab its real Type and Credits
    private void openCoursePortal(Course course) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/syncron/views/main_layout.fxml"));
            Parent root = loader.load();

            // Save the context so the inner tabs (Common, Participants) know where we are!
            SessionManager.setCurrentCourseCode(course.getCourseCode());

            MainController controller = loader.getController();
            // Pass the REAL type and credits from the database!
            controller.setCourseContext(course.getCourseCode(), course.getCourseTitle(), course.getType(), course.getCredits());

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
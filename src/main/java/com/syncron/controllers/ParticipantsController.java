package com.syncron.controllers;

import com.syncron.models.Student;
import com.syncron.models.User;
import com.syncron.utils.DatabaseHandler;
import com.syncron.utils.NavigationManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;

import java.util.ArrayList;
import java.util.List;

public class ParticipantsController {

    @FXML private TextField searchBox;
    @FXML private ComboBox<String> roleFilter;
    @FXML private FlowPane participantsGrid;
    @FXML private VBox participantsContainer;

    private List<User> allParticipants;

    @FXML
    public void initialize() {
        roleFilter.setItems(FXCollections.observableArrayList("All", "Teachers", "Students"));
        roleFilter.setValue("All");

        String currentCourseCode = SessionManager.getCurrentCourseCode();
        allParticipants = DatabaseHandler.getCourseParticipants(currentCourseCode);

        renderParticipantsList(allParticipants);

        roleFilter.setOnAction(event -> applyFilters());
        searchBox.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
    }

    private void renderParticipantsList(List<User> allParticipants) {
        participantsContainer.getChildren().clear();

        for (User u : allParticipants) {
            HBox listItem = new HBox(15);
            listItem.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            listItem.setPadding(new Insets(12, 20, 12, 20));

            // Zebra striping effect on hover
            String defaultStyle = "-fx-background-color: transparent; -fx-border-color: #ECF0F1; -fx-border-width: 0 0 1 0; -fx-cursor: hand;";
            String hoverStyle = "-fx-background-color: #F4F7FB; -fx-border-color: #ECF0F1; -fx-border-width: 0 0 1 0; -fx-cursor: hand;";
            listItem.setStyle(defaultStyle);
            listItem.setOnMouseEntered(e -> listItem.setStyle(hoverStyle));
            listItem.setOnMouseExited(e -> listItem.setStyle(defaultStyle));

            // Avatar
            Circle avatar = new Circle(16, javafx.scene.paint.Color.web(u.getRole().equals("TEACHER") ? "#D35400" : "#3498DB"));

            // Name & Details
            VBox details = new VBox(2);
            Label name = new Label(u.getName());
            name.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");
            Label id = new Label(u.getId());
            id.setStyle("-fx-font-size: 11px; -fx-text-fill: #7F8C8D;");
            details.getChildren().addAll(name, id);

            // Push badge to the right
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            // Role Badge
            Label badge = new Label(u.getRole().equals("TEACHER") ? "Lecturer" : "Student");
            badge.setStyle("-fx-background-color: " + (u.getRole().equals("TEACHER") ? "#FDEBD0" : "#EBF5FB") +
                    "; -fx-text-fill: " + (u.getRole().equals("TEACHER") ? "#D35400" : "#2980B9") +
                    "; -fx-padding: 4 10; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;");

            listItem.getChildren().addAll(avatar, details, spacer, badge);

            // Add click functionality so you can still view profiles!
            listItem.setOnMouseClicked(event -> {
                ProfileController.viewingUser = u;
                NavigationManager.switchScreen("profile.fxml");


                // Future functionality here
            });

            participantsContainer.getChildren().add(listItem);
        }
    }


    private void applyFilters() {
        String selectedRole = roleFilter.getValue();
        String query = searchBox.getText() == null ? "" : searchBox.getText().trim().toLowerCase();

        List<User> filtered = new ArrayList<>();
        for (User user : allParticipants) {
            boolean roleMatches = "All".equals(selectedRole)
                    || ("Teachers".equals(selectedRole) && "TEACHER".equalsIgnoreCase(user.getRole()))
                    || ("Students".equals(selectedRole) && "STUDENT".equalsIgnoreCase(user.getRole()));

            boolean searchMatches = query.isEmpty()
                    || (user.getName() != null && user.getName().toLowerCase().contains(query));


            if (roleMatches && searchMatches) {
                filtered.add(user);
            }
        }

        renderParticipantsList(filtered);
    }

}

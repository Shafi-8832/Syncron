package com.syncron.controllers;

import com.syncron.models.Student;
import com.syncron.models.User;
import com.syncron.utils.DatabaseHandler;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class ParticipantsController {

    @FXML private TextField searchBox;
    @FXML private ComboBox<String> roleFilter;
    @FXML private FlowPane participantsGrid;

    private List<User> allParticipants;

    @FXML
    public void initialize() {
        roleFilter.setItems(FXCollections.observableArrayList("All", "Teachers", "Students"));
        roleFilter.setValue("All");

        allParticipants = DatabaseHandler.getCourseParticipants("CSE 105");
        populateGrid(allParticipants);

        roleFilter.setOnAction(event -> applyFilters());
        searchBox.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
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

        populateGrid(filtered);
    }

    private void populateGrid(List<User> users) {
        participantsGrid.getChildren().clear();

        for (User user : users) {
            VBox card = new VBox(5);
            card.setStyle(
                    "-fx-background-color: white; " +
                    "-fx-background-radius: 12; " +
                    "-fx-padding: 15; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 10, 0.2, 0, 2);"
            );
            card.setPrefWidth(260);

            Label nameLabel = new Label(user.getName());
            nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

            Label roleLabel = new Label(user.getRole());
            roleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7F8C8D;");

            card.getChildren().addAll(nameLabel, roleLabel);

            if (user instanceof Student student) {
                HBox flairBadge = new HBox();
                flairBadge.setStyle("-fx-background-color: #3498DB; -fx-background-radius: 999; -fx-padding: 4 10;");

                Label sectionLabel = new Label("Sec: " + student.getSection());
                sectionLabel.setStyle("-fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold;");

                flairBadge.getChildren().add(sectionLabel);
                card.getChildren().add(flairBadge);
            }

            participantsGrid.getChildren().add(card);
        }
    }
}

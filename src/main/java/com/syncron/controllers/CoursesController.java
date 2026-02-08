package com.syncron.controllers;

import com.syncron.models.Course;
import com.syncron.utils.DatabaseHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.List;

public class CoursesController {

    @FXML
    private VBox coursesVBox;

    public void initialize() {
        List<Course> courses = DatabaseHandler.getAllCourses();
        for (Course course : courses) {
            Label label = new Label(course.getCourseCode() + " - " + course.getCourseTitle());
            label.setStyle("-fx-font-size: 16px; -fx-padding: 10;");
            coursesVBox.getChildren().add(label);
        }
    }
}

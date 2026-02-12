package com.syncron.controllers;

import com.syncron.models.Course;
import com.syncron.utils.DatabaseHandler;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;

public class CoursesController {

    // This links to the <TableView fx:id="coursesTable"> in the FXML file
    @FXML
    private TableView<Course> coursesTable;

    // This method runs automatically when the screen loads
    public void initialize() {
        // 1. Get the data from the Database
        var courses = DatabaseHandler.getAllCourses();

        // 2. Put the data inside the table
        coursesTable.setItems(courses);
    }
}
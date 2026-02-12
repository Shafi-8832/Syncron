package com.syncron.controllers;

import com.syncron.models.Course;
import com.syncron.utils.DatabaseHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

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


        // NEW
        coursesTable.setRowFactory(tv -> {
            javafx.scene.control.TableRow<Course> row = new javafx.scene.control.TableRow<>();

            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    Course rowData = row.getItem();
                    openCourseDetails(rowData.getCourseCode()); // Call the helper method
                }
            });
            return row;
        });

    }

    // 3. Helper Method to switch Screens
    private void openCourseDetails(String courseCode) {
        try {
            // Load the new FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/syncron/views/course_details_general.fxml"));
            Parent root = loader.load();

            // Pass the course code to the new controller object
            CourseDetailsController controller = loader.getController();
            controller.setCourseData(courseCode);

            // Option B : Opening a new window (very bad idea)
//            Stage stage = new Stage();
//            stage.setTitle("Course Details");
//            stage.setScene(new Scene(root, 600, 500));
//            stage.show();

            // 3. Get the main Layout (The Dashboard)
            // We grab the "Scene" from the table, then grab the "Root" (Which is the Dashboard BorderPane)
            BorderPane mainLayout = (BorderPane) coursesTable.getScene().getRoot();


            // 4. Swap the screen!!
            mainLayout.setCenter(root);

        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}
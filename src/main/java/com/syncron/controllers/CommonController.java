package com.syncron.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class CommonController {
    private static final String TEACHER_ROLE = "TEACHER";
    private static final String[] DUMMY_TEACHER_NAMES = {"Khaled Mahmud Shahriar", "Dr. Ahmad"};

    @FXML
    private HBox breadcrumbBox;

    @FXML
    private VBox teacherResourcesContainer;

    @FXML
    public void initialize() {
        loadBreadcrumbs();
        loadTeacherResources();
    }

    private void loadBreadcrumbs() {
        breadcrumbBox.getChildren().clear();
        String[] breadcrumbs = {"Dashboard", "My Courses", "CSE 105", "Common"};

        for (int i = 0; i < breadcrumbs.length; i++) {
            final String breadcrumbName = breadcrumbs[i];
            Hyperlink link = new Hyperlink(breadcrumbName);
            link.setOnAction(event -> System.out.println("Breadcrumb clicked: " + breadcrumbName));
            breadcrumbBox.getChildren().add(link);

            if (i < breadcrumbs.length - 1) {
                breadcrumbBox.getChildren().add(new Label("/"));
            }
        }
    }

    private void loadTeacherResources() {
        teacherResourcesContainer.getChildren().clear();

        for (String teacherName : DUMMY_TEACHER_NAMES) {
            VBox teacherBox = new VBox(10);

            Hyperlink teacherHeader = new Hyperlink("Resources from " + teacherName);
            teacherHeader.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
            Label uploadsLabel = new Label("Uploads: Links/Files");

            teacherBox.getChildren().addAll(teacherHeader, uploadsLabel);

            if (TEACHER_ROLE.equals(SessionManager.getCurrentUserRole())) {
                Button addResourcesButton = new Button("Add Resources");
                addResourcesButton.setOnAction(event -> {
                    if (!addResourcesButton.isDisable()) {
                        HBox uploadOptions = new HBox(10);
                        Button uploadFromComputerButton = new Button("Upload from Computer");
                        Button uploadFromDriveButton = new Button("Upload from Drive");
                        uploadOptions.getChildren().addAll(uploadFromComputerButton, uploadFromDriveButton);
                        teacherBox.getChildren().add(uploadOptions);
                        addResourcesButton.setDisable(true);
                    }
                });
                teacherBox.getChildren().add(addResourcesButton);
            }

            teacherResourcesContainer.getChildren().add(teacherBox);
        }
    }
}

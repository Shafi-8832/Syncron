package com.syncron;

import com.syncron.utils.DatabaseHandler;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // 1. Initialize Database
        DatabaseHandler.initializeDB();
        DatabaseHandler.addSampleData();

        // 2. Load the NEW Dashboard
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("views/dashboard.fxml"));

        // 3. Create Scene
        Scene scene = new Scene(fxmlLoader.load(), 900, 600);

        // 4. Load CSS (Wrap in try-catch or check null to avoid crashes if file is missing)
        if (getClass().getResource("style.css") != null) {
            scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        }

        stage.setTitle("Syncron");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
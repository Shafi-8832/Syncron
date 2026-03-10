package com.syncron.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

import java.io.IOException;

/**
 * Static utility class for managing SPA-style screen navigation.
 * Loads FXML views into a shared content area (StackPane), replacing
 * the previous content each time.
 */
public class NavigationManager {

    private static StackPane contentArea;

    /**
     * Sets the reference to the main content area used for navigation.
     *
     * @param contentArea the StackPane that will hold the active view
     */
    public static void initialize(StackPane contentArea) {
        NavigationManager.contentArea = contentArea;
    }

    /**
     * Loads the specified FXML file from {@code /com/syncron/views/} and
     * displays it in the content area, clearing any previous content.
     *
     * @param fxmlFileName the FXML file name (e.g. "home.fxml")
     */
    public static void switchScreen(String fxmlFileName) {
        if (contentArea == null) {
            throw new IllegalStateException("NavigationManager is not initialized. Call initialize() first.");
        }
        try {
            FXMLLoader loader = new FXMLLoader(
                    NavigationManager.class.getResource("/com/syncron/views/" + fxmlFileName));
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

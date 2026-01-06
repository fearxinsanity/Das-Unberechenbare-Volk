package de.schulprojekt.duv.view;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Entry point of the JavaFX application.
 * Loads the initial StartView and applies global CSS styles.
 */
public class Main extends Application {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    // --- Constants: Configuration ---
    private static final String APP_TITLE = "Das Unberechenbare Volk";
    private static final int WINDOW_WIDTH = 1200;
    private static final int WINDOW_HEIGHT = 750;

    // --- Constants: Resources ---
    private static final String FXML_START_VIEW = "/de/schulprojekt/duv/view/StartView.fxml";
    private static final String CSS_COMMON = "/de/schulprojekt/duv/common.css";
    private static final String CSS_START = "/de/schulprojekt/duv/start.css";

    // --- Main Entry Point ---
    static void main(String[] args) {
        launch(args);
    }

    // --- Application Lifecycle ---

    @Override
    public void start(Stage primaryStage) {
        try {
            // 1. Load FXML
            URL fxmlUrl = getClass().getResource(FXML_START_VIEW);
            if (fxmlUrl == null) {
                throw new IOException("FXML resource not found: " + FXML_START_VIEW);
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            // 2. Create Scene
            Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);

            // 3. Apply CSS
            loadStylesheet(scene, CSS_COMMON);
            loadStylesheet(scene, CSS_START);

            // 4. Configure Stage
            primaryStage.setTitle(APP_TITLE);
            primaryStage.setScene(scene);
            primaryStage.setResizable(true);

            // Ensure full shutdown on close (kills background simulation threads)
            // FIX: Renamed unused parameter 'e' to 'ignored'
            primaryStage.setOnCloseRequest(ignored -> {
                Platform.exit();
                System.exit(0);
            });

            primaryStage.show();

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to start application.", e);
        }
    }

    // --- Helper Methods ---

    /**
     * Safely loads a CSS file and adds it to the scene.
     * Logs a warning if the file is missing but does not crash the app.
     */
    private void loadStylesheet(Scene scene, String resourcePath) {
        URL url = getClass().getResource(resourcePath);
        if (url != null) {
            scene.getStylesheets().add(url.toExternalForm());
        } else {
            LOGGER.log(Level.WARNING, "CSS resource not found: {0}", resourcePath);
        }
    }
}
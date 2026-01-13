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
 */
public class Main extends Application {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    // --- Constants ---
    private static final String APP_TITLE = "Das Unberechenbare Volk";
    private static final int WINDOW_WIDTH = 1200;
    private static final int WINDOW_HEIGHT = 750;

    // Use consistent paths
    private static final String FXML_START_VIEW = "/de/schulprojekt/duv/view/StartView.fxml";
    private static final String CSS_COMMON = "/de/schulprojekt/duv/common.css";
    private static final String CSS_START = "/de/schulprojekt/duv/start.css";

    static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            URL fxmlUrl = getClass().getResource(FXML_START_VIEW);
            if (fxmlUrl == null) {
                throw new IOException("FXML resource not found: " + FXML_START_VIEW);
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            // FIX: Grab controller for clean shutdown
            Object controller = loader.getController();

            Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);

            loadStylesheet(scene, CSS_COMMON);
            loadStylesheet(scene, CSS_START);

            primaryStage.setTitle(APP_TITLE);
            primaryStage.setScene(scene);
            primaryStage.setResizable(true);

            // FIX: Proper shutdown sequence
            primaryStage.setOnCloseRequest(event -> {
                if (controller instanceof DashboardController) {
                    ((DashboardController) controller).shutdown();
                }
                Platform.exit();
                System.exit(0);
            });

            primaryStage.show();

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to start application.", e);
        }
    }

    private void loadStylesheet(Scene scene, String resourcePath) {
        URL url = getClass().getResource(resourcePath);
        if (url != null) {
            scene.getStylesheets().add(url.toExternalForm());
        } else {
            LOGGER.log(Level.WARNING, "CSS resource not found: {0}", resourcePath);
        }
    }
}
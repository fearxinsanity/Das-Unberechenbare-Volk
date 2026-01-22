package de.schulprojekt.duv.view;

import de.schulprojekt.duv.view.controllers.DashboardController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Entry point of the JavaFX application.
 * @author Nico Hoffmann
 * @version 1.1
 */
public class Main extends Application {

    // ========================================
    // Static Variables
    // ========================================

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    private static final String APP_TITLE = "Das Unberechenbare Volk";
    private static final int WINDOW_WIDTH = 1200;
    private static final int WINDOW_HEIGHT = 750;

    private static final String FXML_START_VIEW = "/de/schulprojekt/duv/view/StartView.fxml";
    private static final String CSS_COMMON = "/de/schulprojekt/duv/common.css";
    private static final String CSS_START = "/de/schulprojekt/duv/start.css";
    private static final String ICON_PATH = "/de/schulprojekt/duv/Pictures/DUV_Logo.png";

    private static Locale currentLocale = Locale.GERMAN;

    // ========================================
    // Business Logic Methods
    // ========================================

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        showStartView(primaryStage);
    }

    public static void showStartView(Stage stage) {
        try {
            URL fxmlUrl = Main.class.getResource(FXML_START_VIEW);
            if (fxmlUrl == null) {
                throw new IOException("FXML resource not found: " + FXML_START_VIEW);
            }

            ResourceBundle bundle = ResourceBundle.getBundle("de.schulprojekt.duv.messages", currentLocale);
            FXMLLoader loader = new FXMLLoader(fxmlUrl, bundle);
            Parent root = loader.load();
            Object controller = loader.getController();

            Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);

            loadStylesheet(scene, CSS_COMMON);
            loadStylesheet(scene, CSS_START);

            setAppIcon(stage);

            stage.setTitle(APP_TITLE);
            stage.setScene(scene);
            stage.setResizable(true);

            stage.setOnCloseRequest(e -> {
                if (controller instanceof DashboardController) {
                    ((DashboardController) controller).shutdown();
                }
                Platform.exit();
                System.exit(0);
            });

            stage.show();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to start application.", e);
        }
    }

    public static Locale getLocale() {
        return currentLocale;
    }

    public static void setLocale(Locale locale) {
        currentLocale = locale;
    }

    // ========================================
    // Utility Methods
    // ========================================

    /**
     * Loads a stylesheet into the scene if it exists.
     * @param scene the target scene
     * @param resourcePath the path to the CSS file
     */
    private static void loadStylesheet(Scene scene, String resourcePath) {
        URL url = Main.class.getResource(resourcePath);
        if (url != null) {
            scene.getStylesheets().add(url.toExternalForm());
        } else {
            LOGGER.log(Level.WARNING, "CSS resource not found: " + resourcePath);
        }
    }

    /**
     * Sets the application icon.
     * @param stage the primary stage
     */
    private static void setAppIcon(Stage stage) {
        try (InputStream iconStream = Main.class.getResourceAsStream(ICON_PATH)) {
            if (iconStream != null) {
                stage.getIcons().add(new Image(iconStream));
            } else {
                LOGGER.log(Level.WARNING, "App icon resource not found at: {0}", ICON_PATH);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load app icon.", e);
        }
    }
}
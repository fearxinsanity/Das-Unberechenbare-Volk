package de.schulprojekt.duv.view;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/**
 * Einstiegspunkt der Anwendung.
 * Lädt das FXML-Layout, initialisiert die Scene und kümmert sich um das saubere Beenden.
 */
public class Main extends Application {

    private static final String FXML_PATH = "/de/schulprojekt/duv/view/DashboardUI.fxml";
    private static final String CSS_PATH = "/de/schulprojekt/duv/style.css";
    private static final String APP_TITLE = "Das Unberechenbare Volk - Simulation";

    @Override
    public void start(Stage primaryStage) {
        try {
            // 1. FXML laden
            URL fxmlUrl = getClass().getResource(FXML_PATH);
            if (fxmlUrl == null) {
                throw new IOException("FXML-Datei nicht gefunden: " + FXML_PATH);
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            // 2. Controller holen (wird automatisch vom FXMLLoader erstellt)
            DashboardController dashboardController = loader.getController();

            // 3. Scene erstellen
            Scene scene = new Scene(root, 1280, 800); // Standardgröße etwas erhöht für gute Übersicht

            // 4. Stylesheet laden
            URL cssUrl = getClass().getResource(CSS_PATH);
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            } else {
                System.err.println("Warnung: CSS-Datei nicht gefunden: " + CSS_PATH);
            }

            // 5. Stage konfigurieren
            primaryStage.setTitle(APP_TITLE);
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1024);
            primaryStage.setMinHeight(768);

            // 6. Shutdown-Logik: Threads beenden, wenn Fenster geschlossen wird
            primaryStage.setOnCloseRequest(e -> {
                if (dashboardController != null) {
                    dashboardController.shutdown();
                }
                Platform.exit();
                System.exit(0); // Sicherstellen, dass auch ExecutorServices sterben
            });

            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Kritischer Fehler beim Starten der Anwendung: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
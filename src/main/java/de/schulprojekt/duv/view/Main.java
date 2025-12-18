package de.schulprojekt.duv.view;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class Main extends Application {

    // Pfade zu den Ressourcen (müssen im resources-Ordner liegen)
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

            // 2. Controller holen (wird automatisch vom FXMLLoader instanziiert)
            DashboardController dashboardController = loader.getController();

            // 3. Scene erstellen
            Scene scene = new Scene(root, 1280, 800);

            // 4. Stylesheet laden (optional, falls vorhanden)
            URL cssUrl = getClass().getResource(CSS_PATH);
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            } else {
                System.out.println("Info: CSS-Datei nicht gefunden oder nicht benötigt.");
            }

            // 5. Stage konfigurieren
            primaryStage.setTitle(APP_TITLE);
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1024);
            primaryStage.setMinHeight(768);

            // 6. Shutdown-Logik: Threads sauber beenden
            primaryStage.setOnCloseRequest(e -> {
                if (dashboardController != null) {
                    // Ruft die Aufräum-Methode im Controller auf
                    dashboardController.shutdown();
                }
                Platform.exit();
                System.exit(0); // Beendet auch alle Hintergrund-Threads (ExecutorService)
            });

            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Kritischer Fehler beim Starten: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
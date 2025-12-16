package de.schulprojekt.duv.view;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class Main extends Application {

    private static final String FXML_PATH = "/de/schulprojekt/duv/view/DashboardUI.fxml";
    private static final String APP_TITLE = "Das Unberechenbare Volk";

    @Override
    public void start(Stage primaryStage) throws IOException {
        URL fxmlURL = getClass().getResource(FXML_PATH);
        if (fxmlURL == null) {
            throw new IOException("FXML resource not found: " + FXML_PATH);
        }

        FXMLLoader loader = new FXMLLoader(fxmlURL);
        Parent root = loader.load();

        // HINWEIS: Wir müssen hier nichts mehr manuell verknüpfen.
        // Der DashboardController initialisiert sich und die Simulation selbst in seiner initialize()-Methode.

        Scene scene = new Scene(root, 1200, 650);

        URL cssURL = getClass().getResource("/de/schulprojekt/duv/style.css");
        if (cssURL != null) {
            scene.getStylesheets().add(cssURL.toExternalForm());
        }

        primaryStage.setTitle(APP_TITLE);
        primaryStage.setScene(scene);
        primaryStage.setMaximized(false);
        primaryStage.setResizable(true);

        // WICHTIG: Beim Schließen der Anwendung alle Threads stoppen!
        primaryStage.setOnCloseRequest(e -> {
            DashboardController controller = loader.getController();
            if (controller != null) {
                controller.shutdown();
            }
            System.exit(0);
        });

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
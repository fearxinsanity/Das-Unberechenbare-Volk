package com.unberechenbarevolk.view;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

/**
 * Repräsentiert das Haupt-Benutzerinterface der Anwendung, das auf JavaFX basiert.
 * Enthält die visuellen Komponenten wie Diagramme, Slider und den Ereignis-Feed.
 */
public class DashboardUI extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        java.net.URL fxmlResource = getClass().getResource("Dashboard.fxml");
        if (fxmlResource == null) {
            throw new IOException("FXML resource 'Dashboard.fxml' not found. Please check the resource path.");
        }
        FXMLLoader loader = new FXMLLoader(fxmlResource);
        Parent root = loader.load();

        primaryStage.setTitle("Das unberechenbare Volk");
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.show();
    }
}

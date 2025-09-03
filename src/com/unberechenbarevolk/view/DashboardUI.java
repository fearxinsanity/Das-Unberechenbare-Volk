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
        FXMLLoader loader = new FXMLLoader(getClass().getResource("Dashboard.fxml"));
        Parent root = loader.load();

        primaryStage.setTitle("Das unberechenbare Volk");
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.show();
    }
}

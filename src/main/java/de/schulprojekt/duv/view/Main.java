package de.schulprojekt.duv.view;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        Label label = new Label("Das unberechenbare volk - Simulation (Smoke Test)");
        StackPane root = new StackPane(label);

        Scene scene = new Scene(root, 640, 480);

        primaryStage.setTitle("Wahl-Simulator");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

     static void main(String[] args) {
        launch(args);
    }
}

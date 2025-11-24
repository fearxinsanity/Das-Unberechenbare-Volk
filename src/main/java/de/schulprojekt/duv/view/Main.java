package de.schulprojekt.duv.view;

// --- JavaFX ---
import de.schulprojekt.duv.controller.SimulationController;
import de.schulprojekt.duv.model.engine.SimulationEngine;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/**
 * The main entry point for the JavaFX application.
 * It loads the main FXML layout and initializes the primary stage.
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        // Load FXML file from folder
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("Dashboard.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1600, 900); //Default Size

        DashboardController dashboardController = fxmlLoader.getController();
        SimulationEngine model = new SimulationEngine();
        SimulationController simulationController = new SimulationController(model, dashboardController);
        simulationController.initializeSimulation();

        //Load the CSS file
        URL cssUrl = Main.class.getResource("styles.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        } else {
            System.err.println("CSS resource not found");
        }

        primaryStage.setTitle("Das unberechenbare Volk");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * The main method is ignored in correctly deployed JavaFX applications.
     * main() serves only as fallback in case the application is launched
     * @param args the command line arguments
     */
    static void main(String[] args) {
        launch(args);
    }

}
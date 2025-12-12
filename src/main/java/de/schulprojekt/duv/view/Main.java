package de.schulprojekt.duv.view;

//--- Project internal ---
import de.schulprojekt.duv.controller.SimulationController;
//--- JavaFX ---
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

//--- Java Exception ---
import java.io.IOException;

//--- Java net ---
import java.net.URL;

/**
 * The main entry point for the JavaFX application.
 * It loads the main FXML layout and initializes the primary stage.
 */
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
        DashboardController view = loader.getController();
        SimulationController simulationController = new SimulationController(view);
        view.setSimulationController(simulationController);

        Scene scene = new Scene(root, 1200, 650);

        URL cssURL = getClass().getResource("/de/schulprojekt/duv/view/style.css");
        if (cssURL != null) {
            scene.getStylesheets().add(cssURL.toExternalForm());
        } else {
            System.err.println("FATAL: CSS-Datei 'style.css' konnte nicht gefunden werden.");
        }

        primaryStage.setTitle(APP_TITLE);
        primaryStage.setScene(scene);
        primaryStage.setMaximized(false);
        primaryStage.setResizable(true);
        primaryStage.show();  // ZUERST anzeigen!

        // DANACH die Visuals initialisieren (wenn Layout-Dimensionen bekannt sind)
        javafx.application.Platform.runLater(() -> {
            view.setupVisuals();
            view.updateDashboard(simulationController.getParties(), simulationController.getVoters());
        });
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
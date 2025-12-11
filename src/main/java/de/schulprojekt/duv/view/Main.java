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

            // ZUERST: Setup und Initialisierung der visuellen Elemente
            view.setupVisuals();

            Scene scene = new Scene(root, 1200, 650);

            // KORREKTUR: CSS-Stylesheet laden
            // Verwende den absoluten Pfad vom Ressourcen-Root (/de/schulprojekt/duv/view/style.css)
            URL cssURL = getClass().getResource("/de/schulprojekt/duv/view/style.css");

            if (cssURL != null) {
                scene.getStylesheets().add(cssURL.toExternalForm());
            } else {
                System.err.println("FATAL: CSS-Datei 'style.css' konnte nicht gefunden werden. Design wird nicht geladen.");
                // Falls das CSS fehlschlägt, ist das UI nutzbar, aber unschön.
            }

            // ZULETZT: Initiales Update nach Laden aller Styles und Knoten
            view.updateDashboard(simulationController.getParties(), simulationController.getVoters());

            primaryStage.setTitle(APP_TITLE);
            primaryStage.setScene(scene);
            primaryStage.setMaximized(false);
            primaryStage.setResizable(true); // Wir haben es auf True gesetzt
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
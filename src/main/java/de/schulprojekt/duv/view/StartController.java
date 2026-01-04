package de.schulprojekt.duv.view;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

/**
 * Steuert ausschließlich den Startbildschirm.
 * Trennung der Belange: Dieser Controller weiß nichts von der Simulation,
 * er weiß nur, wie man zur Simulations-Ansicht wechselt.
 */
public class StartController {

    private static final String DASHBOARD_FXML = "/de/schulprojekt/duv/view/DashboardUI.fxml";
    private static final String CSS_PATH = "/de/schulprojekt/duv/style.css";

    @FXML
    public void handleStartSimulation(ActionEvent event) {
        try {
            switchScene(event, DASHBOARD_FXML);
        } catch (IOException e) {
            e.printStackTrace();
            // Hier könnte man noch einen Error-Dialog anzeigen
        }
    }

    @FXML
    public void handleExitApplication(ActionEvent event) {
        System.exit(0);
    }

    /**
     * Kapselt die Logik für den Szenenwechsel, um den Code lesbar zu halten.
     */
    private void switchScene(ActionEvent event, String fxmlPath) throws IOException {
        // 1. Loader vorbereiten
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Parent root = loader.load();

        // 2. Stage holen (vom Button aus, der geklickt wurde)
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        // 3. Neue Scene erstellen (übernimmt aktuelle Größe des Fensters)
        Scene newScene = new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight());

        // 4. Stylesheet muss neu gesetzt werden, da es an der Scene hängt
        newScene.getStylesheets().add(getClass().getResource(CSS_PATH).toExternalForm());

        // 5. Scene setzen und anzeigen
        stage.setScene(newScene);
        stage.show();
    }
}
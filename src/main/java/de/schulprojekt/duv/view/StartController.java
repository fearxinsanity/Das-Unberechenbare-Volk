package de.schulprojekt.duv.view;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.Locale;

public class StartController {

    private static final String DASHBOARD_FXML = "/de/schulprojekt/duv/view/DashboardUI.fxml";
    private static final String CSS_PATH = "/de/schulprojekt/duv/style.css";

    @FXML private ImageView logoImageView;
    @FXML private VBox cardBox;
    @FXML private Region glowRegion;

    @FXML
    public void initialize() {
        // Responsive Logik erst starten, wenn die Scene bereit ist
        Platform.runLater(() -> {
            if (logoImageView.getScene() != null) {
                Scene scene = logoImageView.getScene();

                // 1. Schriftgrößen-Skalierung (Identisch zum Dashboard für Konsistenz)
                scene.widthProperty().addListener((obs, oldVal, newVal) -> adjustScale(newVal.doubleValue()));
                adjustScale(scene.getWidth()); // Initialer Aufruf

                // 2. Logo dynamisch skalieren (50% der Fensterbreite, aber max 700px)
                logoImageView.fitWidthProperty().bind(
                        Bindings.min(700, scene.widthProperty().multiply(0.5))
                );

                // 3. Karten-Breite anpassen (60% der Fensterbreite, min 400px, max 800px)
                cardBox.maxWidthProperty().bind(
                        Bindings.max(400, Bindings.min(800, scene.widthProperty().multiply(0.6)))
                );

                // 4. Glow-Effekt anpassen (etwas breiter als die Karte)
                glowRegion.maxWidthProperty().bind(cardBox.maxWidthProperty().multiply(1.2));
                glowRegion.maxHeightProperty().bind(cardBox.heightProperty().multiply(0.8));
            }
        });
    }

    private void adjustScale(double windowWidth) {
        if (logoImageView.getScene() == null) return;

        // Basis: 12px bei 1280px Breite (Gleiche Formel wie DashboardController)
        double baseSize = 12.0;
        double scaleFactor = windowWidth / 1280.0;

        // Sanfte Skalierung
        double newSize = Math.max(11.0, Math.min(20.0, baseSize * Math.sqrt(scaleFactor)));

        logoImageView.getScene().getRoot().setStyle("-fx-font-size: " + String.format(Locale.US, "%.1f", newSize) + "px;");
    }

    @FXML
    public void handleStartSimulation(ActionEvent event) {
        try {
            switchScene(event, DASHBOARD_FXML);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleExitApplication(ActionEvent event) {
        System.exit(0);
    }

    private void switchScene(ActionEvent event, String fxmlPath) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Parent root = loader.load();

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        // WICHTIG: Die Scene-Größe beibehalten beim Wechsel
        Scene currentScene = stage.getScene();
        Scene newScene = new Scene(root, currentScene.getWidth(), currentScene.getHeight());

        if (getClass().getResource(CSS_PATH) != null) {
            newScene.getStylesheets().add(getClass().getResource(CSS_PATH).toExternalForm());
        }

        stage.setScene(newScene);
        stage.show();
    }
}
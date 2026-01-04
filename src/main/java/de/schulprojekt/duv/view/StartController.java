package de.schulprojekt.duv.view;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.Locale;

public class StartController {

    private static final String DASHBOARD_FXML = "/de/schulprojekt/duv/view/DashboardUI.fxml";
    private static final String CSS_PATH = "/de/schulprojekt/duv/style.css";

    @FXML private ImageView logoImageView;
    @FXML private VBox cardBox;
    @FXML private AnchorPane hudLayer;

    // WIEDER DA: Referenz auf den Glow
    @FXML private Region glowRegion;

    @FXML
    public void initialize() {
        // Initiale Transparenz
        cardBox.setOpacity(0);
        hudLayer.setOpacity(0);

        // WIEDER DA: Unsichtbar machen für Animation
        glowRegion.setOpacity(0);

        Platform.runLater(() -> {
            if (logoImageView.getScene() != null) {
                Scene scene = logoImageView.getScene();
                startIntroAnimation();

                // 1. Skalierung
                scene.widthProperty().addListener((obs, oldVal, newVal) -> adjustScale(newVal.doubleValue()));
                adjustScale(scene.getWidth());

                // 2. Logo
                logoImageView.fitWidthProperty().bind(
                        Bindings.min(600, scene.widthProperty().multiply(0.45))
                );

                // 3. Karte
                cardBox.maxWidthProperty().bind(
                        Bindings.max(450, Bindings.min(850, scene.widthProperty().multiply(0.65)))
                );

                // WIEDER DA: 4. Glow-Effekt an Karte binden
                glowRegion.maxWidthProperty().bind(cardBox.maxWidthProperty().multiply(1.3));
                glowRegion.maxHeightProperty().bind(cardBox.heightProperty().multiply(0.9));
            }
        });
    }

    private void startIntroAnimation() {
        // HUD
        FadeTransition ftHud = new FadeTransition(Duration.seconds(1.5), hudLayer);
        ftHud.setFromValue(0);
        ftHud.setToValue(1.0);
        ftHud.play();

        // Karte
        FadeTransition ftCard = new FadeTransition(Duration.seconds(2.0), cardBox);
        ftCard.setFromValue(0);
        ftCard.setToValue(1.0);
        ftCard.setDelay(Duration.seconds(0.5));
        ftCard.play();

        // WIEDER DA: Glow Animation
        FadeTransition ftGlow = new FadeTransition(Duration.seconds(3.0), glowRegion);
        ftGlow.setFromValue(0);
        ftGlow.setToValue(0.8); // 80% Deckkraft
        ftGlow.setDelay(Duration.seconds(0.8));
        ftGlow.play();
    }

    private void adjustScale(double windowWidth) {
        if (logoImageView.getScene() == null) return;
        double baseSize = 12.0;
        double scaleFactor = windowWidth / 1280.0;
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
        Scene currentScene = stage.getScene();
        Scene newScene = new Scene(root, currentScene.getWidth(), currentScene.getHeight());
        if (getClass().getResource(CSS_PATH) != null) {
            newScene.getStylesheets().add(getClass().getResource(CSS_PATH).toExternalForm());
        }
        stage.setScene(newScene);
        stage.show();
    }
}
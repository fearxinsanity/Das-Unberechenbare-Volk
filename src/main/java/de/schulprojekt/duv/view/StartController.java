package de.schulprojekt.duv.view;

import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class StartController {

    private static final String DASHBOARD_FXML = "/de/schulprojekt/duv/view/DashboardUI.fxml";
    private static final String CSS_PATH = "/de/schulprojekt/duv/style.css";

    @FXML private StackPane rootPane;
    @FXML private ImageView logoImageView;
    @FXML private VBox cardBox;
    @FXML private AnchorPane hudLayer;
    @FXML private Region glowRegion;
    @FXML private Canvas codeCanvas;

    // --- Animation Logic: Particle Network ---
    private AnimationTimer animTimer;
    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();

    // Konfiguration
    private static final int PARTICLE_COUNT = 80;
    // Basis-Abstand bei einer Referenzauflösung (z.B. Full HD)
    private static final double BASE_CONNECTION_DIST = 160;
    // Basis-Geschwindigkeit (relativ zur Screen-Größe)
    private static final double RELATIVE_SPEED = 0.0005;

    @FXML
    public void initialize() {
        cardBox.setOpacity(0);
        hudLayer.setOpacity(0);
        glowRegion.setOpacity(0);

        // Canvas immer an Root-Größe binden
        codeCanvas.widthProperty().bind(rootPane.widthProperty());
        codeCanvas.heightProperty().bind(rootPane.heightProperty());

        Platform.runLater(() -> {
            if (logoImageView.getScene() != null) {
                Scene scene = logoImageView.getScene();
                startIntroAnimation();

                // Partikel initialisieren (nutzen jetzt relative 0.0-1.0 Koordinaten)
                initParticles();
                startNetworkAnimation();

                // --- UI Skalierung ---
                scene.widthProperty().addListener((obs, oldVal, newVal) -> adjustScale(newVal.doubleValue()));
                adjustScale(scene.getWidth());

                logoImageView.fitWidthProperty().bind(
                        Bindings.min(600, scene.widthProperty().multiply(0.45))
                );

                cardBox.maxWidthProperty().bind(
                        Bindings.min(scene.widthProperty().multiply(0.90), 850)
                );

                glowRegion.maxWidthProperty().bind(cardBox.widthProperty().multiply(1.3));
                glowRegion.maxHeightProperty().bind(cardBox.heightProperty().multiply(0.9));
            }
        });
    }

    // --- Partikel Klasse (Jetzt mit relativen Koordinaten 0.0 bis 1.0) ---
    private static class Particle {
        double relX, relY;   // Position 0.0 - 1.0
        double velX, velY;   // Geschwindigkeit
        double sizeBase;     // Basis-Größe

        Particle(Random r) {
            this.relX = r.nextDouble();
            this.relY = r.nextDouble();

            // Zufällige Richtung
            this.velX = (r.nextDouble() - 0.5) * RELATIVE_SPEED;
            this.velY = (r.nextDouble() - 0.5) * RELATIVE_SPEED;

            this.sizeBase = 1.5 + r.nextDouble() * 2.0;
        }

        void update() {
            relX += velX;
            relY += velY;

            // Abprallen an den Rändern (0.0 und 1.0)
            if (relX < 0) { relX = 0; velX *= -1; }
            if (relX > 1) { relX = 1; velX *= -1; }
            if (relY < 0) { relY = 0; velY *= -1; }
            if (relY > 1) { relY = 1; velY *= -1; }
        }
    }

    private void initParticles() {
        particles.clear();
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            particles.add(new Particle(random));
        }
    }

    private void startNetworkAnimation() {
        GraphicsContext gc = codeCanvas.getGraphicsContext2D();

        animTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                drawNetwork(gc, codeCanvas.getWidth(), codeCanvas.getHeight());
            }
        };
        animTimer.start();
    }

    private void drawNetwork(GraphicsContext gc, double w, double h) {
        // 1. Alles löschen
        gc.clearRect(0, 0, w, h);

        // 2. Skalierungsfaktor berechnen (Basis: Diagonale von ca. 1400px = 1200x750)
        // Wenn das Fenster größer wird, wird dieser Faktor > 1.0
        double currentDiagonal = Math.sqrt(w * w + h * h);
        double scaleFactor = currentDiagonal / 1400.0;

        // Begrenzung nach unten, damit es auf Mini-Screens nicht verschwindet
        scaleFactor = Math.max(0.5, scaleFactor);

        // Dynamischer Verbindungsabstand
        double activeConnectionDist = BASE_CONNECTION_DIST * scaleFactor;

        gc.setFill(Color.web("#D4AF37"));

        // 3. Punkte zeichnen (Pixel-Position = Relativ * Breite/Höhe)
        for (Particle p : particles) {
            p.update(); // Bewegung berechnen

            double px = p.relX * w;
            double py = p.relY * h;
            double pSize = p.sizeBase * scaleFactor; // Auch Punkte wachsen leicht mit

            gc.fillOval(px, py, pSize, pSize);
        }

        // 4. Verbindungen zeichnen
        gc.setLineWidth(0.8 * scaleFactor); // Linien werden auch dicker

        for (int i = 0; i < particles.size(); i++) {
            Particle p1 = particles.get(i);
            double p1x = p1.relX * w;
            double p1y = p1.relY * h;

            for (int j = i + 1; j < particles.size(); j++) {
                Particle p2 = particles.get(j);
                double p2x = p2.relX * w;
                double p2y = p2.relY * h;

                double dx = p1x - p2x;
                double dy = p1y - p2y;
                double dist = Math.sqrt(dx * dx + dy * dy);

                // Prüfen gegen skalierten Abstand
                if (dist < activeConnectionDist) {
                    double opacity = (1.0 - (dist / activeConnectionDist)) * 0.4;
                    gc.setStroke(Color.color(0.83, 0.68, 0.21, opacity));

                    // Zeichne Linie zwischen den Mitten der Punkte
                    double offset = (p1.sizeBase * scaleFactor) / 2;
                    gc.strokeLine(p1x + offset, p1y + offset,
                            p2x + offset, p2y + offset);
                }
            }
        }
    }

    private void startIntroAnimation() {
        FadeTransition ftHud = new FadeTransition(Duration.seconds(1.5), hudLayer);
        ftHud.setFromValue(0); ftHud.setToValue(1.0); ftHud.play();

        FadeTransition ftCard = new FadeTransition(Duration.seconds(2.0), cardBox);
        ftCard.setFromValue(0); ftCard.setToValue(1.0); ftCard.setDelay(Duration.seconds(0.5)); ftCard.play();

        FadeTransition ftGlow = new FadeTransition(Duration.seconds(3.0), glowRegion);
        ftGlow.setFromValue(0); ftGlow.setToValue(0.8);
        ftGlow.setDelay(Duration.seconds(0.8));
        ftGlow.play();
    }

    private void adjustScale(double windowWidth) {
        if (logoImageView.getScene() == null) return;

        double baseSize = 12.0;
        double scaleFactor = windowWidth / 1280.0;
        double newSize = Math.max(10.0, Math.min(24.0, baseSize * scaleFactor));

        logoImageView.getScene().getRoot().setStyle("-fx-font-size: " + String.format(Locale.US, "%.1f", newSize) + "px;");
    }

    @FXML
    public void handleStartSimulation(ActionEvent event) {
        if (animTimer != null) animTimer.stop();
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
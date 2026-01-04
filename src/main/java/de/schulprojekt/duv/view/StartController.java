package de.schulprojekt.duv.view;

import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
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
import javafx.util.Duration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class StartController {

    private static final String DASHBOARD_FXML = "/de/schulprojekt/duv/view/DashboardUI.fxml";
    private static final String DASHBOARD_CSS = "/de/schulprojekt/duv/dashboard.css";

    @FXML private StackPane rootPane;
    @FXML private ImageView logoImageView;
    @FXML private VBox cardBox;
    @FXML private AnchorPane hudLayer;
    @FXML private Region glowRegion;
    @FXML private Canvas codeCanvas;

    // NEU: Referenz auf das Hintergrund-Gitter
    @FXML private Region gridRegion;

    // --- Animation Logic ---
    private AnimationTimer animTimer;
    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();
    private static final int PARTICLE_COUNT = 80;
    private static final double BASE_CONNECTION_DIST = 160;
    private static final double RELATIVE_SPEED = 0.0005;

    @FXML
    public void initialize() {
        cardBox.setOpacity(0);
        hudLayer.setOpacity(0);
        glowRegion.setOpacity(0);

        codeCanvas.widthProperty().bind(rootPane.widthProperty());
        codeCanvas.heightProperty().bind(rootPane.heightProperty());

        Platform.runLater(() -> {
            if (logoImageView.getScene() != null) {
                Scene scene = logoImageView.getScene();
                startIntroAnimation();

                initParticles();
                startNetworkAnimation();

                scene.widthProperty().addListener((obs, oldVal, newVal) -> adjustScale(newVal.doubleValue()));
                adjustScale(scene.getWidth());

                logoImageView.fitWidthProperty().bind(Bindings.min(600, scene.widthProperty().multiply(0.45)));
                cardBox.maxWidthProperty().bind(Bindings.min(scene.widthProperty().multiply(0.90), 850));

                glowRegion.maxWidthProperty().bind(cardBox.widthProperty().multiply(1.3));
                glowRegion.maxHeightProperty().bind(cardBox.heightProperty().multiply(0.9));
            }
        });
    }

    // --- Partikel Logik ---
    private static class Particle {
        double relX, relY, velX, velY, sizeBase;
        Particle(Random r) {
            this.relX = r.nextDouble(); this.relY = r.nextDouble();
            this.velX = (r.nextDouble() - 0.5) * RELATIVE_SPEED;
            this.velY = (r.nextDouble() - 0.5) * RELATIVE_SPEED;
            this.sizeBase = 1.5 + r.nextDouble() * 2.0;
        }
        void update() {
            relX += velX; relY += velY;
            if (relX < 0) { relX = 0; velX *= -1; }
            if (relX > 1) { relX = 1; velX *= -1; }
            if (relY < 0) { relY = 0; velY *= -1; }
            if (relY > 1) { relY = 1; velY *= -1; }
        }
    }

    private void initParticles() {
        particles.clear();
        for (int i = 0; i < PARTICLE_COUNT; i++) particles.add(new Particle(random));
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
        gc.clearRect(0, 0, w, h);
        double scaleFactor = Math.max(0.5, Math.sqrt(w*w + h*h) / 1400.0);
        double activeConnectionDist = BASE_CONNECTION_DIST * scaleFactor;

        gc.setFill(Color.web("#D4AF37"));
        for (Particle p : particles) {
            p.update();
            gc.fillOval(p.relX * w, p.relY * h, p.sizeBase * scaleFactor, p.sizeBase * scaleFactor);
        }

        gc.setLineWidth(0.8 * scaleFactor);
        for (int i = 0; i < particles.size(); i++) {
            Particle p1 = particles.get(i);
            for (int j = i + 1; j < particles.size(); j++) {
                Particle p2 = particles.get(j);
                double dx = (p1.relX - p2.relX) * w;
                double dy = (p1.relY - p2.relY) * h;
                double dist = Math.sqrt(dx*dx + dy*dy);
                if (dist < activeConnectionDist) {
                    gc.setStroke(Color.color(0.83, 0.68, 0.21, (1.0 - dist/activeConnectionDist) * 0.4));
                    gc.strokeLine(p1.relX*w + (p1.sizeBase*scaleFactor)/2, p1.relY*h + (p1.sizeBase*scaleFactor)/2,
                            p2.relX*w + (p2.sizeBase*scaleFactor)/2, p2.relY*h + (p2.sizeBase*scaleFactor)/2);
                }
            }
        }
    }

    private void startIntroAnimation() {
        FadeTransition ftHud = new FadeTransition(Duration.seconds(1.5), hudLayer); ftHud.setFromValue(0); ftHud.setToValue(1.0); ftHud.play();
        FadeTransition ftCard = new FadeTransition(Duration.seconds(2.0), cardBox); ftCard.setFromValue(0); ftCard.setToValue(1.0); ftCard.setDelay(Duration.seconds(0.5)); ftCard.play();
        FadeTransition ftGlow = new FadeTransition(Duration.seconds(3.0), glowRegion); ftGlow.setFromValue(0); ftGlow.setToValue(0.8); ftGlow.setDelay(Duration.seconds(0.8)); ftGlow.play();
    }

    private void adjustScale(double windowWidth) {
        if (logoImageView.getScene() == null) return;
        double newSize = Math.max(10.0, Math.min(24.0, 12.0 * (windowWidth / 1280.0)));
        logoImageView.getScene().getRoot().setStyle("-fx-font-size: " + String.format(Locale.US, "%.1f", newSize) + "px;");
    }

    // =========================================================================
    // === TRANSITION: Overlay + Cleanup ===
    // =========================================================================

    @FXML
    public void handleStartSimulation(ActionEvent event) {
        try {
            // 1. Dashboard laden
            FXMLLoader loader = new FXMLLoader(getClass().getResource(DASHBOARD_FXML));
            Parent dashboardRoot = loader.load();

            // Transparent machen, damit der Übergang weich ist
            dashboardRoot.setStyle("-fx-background-color: transparent;");
            dashboardRoot.setOpacity(0);
            dashboardRoot.setScaleX(0.95);
            dashboardRoot.setScaleY(0.95);

            String dashboardCssUrl = getClass().getResource(DASHBOARD_CSS).toExternalForm();
            if (dashboardCssUrl != null) rootPane.getScene().getStylesheets().add(dashboardCssUrl);

            // Dashboard ÜBER alles legen
            rootPane.getChildren().add(dashboardRoot);

            // 2. ANIMATIONEN

            // A. Start-Elemente schnell ausblenden
            FadeTransition ftCard = new FadeTransition(Duration.seconds(0.6), cardBox); ftCard.setToValue(0);
            FadeTransition ftHud = new FadeTransition(Duration.seconds(0.4), hudLayer); ftHud.setToValue(0);
            FadeTransition ftGlow = new FadeTransition(Duration.seconds(0.4), glowRegion); ftGlow.setToValue(0);

            // B. Hintergrund (Gitter & Partikel) LANGSAM ausblenden (löst das Problem "Animation stört")
            // Sie verschwinden, während das Dashboard erscheint.
            FadeTransition ftGrid = new FadeTransition(Duration.seconds(1.5), gridRegion);
            ftGrid.setToValue(0); // Gitter weg

            FadeTransition ftCanvas = new FadeTransition(Duration.seconds(1.5), codeCanvas);
            ftCanvas.setToValue(0); // Partikel weg

            // C. Dashboard einblenden
            FadeTransition ftDash = new FadeTransition(Duration.seconds(1.2), dashboardRoot);
            ftDash.setToValue(1.0);

            ScaleTransition stDash = new ScaleTransition(Duration.seconds(1.2), dashboardRoot);
            stDash.setToX(1.0);
            stDash.setToY(1.0);

            // Alle Animationen parallel starten
            ParallelTransition pt = new ParallelTransition(ftCard, ftHud, ftGlow, ftGrid, ftCanvas, ftDash, stDash);

            pt.setOnFinished(e -> {
                // Aufräumen: Timer stoppen (Spart CPU!)
                if (animTimer != null) animTimer.stop();

                // Unsichtbare Elemente verstecken/entfernen
                cardBox.setVisible(false);
                hudLayer.setVisible(false);
                glowRegion.setVisible(false);
                gridRegion.setVisible(false);
                codeCanvas.setVisible(false);
            });

            pt.play();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleExitApplication(ActionEvent event) {
        System.exit(0);
    }
}
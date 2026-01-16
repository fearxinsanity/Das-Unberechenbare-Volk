package de.schulprojekt.duv.view.controllers;

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
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for the application start screen and login visuals.
 * @author Nico Hoffmann
 * @version 1.1
 */
public class StartController {

    // ========================================
    // Static Variables
    // ========================================

    private static final Logger LOGGER = Logger.getLogger(StartController.class.getName());
    private static final long DEFAULT_POPULATION = 250_000;
    private static final long DEFAULT_BUDGET = 500_000;

    private static final String DASHBOARD_FXML = "/de/schulprojekt/duv/view/DashboardUI.fxml";
    private static final String DASHBOARD_CSS = "/de/schulprojekt/duv/dashboard.css";

    private static final int PARTICLE_COUNT = 90;
    private static final double BASE_CONNECTION_DIST = 170;
    private static final double RELATIVE_SPEED = 0.0004;

    // ========================================
    // Instance Variables
    // ========================================

    @FXML private StackPane rootPane;
    @FXML private ImageView logoImageView;
    @FXML private VBox cardBox;
    @FXML private AnchorPane hudLayer;
    @FXML private Region glowRegion;
    @FXML private Canvas codeCanvas;
    @FXML private Region gridRegion;

    private AnimationTimer animTimer;
    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();

    // ========================================
    // Business Logic Methods
    // ========================================

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

                scene.widthProperty().addListener((_, _, newVal) -> adjustScale(newVal.doubleValue()));
                adjustScale(scene.getWidth());

                logoImageView.fitWidthProperty().bind(Bindings.min(500, scene.widthProperty().multiply(0.4)));
                cardBox.maxWidthProperty().bind(Bindings.min(scene.widthProperty().multiply(0.8), 600));
            }
        });
    }

    @FXML
    public void handleStartSimulation(ActionEvent ignored) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(DASHBOARD_FXML));
            Parent dashboardRoot = loader.load();

            DashboardController dashboardCtrl = loader.getController();
            if (dashboardCtrl != null) {
                dashboardCtrl.applyInitialSettings(DEFAULT_POPULATION, DEFAULT_BUDGET);
            }

            dashboardRoot.setStyle("-fx-background-color: transparent;");
            dashboardRoot.setOpacity(0);
            dashboardRoot.setScaleX(0.95);
            dashboardRoot.setScaleY(0.95);

            URL cssUrl = getClass().getResource(DASHBOARD_CSS);
            if (cssUrl != null && rootPane.getScene() != null) {
                rootPane.getScene().getStylesheets().add(cssUrl.toExternalForm());
            }

            rootPane.getChildren().add(dashboardRoot);
            buildTransitionAnimation(dashboardRoot).play();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load Dashboard view.", e);
        }
    }

    @FXML
    public void handleExitApplication(ActionEvent ignored) {
        Platform.exit();
        System.exit(0);
    }

    // ========================================
    // Utility Methods
    // ========================================

    private void startIntroAnimation() {
        FadeTransition ftHud = new FadeTransition(Duration.seconds(1.0), hudLayer);
        ftHud.setToValue(1.0); ftHud.play();

        FadeTransition ftCard = new FadeTransition(Duration.seconds(1.5), cardBox);
        ftCard.setToValue(1.0); ftCard.setDelay(Duration.seconds(0.2)); ftCard.play();

        FadeTransition ftGlow = new FadeTransition(Duration.seconds(2.5), glowRegion);
        ftGlow.setToValue(0.8); ftGlow.setDelay(Duration.seconds(0.5)); ftGlow.play();
    }

    private void startNetworkAnimation() {
        GraphicsContext gc = codeCanvas.getGraphicsContext2D();
        animTimer = new AnimationTimer() {
            @Override public void handle(long now) { drawNetwork(gc, codeCanvas.getWidth(), codeCanvas.getHeight()); }
        };
        animTimer.start();
    }

    private void drawNetwork(GraphicsContext gc, double w, double h) {
        gc.clearRect(0, 0, w, h);
        double scaleFactor = Math.max(0.5, Math.sqrt(w * w + h * h) / 1400.0);
        double activeDist = BASE_CONNECTION_DIST * scaleFactor;

        gc.setFill(Color.web("#D4AF37"));
        for (Particle p : particles) {
            p.update();
            gc.fillOval(p.relX * w, p.relY * h, p.sizeBase * scaleFactor, p.sizeBase * scaleFactor);
        }

        gc.setLineWidth(0.6 * scaleFactor);
        for (int i = 0; i < particles.size(); i++) {
            Particle p1 = particles.get(i);
            for (int j = i + 1; j < particles.size(); j++) {
                Particle p2 = particles.get(j);
                double dist = Math.sqrt(Math.pow((p1.relX - p2.relX) * w, 2) + Math.pow((p1.relY - p2.relY) * h, 2));

                if (dist < activeDist) {
                    double opacity = (1.0 - dist / activeDist) * 0.3;
                    gc.setStroke(Color.color(0.83, 0.68, 0.21, opacity));
                    gc.strokeLine(p1.relX * w + p1.sizeBase/2, p1.relY * h + p1.sizeBase/2,
                            p2.relX * w + p2.sizeBase/2, p2.relY * h + p2.sizeBase/2);
                }
            }
        }
    }

    private void initParticles() {
        particles.clear();
        for (int i = 0; i < PARTICLE_COUNT; i++) particles.add(new Particle(random));
    }

    private void adjustScale(double windowWidth) {
        if (logoImageView.getScene() == null) return;
        double rawSize = 12.0 * (windowWidth / 1280.0);
        logoImageView.getScene().getRoot().setStyle("-fx-font-size: " + String.format(Locale.US, "%.1f", Math.clamp(rawSize, 10.0, 24.0)) + "px;");
    }

    private ParallelTransition buildTransitionAnimation(Parent dashboardRoot) {
        FadeTransition ftCard = new FadeTransition(Duration.seconds(0.5), cardBox); ftCard.setToValue(0);
        FadeTransition ftHud = new FadeTransition(Duration.seconds(0.3), hudLayer); ftHud.setToValue(0);
        FadeTransition ftGlow = new FadeTransition(Duration.seconds(0.3), glowRegion); ftGlow.setToValue(0);
        FadeTransition ftGrid = new FadeTransition(Duration.seconds(1.0), gridRegion); ftGrid.setToValue(0);
        FadeTransition ftCanvas = new FadeTransition(Duration.seconds(1.0), codeCanvas); ftCanvas.setToValue(0);

        FadeTransition ftDash = new FadeTransition(Duration.seconds(1.0), dashboardRoot); ftDash.setToValue(1.0);
        ScaleTransition stDash = new ScaleTransition(Duration.seconds(1.0), dashboardRoot); stDash.setToX(1.0); stDash.setToY(1.0);

        ParallelTransition pt = new ParallelTransition(ftCard, ftHud, ftGlow, ftGrid, ftCanvas, ftDash, stDash);
        pt.setOnFinished(ignored -> {
            if (animTimer != null) animTimer.stop();
            cardBox.setVisible(false); hudLayer.setVisible(false); glowRegion.setVisible(false);
            gridRegion.setVisible(false); codeCanvas.setVisible(false);
        });
        return pt;
    }

    // ========================================
    // Inner Classes
    // ========================================

    private static class Particle {
        double relX, relY, velX, velY, sizeBase;
        Particle(Random r) {
            this.relX = r.nextDouble(); this.relY = r.nextDouble();
            this.velX = (r.nextDouble() - 0.5) * RELATIVE_SPEED;
            this.velY = (r.nextDouble() - 0.5) * RELATIVE_SPEED;
            this.sizeBase = 1.0 + r.nextDouble() * 2.0;
        }
        void update() {
            relX += velX; relY += velY;
            if (relX < 0 || relX > 1) velX *= -1;
            if (relY < 0 || relY > 1) velY *= -1;
        }
    }
}
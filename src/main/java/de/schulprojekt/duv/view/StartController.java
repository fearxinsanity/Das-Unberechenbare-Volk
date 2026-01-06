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
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controls the intro screen, background particle effects, and the transition to the dashboard.
 */
public class StartController {

    private static final Logger LOGGER = Logger.getLogger(StartController.class.getName());

    // --- Constants: Resources ---
    private static final String DASHBOARD_FXML = "/de/schulprojekt/duv/view/DashboardUI.fxml";
    private static final String DASHBOARD_CSS = "/de/schulprojekt/duv/dashboard.css";

    // --- Constants: Animation ---
    private static final int PARTICLE_COUNT = 80;
    private static final double BASE_CONNECTION_DIST = 160;
    private static final double RELATIVE_SPEED = 0.0005;

    // --- FXML: UI Components ---
    @FXML private StackPane rootPane;
    @FXML private ImageView logoImageView;
    @FXML private VBox cardBox;
    @FXML private AnchorPane hudLayer;
    @FXML private Region glowRegion;
    @FXML private Canvas codeCanvas;
    @FXML private Region gridRegion;

    // --- State ---
    private AnimationTimer animTimer;
    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();

    // --- Initialization ---

    @FXML
    public void initialize() {
        // Initial visibility states
        cardBox.setOpacity(0);
        hudLayer.setOpacity(0);
        glowRegion.setOpacity(0);

        // Bind canvas to full screen
        codeCanvas.widthProperty().bind(rootPane.widthProperty());
        codeCanvas.heightProperty().bind(rootPane.heightProperty());

        Platform.runLater(() -> {
            if (logoImageView.getScene() != null) {
                Scene scene = logoImageView.getScene();

                startIntroAnimation();
                initParticles();
                startNetworkAnimation();

                // Responsive scaling listener
                scene.widthProperty().addListener((ignored, ignored2, newVal) ->
                        adjustScale(newVal.doubleValue())
                );
                adjustScale(scene.getWidth());

                // Layout bindings
                logoImageView.fitWidthProperty().bind(Bindings.min(600, scene.widthProperty().multiply(0.45)));
                cardBox.maxWidthProperty().bind(Bindings.min(scene.widthProperty().multiply(0.90), 850));

                glowRegion.maxWidthProperty().bind(cardBox.widthProperty().multiply(1.3));
                glowRegion.maxHeightProperty().bind(cardBox.heightProperty().multiply(0.9));
            }
        });
    }

    // --- Event Handlers ---

    @FXML
    public void handleStartSimulation(ActionEvent ignored) {
        try {
            // 1. Load Dashboard
            FXMLLoader loader = new FXMLLoader(getClass().getResource(DASHBOARD_FXML));
            Parent dashboardRoot = loader.load();

            // 2. Prepare Dashboard (Transparent & Scaled Down for effect)
            dashboardRoot.setStyle("-fx-background-color: transparent;");
            dashboardRoot.setOpacity(0);
            dashboardRoot.setScaleX(0.95);
            dashboardRoot.setScaleY(0.95);

            // 3. Apply CSS safely
            URL cssUrl = getClass().getResource(DASHBOARD_CSS);
            if (cssUrl != null) {
                rootPane.getScene().getStylesheets().add(cssUrl.toExternalForm());
            } else {
                LOGGER.warning("Dashboard CSS not found: " + DASHBOARD_CSS);
            }

            // 4. Add to scene (layered on top)
            rootPane.getChildren().add(dashboardRoot);

            // 5. Build and Play Transition
            // FIX: Inlined redundant variable 'transition'
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

    // --- Animation Logic: Setup ---

    private void startIntroAnimation() {
        FadeTransition ftHud = new FadeTransition(Duration.seconds(1.5), hudLayer);
        ftHud.setFromValue(0); ftHud.setToValue(1.0);
        ftHud.play();

        FadeTransition ftCard = new FadeTransition(Duration.seconds(2.0), cardBox);
        ftCard.setFromValue(0); ftCard.setToValue(1.0);
        ftCard.setDelay(Duration.seconds(0.5));
        ftCard.play();

        FadeTransition ftGlow = new FadeTransition(Duration.seconds(3.0), glowRegion);
        ftGlow.setFromValue(0); ftGlow.setToValue(0.8);
        ftGlow.setDelay(Duration.seconds(0.8));
        ftGlow.play();
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

    // --- Animation Logic: Drawing ---

    private void drawNetwork(GraphicsContext gc, double w, double h) {
        gc.clearRect(0, 0, w, h);
        double scaleFactor = Math.max(0.5, Math.sqrt(w * w + h * h) / 1400.0);
        double activeConnectionDist = BASE_CONNECTION_DIST * scaleFactor;

        // Draw Nodes
        gc.setFill(Color.web("#D4AF37"));
        for (Particle p : particles) {
            p.update();
            gc.fillOval(p.relX * w, p.relY * h, p.sizeBase * scaleFactor, p.sizeBase * scaleFactor);
        }

        // Draw Connections
        gc.setLineWidth(0.8 * scaleFactor);
        for (int i = 0; i < particles.size(); i++) {
            Particle p1 = particles.get(i);
            for (int j = i + 1; j < particles.size(); j++) {
                Particle p2 = particles.get(j);

                double dx = (p1.relX - p2.relX) * w;
                double dy = (p1.relY - p2.relY) * h;
                double dist = Math.sqrt(dx * dx + dy * dy);

                if (dist < activeConnectionDist) {
                    double opacity = (1.0 - dist / activeConnectionDist) * 0.4;
                    gc.setStroke(Color.color(0.83, 0.68, 0.21, opacity));

                    double offset1 = (p1.sizeBase * scaleFactor) / 2;
                    double offset2 = (p2.sizeBase * scaleFactor) / 2;

                    gc.strokeLine(
                            p1.relX * w + offset1, p1.relY * h + offset1,
                            p2.relX * w + offset2, p2.relY * h + offset2
                    );
                }
            }
        }
    }

    // --- Helper Methods ---

    private void initParticles() {
        particles.clear();
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            particles.add(new Particle(random));
        }
    }

    private void adjustScale(double windowWidth) {
        if (logoImageView.getScene() == null) return;

        // Calculate responsive font size
        double rawSize = 12.0 * (windowWidth / 1280.0);
        double newSize = Math.clamp(rawSize, 10.0, 24.0);

        logoImageView.getScene().getRoot().setStyle(
                "-fx-font-size: " + String.format(Locale.US, "%.1f", newSize) + "px;"
        );
    }

    /**
     * Builds the transition sequence that hides the intro and shows the dashboard.
     */
    private ParallelTransition buildTransitionAnimation(Parent dashboardRoot) {
        // A. Fade Out Intro Elements
        FadeTransition ftCard = new FadeTransition(Duration.seconds(0.6), cardBox);
        ftCard.setToValue(0);

        FadeTransition ftHud = new FadeTransition(Duration.seconds(0.4), hudLayer);
        ftHud.setToValue(0);

        FadeTransition ftGlow = new FadeTransition(Duration.seconds(0.4), glowRegion);
        ftGlow.setToValue(0);

        // B. Fade Out Background (Slowly)
        FadeTransition ftGrid = new FadeTransition(Duration.seconds(1.5), gridRegion);
        ftGrid.setToValue(0);

        FadeTransition ftCanvas = new FadeTransition(Duration.seconds(1.5), codeCanvas);
        ftCanvas.setToValue(0);

        // C. Fade In Dashboard
        FadeTransition ftDash = new FadeTransition(Duration.seconds(1.2), dashboardRoot);
        ftDash.setToValue(1.0);

        ScaleTransition stDash = new ScaleTransition(Duration.seconds(1.2), dashboardRoot);
        stDash.setToX(1.0);
        stDash.setToY(1.0);

        ParallelTransition transition = new ParallelTransition(ftCard, ftHud, ftGlow, ftGrid, ftCanvas, ftDash, stDash);

        // Cleanup after animation
        transition.setOnFinished(ignored -> {
            if (animTimer != null) animTimer.stop();

            cardBox.setVisible(false);
            hudLayer.setVisible(false);
            glowRegion.setVisible(false);
            gridRegion.setVisible(false);
            codeCanvas.setVisible(false);
        });

        return transition;
    }

    // --- Inner Classes ---

    private static class Particle {
        double relX, relY, velX, velY, sizeBase;

        Particle(Random r) {
            this.relX = r.nextDouble();
            this.relY = r.nextDouble();
            this.velX = (r.nextDouble() - 0.5) * RELATIVE_SPEED;
            this.velY = (r.nextDouble() - 0.5) * RELATIVE_SPEED;
            this.sizeBase = 1.5 + r.nextDouble() * 2.0;
        }

        void update() {
            relX += velX;
            relY += velY;

            // Bounce off edges
            if (relX < 0) { relX = 0; velX *= -1; }
            if (relX > 1) { relX = 1; velX *= -1; }
            if (relY < 0) { relY = 0; velY *= -1; }
            if (relY > 1) { relY = 1; velY *= -1; }
        }
    }
}
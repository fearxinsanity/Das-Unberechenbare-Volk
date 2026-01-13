package de.schulprojekt.duv.view;

import de.schulprojekt.duv.controller.SimulationController;
import de.schulprojekt.duv.model.core.SimulationParameters;
import de.schulprojekt.duv.model.party.Party;
import de.schulprojekt.duv.model.scandal.ScandalEvent;
import de.schulprojekt.duv.model.voter.VoterTransition;
import de.schulprojekt.duv.view.components.CanvasRenderer;
import de.schulprojekt.duv.view.components.ChartManager;
import de.schulprojekt.duv.view.components.FeedManager;
import de.schulprojekt.duv.view.components.TooltipManager;
import de.schulprojekt.duv.view.util.VisualFX;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main controller for the simulation dashboard.
 */
public class DashboardController {

    private static final Logger LOGGER = Logger.getLogger(DashboardController.class.getName());

    // --- Constants ---
    private static final double MIN_SCANDAL_PROB = 0.0;
    private static final double MAX_SCANDAL_PROB = 60.0;

    // --- FXML: Layout & Containers ---
    @FXML private ScrollPane scandalTickerScroll;
    @FXML private HBox scandalTickerBox;
    @FXML private Pane animationPane;
    @FXML private Pane eventFeedPane;
    @FXML private VBox leftSidebar;
    @FXML private VBox rightSidebar;

    // -- Locking Containers (Content VBoxes) --
    @FXML private VBox populationBox;
    @FXML private VBox partyBox;
    @FXML private VBox budgetBox;
    @FXML private VBox durationBox;

    // -- Locking Overlays (Labels inside StackPanes) --
    @FXML private Label populationOverlay;
    @FXML private Label partyOverlay;
    @FXML private Label budgetOverlay;
    @FXML private Label durationOverlay;

    // --- FXML: Visualization ---
    @FXML private LineChart<Number, Number> historyChart;
    @FXML private Label timeStepLabel;

    // --- FXML: Controls & Inputs ---
    @FXML private TextField voterCountField;
    @FXML private TextField partyCountField;
    @FXML private TextField budgetField;
    @FXML private TextField scandalChanceField;

    @FXML private Slider mediaInfluenceSlider;
    @FXML private Slider mobilityRateSlider;
    @FXML private Slider loyaltyMeanSlider;
    @FXML private Slider randomRangeSlider;

    // -- Timer Input --
    @FXML private TextField durationField;

    // --- FXML: Buttons ---
    @FXML private Button startButton;
    @FXML private Button pauseButton;
    @FXML private Button resetButton;
    @FXML private Button intelButton;
    @FXML private Button parliamentButton;

    // --- Managers ---
    private SimulationController controller;
    private CanvasRenderer canvasRenderer;
    private ChartManager chartManager;
    private FeedManager feedManager;
    private TooltipManager tooltipManager;

    private int currentTick = 0;

    // --- Timer State ---
    private Timeline simulationTimer;
    private int configDurationSeconds = 30; // Standard 30s
    private int remainingSeconds = 30;

    // --- Text Storage for Locking ---
    private String originalIntelText;
    private String originalParliamentText;

    // --- Animation State ---
    private final Map<Node, FadeTransition> activeBlinks = new HashMap<>();

    // --- Initialization ---

    @FXML
    public void initialize() {
        this.canvasRenderer = new CanvasRenderer(animationPane);
        this.chartManager = new ChartManager(historyChart);
        this.feedManager = new FeedManager(scandalTickerBox, scandalTickerScroll, eventFeedPane);
        this.tooltipManager = new TooltipManager(animationPane);

        canvasRenderer.getCanvas().setOnMouseMoved(e ->
                tooltipManager.handleMouseMove(
                        e.getX(),
                        e.getY(),
                        controller.getParties(),
                        canvasRenderer.getPartyPositions(),
                        controller.getCurrentParameters().populationSize()
                )
        );
        canvasRenderer.getCanvas().setOnMouseExited(ignored -> tooltipManager.hideTooltip());

        this.controller = new SimulationController(this);

        synchronizeUiWithParameters(controller.getCurrentParameters());

        // Setup Timer
        updateDurationDisplay();
        setupTimer();

        canvasRenderer.startVisualTimer();
        updateStatusDisplay(false);

        setupResponsiveLayout();

        // Capture original text BEFORE locking
        if (intelButton != null) originalIntelText = intelButton.getText();
        if (parliamentButton != null) originalParliamentText = parliamentButton.getText();

        // Initial Locking (Intel & Parliament)
        lockResultButtons(true);

        Platform.runLater(() -> {
            if (controller != null) {
                updateDashboard(controller.getParties(), List.of(), null, 0);
            }
        });
    }

    private void setupTimer() {
        simulationTimer = new Timeline(new KeyFrame(Duration.seconds(1), _ -> {
            remainingSeconds--;
            updateStatusDisplay(true);

            // Wenn Zeit abgelaufen ist:
            if (remainingSeconds <= 0) {
                handlePauseSimulation(null); // Stop Simulation logic

                // UNLOCK RESULTS explicitly
                lockResultButtons(false);
                VisualFX.triggerSidebarGlitch(leftSidebar, rightSidebar);

                LOGGER.info("Simulation Mission Complete. Access Granted.");
            }
        }));
        simulationTimer.setCycleCount(Timeline.INDEFINITE);
    }

    private void lockResultButtons(boolean locked) {
        setButtonLockState(intelButton, locked, originalIntelText);
        setButtonLockState(parliamentButton, locked, originalParliamentText);
    }

    private void setButtonLockState(Button btn, boolean locked, String originalText) {
        if (btn != null) {
            btn.setDisable(locked);
            if (locked) {
                if (!btn.getStyleClass().contains("locked-button")) {
                    btn.getStyleClass().add("locked-button");
                }
                btn.setText("[ LOCKED ]");
            } else {
                btn.getStyleClass().remove("locked-button");
                if (originalText != null) btn.setText(originalText);
            }
        }
    }

    private void synchronizeUiWithParameters(SimulationParameters params) {
        voterCountField.setText(String.valueOf(params.populationSize()));
        partyCountField.setText(String.valueOf(params.partyCount()));

        scandalChanceField.setText(String.format(Locale.US, "%.1f", params.scandalProbability()));
        mediaInfluenceSlider.setValue(params.mediaInfluence());
        mobilityRateSlider.setValue(params.volatilityRate());
        loyaltyMeanSlider.setValue(params.loyaltyAverage());
        randomRangeSlider.setValue(params.chaosFactor());

        double displayBudget = params.budgetEffectiveness() * 500000.0;
        budgetField.setText(String.format(Locale.US, "%.0f", displayBudget));
    }

    private void setupResponsiveLayout() {
        Platform.runLater(() -> {
            if (animationPane.getScene() != null) {
                Scene scene = animationPane.getScene();
                scene.widthProperty().addListener((ignored, ignored2, newVal) ->
                        VisualFX.adjustResponsiveScale(scene, newVal.doubleValue())
                );
                VisualFX.adjustResponsiveScale(scene, scene.getWidth());

                if (leftSidebar != null) {
                    leftSidebar.prefWidthProperty().bind(Bindings.max(250, Bindings.min(450, scene.widthProperty().multiply(0.22))));
                }
                if (rightSidebar != null) {
                    rightSidebar.prefWidthProperty().bind(Bindings.max(250, Bindings.min(450, scene.widthProperty().multiply(0.22))));
                }
            }
        });
    }

    /**
     * Hauptmethode zum Sperren/Entsperren der Simulations-Parameter.
     */
    private void setSimulationLocked(boolean locked) {
        toggleBoxLockState(populationBox, populationOverlay, locked);
        toggleBoxLockState(partyBox, partyOverlay, locked);
        toggleBoxLockState(budgetBox, budgetOverlay, locked);
        toggleBoxLockState(durationBox, durationOverlay, locked);
    }

    /**
     * Setzt den Lock-Status für eine Box + Overlay.
     * Wendet das Styling auf das Parent (StackPane) an.
     */
    private void toggleBoxLockState(VBox box, Label overlay, boolean locked) {
        if (box == null) return;

        // Deaktiviere die Eingaben
        box.setDisable(locked);

        // Overlay sichtbar machen
        if (overlay != null) {
            overlay.setVisible(locked);
        }

        // Style auf das Parent (StackPane) anwenden, damit der Rahmen außen ist
        Parent container = box.getParent();
        if (container != null) {
            if (locked) {
                if (!container.getStyleClass().contains("locked-zone")) {
                    container.getStyleClass().add("locked-zone");
                }
                setBlinking(container, true);
            } else {
                container.getStyleClass().remove("locked-zone");
                setBlinking(container, false);
            }
        }
    }

    private void setBlinking(Node node, boolean blinking) {
        if (blinking) {
            if (!activeBlinks.containsKey(node)) {
                FadeTransition fade = new FadeTransition(Duration.seconds(0.8), node);
                fade.setFromValue(1.0);
                fade.setToValue(0.5);
                fade.setCycleCount(Animation.INDEFINITE);
                fade.setAutoReverse(true);
                fade.play();
                activeBlinks.put(node, fade);
            }
        } else {
            if (activeBlinks.containsKey(node)) {
                FadeTransition fade = activeBlinks.get(node);
                fade.stop();
                node.setOpacity(1.0);
                activeBlinks.remove(node);
            }
        }
    }

    // --- Core Update Loop ---

    public void updateDashboard(List<Party> parties, List<VoterTransition> transitions, ScandalEvent scandal, int step) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> updateDashboard(parties, transitions, scandal, step));
            return;
        }

        this.currentTick = step;

        if (step == 0) {
            chartManager.clear();
            canvasRenderer.clear(parties);
            feedManager.clear();
        }

        updateStatusDisplay(controller.isRunning());

        feedManager.processScandal(scandal, step);
        chartManager.update(parties, step);
        canvasRenderer.update(parties, transitions, controller.getCurrentParameters().populationSize());

        if (scandal != null) {
            VisualFX.triggerSidebarGlitch(leftSidebar, rightSidebar);
        }
    }

    private void updateStatusDisplay(boolean isRunning) {
        if (timeStepLabel == null) return;
        String statusText = isRunning ? "RUNNING" : "HALTED";
        String color = isRunning ? "#55ff55" : "#ff5555";

        int m = remainingSeconds / 60;
        int s = remainingSeconds % 60;
        String timeText = String.format("%02d:%02d", m, s);

        timeStepLabel.setText(String.format("STATUS: %s | TICK: %d | T-MINUS: %s", statusText, currentTick, timeText));
        timeStepLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-family: 'Consolas'; -fx-font-weight: bold;");
    }

    // --- Event Handlers ---

    @FXML
    public void handleStartSimulation(ActionEvent ignored) {
        if (controller != null) {
            if (remainingSeconds > 0) {
                controller.startSimulation();
                updateButtonState(true);

                if (simulationTimer.getStatus() != Animation.Status.RUNNING) {
                    simulationTimer.play();
                }
                setSimulationLocked(true);

                // Ergebnisse sperren
                lockResultButtons(true);

                updateStatusDisplay(true);
            }
        }
    }

    @FXML
    public void handlePauseSimulation(ActionEvent ignored) {
        if (controller != null) {
            controller.pauseSimulation();
            updateButtonState(false);

            simulationTimer.pause();

            updateStatusDisplay(false);
        }
    }

    @FXML
    public void handleResetSimulation(ActionEvent ignored) {
        if (controller != null) {
            controller.resetSimulation();
            updateButtonState(false);
            if (resetButton != null) resetButton.setDisable(true);
            this.currentTick = 0;

            simulationTimer.stop();
            remainingSeconds = configDurationSeconds;
            updateDurationDisplay();

            setSimulationLocked(false);

            // Ergebnisse wieder sperren
            lockResultButtons(true);

            updateStatusDisplay(false);
        }
    }

    // --- Helper Methods ---

    @FXML
    public void handleDurationIncrement(ActionEvent ignored) {
        if (configDurationSeconds < 300) {
            configDurationSeconds += 30;
            remainingSeconds = configDurationSeconds;
            updateDurationDisplay();
        }
    }

    @FXML
    public void handleDurationDecrement(ActionEvent ignored) {
        if (configDurationSeconds > 30) {
            configDurationSeconds -= 30;
            remainingSeconds = configDurationSeconds;
            updateDurationDisplay();
        }
    }

    private void updateDurationDisplay() {
        int m = configDurationSeconds / 60;
        int s = configDurationSeconds % 60;
        durationField.setText(String.format("%02d:%02d", m, s));
    }

    @FXML
    public void handleRandomize(ActionEvent ignored) {
        Random rand = new Random();

        int rPop = 10000 + rand.nextInt(490000);
        int rParties = 2 + rand.nextInt(7);
        double rMedia = rand.nextDouble() * 100.0;
        double rVolatility = rand.nextDouble() * 100.0;
        double rLoyalty = rand.nextDouble() * 100.0;
        double rBudget = 50000.0 + rand.nextDouble() * 1950000.0;
        double rScandal = rand.nextDouble() * 15.0;
        double rChaos = 0.1 + rand.nextDouble() * 2.9;

        voterCountField.setText(String.valueOf(rPop));
        partyCountField.setText(String.valueOf(rParties));
        mediaInfluenceSlider.setValue(rMedia);
        mobilityRateSlider.setValue(rVolatility);
        loyaltyMeanSlider.setValue(rLoyalty);
        budgetField.setText(String.format(Locale.US, "%.0f", rBudget));
        scandalChanceField.setText(String.format(Locale.US, "%.1f", rScandal));
        randomRangeSlider.setValue(rChaos);

        handleParameterChange(null);
    }

    @FXML
    public void handleParameterChange(Event ignored) {
        if (controller == null) return;
        try {
            int popSize = parseIntSafe(voterCountField.getText(), 100000);
            int parties = parseIntSafe(partyCountField.getText(), 5);
            parties = Math.clamp(parties, 2, 8);

            double scandalProb = Math.clamp(parseDoubleSafe(scandalChanceField.getText(), 5.0), MIN_SCANDAL_PROB, MAX_SCANDAL_PROB);

            double budgetInput = parseDoubleSafe(budgetField.getText(), 500000.0);
            double budgetEffectiveness = Math.clamp(budgetInput / 500000.0, 0.1, 10.0);

            SimulationParameters params = new SimulationParameters(
                    popSize,
                    mediaInfluenceSlider.getValue(),
                    mobilityRateSlider.getValue(),
                    scandalProb,
                    loyaltyMeanSlider.getValue(),
                    controller.getCurrentParameters().tickRate(),
                    randomRangeSlider.getValue(),
                    parties,
                    budgetEffectiveness
            );
            controller.updateAllParameters(params);

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Invalid parameter input", e);
        }
    }

    @FXML
    public void handleShowStatistics(ActionEvent ignored) {
        navigate("/de/schulprojekt/duv/view/StatisticsView.fxml", (loader, ignoredRoot) -> {
            StatisticsController statsCtrl = loader.getController();
            Parent dashboardRoot = startButton.getScene().getRoot();
            statsCtrl.initData(controller.getParties(), historyChart.getData(), this.currentTick, dashboardRoot);
        });
    }

    @FXML
    public void handleShowParliament(ActionEvent ignored) {
        navigate("/de/schulprojekt/duv/view/ParliamentView.fxml", (loader, ignoredRoot) -> {
            ParliamentController parliamentController = loader.getController();
            Parent dashboardView = startButton.getScene().getRoot();
            parliamentController.initData(controller.getParties(), dashboardView);
        });
    }

    private void navigate(String fxmlPath, java.util.function.BiConsumer<FXMLLoader, Parent> initAction) {
        if (controller == null) return;
        if (controller.isRunning()) handlePauseSimulation(null);

        try {
            var resource = getClass().getResource(fxmlPath);
            if (resource == null) {
                new Alert(Alert.AlertType.ERROR, "View not found: " + fxmlPath).show();
                return;
            }
            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();
            initAction.accept(loader, root);
            startButton.getScene().setRoot(root);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Navigation failed", e);
        }
    }

    @FXML public void handleVoterCountIncrement(ActionEvent ignored) { adjustIntField(voterCountField, 10000, 1000, 2000000); }
    @FXML public void handleVoterCountDecrement(ActionEvent ignored) { adjustIntField(voterCountField, -10000, 1000, 2000000); }
    @FXML public void handlePartyCountIncrement(ActionEvent ignored) { adjustIntField(partyCountField, 1, 2, 8); }
    @FXML public void handlePartyCountDecrement(ActionEvent ignored) { adjustIntField(partyCountField, -1, 2, 8); }
    @FXML public void handleScandalChanceIncrement(ActionEvent ignored) { adjustDoubleField(scandalChanceField, 0.5); }
    @FXML public void handleScandalChanceDecrement(ActionEvent ignored) { adjustDoubleField(scandalChanceField, -0.5); }

    @FXML public void handleSpeed1x(ActionEvent ignored) { controller.updateSimulationSpeed(1); }
    @FXML public void handleSpeed2x(ActionEvent ignored) { controller.updateSimulationSpeed(2); }
    @FXML public void handleSpeed4x(ActionEvent ignored) { controller.updateSimulationSpeed(4); }

    private void adjustIntField(TextField f, int delta, int min, int max) {
        int val = parseIntSafe(f.getText(), min);
        f.setText(String.valueOf(Math.clamp(val + delta, min, max)));
        handleParameterChange(null);
    }

    private void adjustDoubleField(TextField f, double delta) {
        double val = parseDoubleSafe(f.getText(), 0.0);
        f.setText(String.format(Locale.US, "%.1f", Math.clamp(val + delta, MIN_SCANDAL_PROB, MAX_SCANDAL_PROB)));
        handleParameterChange(null);
    }

    private int parseIntSafe(String t, int def) {
        try { return Integer.parseInt(t.replaceAll("[^0-9]", "")); } catch (Exception e) { return def; }
    }

    private double parseDoubleSafe(String t, double def) {
        try { return Double.parseDouble(t.replace(",", ".")); } catch (Exception e) { return def; }
    }

    private void updateButtonState(boolean isRunning) {
        if (startButton != null) startButton.setDisable(isRunning);
        if (pauseButton != null) pauseButton.setDisable(!isRunning);
        if (resetButton != null) resetButton.setDisable(isRunning);
    }

    public void shutdown() {
        if (controller != null) controller.shutdown();
        if (canvasRenderer != null) canvasRenderer.stop();
        if (simulationTimer != null) simulationTimer.stop();
    }
}
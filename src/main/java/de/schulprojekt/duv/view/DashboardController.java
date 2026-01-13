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
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
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

    // --- FXML: Buttons ---
    @FXML private Button startButton;
    @FXML private Button pauseButton;
    @FXML private Button resetButton;

    // --- Managers ---
    private SimulationController controller;
    private CanvasRenderer canvasRenderer;
    private ChartManager chartManager;
    private FeedManager feedManager;
    private TooltipManager tooltipManager;

    private int currentTick = 0;

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
                        controller.getCurrentParameters().populationSize() // REF: Record Access
                )
        );
        canvasRenderer.getCanvas().setOnMouseExited(ignored -> tooltipManager.hideTooltip());

        this.controller = new SimulationController(this);

        synchronizeUiWithParameters(controller.getCurrentParameters());

        canvasRenderer.startVisualTimer();
        updateStatusDisplay(false);

        setupResponsiveLayout();

        Platform.runLater(() -> {
            if (controller != null) {
                updateDashboard(controller.getParties(), List.of(), null, 0);
            }
        });
    }

    private void synchronizeUiWithParameters(SimulationParameters params) {
        voterCountField.setText(String.valueOf(params.populationSize())); // REF
        partyCountField.setText(String.valueOf(params.partyCount()));     // REF

        scandalChanceField.setText(String.format(Locale.US, "%.1f", params.scandalProbability())); // REF
        mediaInfluenceSlider.setValue(params.mediaInfluence());       // REF
        mobilityRateSlider.setValue(params.volatilityRate());         // REF
        loyaltyMeanSlider.setValue(params.loyaltyAverage());          // REF
        randomRangeSlider.setValue(params.chaosFactor());             // REF

        double displayBudget = params.budgetEffectiveness() * 500000.0; // REF
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
        canvasRenderer.update(parties, transitions, controller.getCurrentParameters().populationSize()); // REF

        if (scandal != null) {
            VisualFX.triggerSidebarGlitch(leftSidebar, rightSidebar);
        }
    }

    private void updateStatusDisplay(boolean isRunning) {
        if (timeStepLabel == null) return;
        String statusText = isRunning ? "RUNNING" : "HALTED";
        String color = isRunning ? "#55ff55" : "#ff5555";
        timeStepLabel.setText(String.format("SYSTEM_STATUS: %s | TICK: %d", statusText, currentTick));
        timeStepLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-family: 'Consolas'; -fx-font-weight: bold;");
    }

    // --- Event Handlers ---

    @FXML
    public void handleStartSimulation(ActionEvent ignored) {
        if (controller != null) {
            controller.startSimulation();
            updateButtonState(true);
            updateStatusDisplay(true);
        }
    }

    @FXML
    public void handlePauseSimulation(ActionEvent ignored) {
        if (controller != null) {
            controller.pauseSimulation();
            updateButtonState(false);
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
            updateStatusDisplay(false);
        }
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
                    controller.getCurrentParameters().tickRate(), // REF
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

    // --- Helper Methods ---

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
    }
}
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
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.io.IOException;
import java.text.NumberFormat;
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

    // Limits
    private static final int MIN_POPULATION = 10_000;
    private static final int MAX_POPULATION = 500_000;
    private static final double MAX_BUDGET_FACTOR = 1000.0;

    // --- FXML: Layout & Containers ---
    @FXML private ScrollPane scandalTickerScroll;
    @FXML private HBox scandalTickerBox;
    @FXML private Pane animationPane;
    @FXML private Pane eventFeedPane;
    @FXML private VBox leftSidebar;
    @FXML private VBox rightSidebar;

    // -- Locking Containers ---
    @FXML private VBox populationBox;
    @FXML private VBox partyBox;
    @FXML private VBox budgetBox;
    @FXML private VBox durationBox;

    // -- Locking Overlays ---
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

        // Tooltip setup
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

        // Setup Fields
        setupInteractiveField(voterCountField);
        setupInteractiveField(partyCountField);
        setupInteractiveField(budgetField);
        setupInteractiveField(scandalChanceField);

        synchronizeUiWithParameters(controller.getCurrentParameters());

        // Setup Timer
        updateDurationDisplay();
        setupTimer();

        canvasRenderer.startVisualTimer();
        updateStatusDisplay(false);

        setupResponsiveLayout();

        // Capture original text BEFORE locking
        if (intelButton != null) {
            originalIntelText = intelButton.getText();
            intelButton.setOnMouseEntered(e -> VisualFX.stopPulse(intelButton));
        }
        if (parliamentButton != null) {
            originalParliamentText = parliamentButton.getText();
            parliamentButton.setOnMouseEntered(e -> VisualFX.stopPulse(parliamentButton));
        }

        // Initial Locking (Intel & Parliament)
        lockResultButtons(true);

        Platform.runLater(() -> {
            if (controller != null) {
                updateDashboard(controller.getParties(), List.of(), null, 0);
            }
        });
    }

    private void setupInteractiveField(TextField field) {
        if (field == null) return;

        applyInputFilter(field);

        field.setOnAction(e -> {
            formatAndApply(field);
            animationPane.requestFocus();
        });

        field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) { // Focus lost
                formatAndApply(field);
            }
        });

        field.setOnKeyPressed(e -> field.setStyle(""));
    }

    private void applyInputFilter(TextField field) {
        if (field == null) return;
        boolean isDecimal = (field == scandalChanceField);
        String regex = isDecimal ? "[0-9.,]*" : "[0-9.]*";

        field.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.matches(regex)) {
                return change;
            }
            return null;
        }));
    }

    private void removeInputFilter(TextField field) {
        if (field != null) {
            field.setTextFormatter(null);
        }
    }

    private void formatAndApply(TextField field) {
        String text = field.getText();
        if (text == null || text.isEmpty()) return;

        try {
            boolean isDecimal = (field == scandalChanceField);

            if (isDecimal) {
                double val = parseDoubleSafe(text, 0.0);
                field.setText(String.format(Locale.US, "%.1f", val));
            } else {
                long val = parseLongSafe(text);

                if (field == voterCountField) {
                    val = Math.clamp(val, MIN_POPULATION, MAX_POPULATION);
                }

                NumberFormat formatter = NumberFormat.getInstance(Locale.GERMANY); // Format with dots (DE style often used in EU tech)
                field.setText(formatter.format(val));
            }

            handleParameterChange(null);

        } catch (Exception e) {
            field.setStyle("-fx-border-color: red;");
        }
    }

    private long parseLongSafe(String text) {
        String clean = text.replaceAll("[^0-9]", "");
        return clean.isEmpty() ? 0 : Long.parseLong(clean);
    }

    public void applyInitialSettings(long population, long budget) {
        if (voterCountField != null) {
            long safePop = Math.clamp(population, MIN_POPULATION, MAX_POPULATION);
            voterCountField.setText(String.format(Locale.GERMANY, "%,d", safePop));
        }
        if (budgetField != null) {
            budgetField.setText(String.format(Locale.GERMANY, "%,d", budget));
        }
        handleParameterChange(null);
    }

    private void setupTimer() {
        simulationTimer = new Timeline(new KeyFrame(Duration.seconds(1), ignored -> {
            remainingSeconds--;
            updateStatusDisplay(true);

            if (remainingSeconds <= 0) {
                handlePauseSimulation(null);
                lockResultButtons(false);
                VisualFX.triggerSidebarGlitch(leftSidebar, rightSidebar);
                VisualFX.startPulse(intelButton, Color.LIME);
                VisualFX.startPulse(parliamentButton, Color.LIME);
                LOGGER.info("Simulation finished. Access granted.");
            }
        }));
        simulationTimer.setCycleCount(Timeline.INDEFINITE);
    }

    private void lockResultButtons(boolean locked) {
        setButtonLockState(intelButton, locked, originalIntelText);
        setButtonLockState(parliamentButton, locked, originalParliamentText);
        if (locked) {
            if (intelButton != null) VisualFX.stopPulse(intelButton);
            if (parliamentButton != null) VisualFX.stopPulse(parliamentButton);
        }
    }

    private void setButtonLockState(Button btn, boolean locked, String originalText) {
        if (btn != null) {
            btn.setDisable(locked);
            if (locked) {
                if (!btn.getStyleClass().contains("locked-button")) {
                    btn.getStyleClass().add("locked-button");
                }
                btn.setText("[ LOCKED ]"); // English
            } else {
                btn.getStyleClass().remove("locked-button");
                if (originalText != null) btn.setText(originalText);
            }
        }
    }

    private void synchronizeUiWithParameters(SimulationParameters params) {
        voterCountField.setText(String.format(Locale.GERMANY, "%,d", params.populationSize()));
        partyCountField.setText(String.valueOf(params.partyCount()));
        scandalChanceField.setText(String.format(Locale.US, "%.1f", params.scandalProbability()));
        mediaInfluenceSlider.setValue(params.mediaInfluence());
        mobilityRateSlider.setValue(params.volatilityRate());
        loyaltyMeanSlider.setValue(params.loyaltyAverage());
        randomRangeSlider.setValue(params.chaosFactor());
        double displayBudget = params.budgetEffectiveness() * 500000.0;
        budgetField.setText(String.format(Locale.GERMANY, "%,.0f", displayBudget));
    }

    private void setupResponsiveLayout() {
        Platform.runLater(() -> {
            if (animationPane.getScene() != null) {
                Scene scene = animationPane.getScene();
                scene.widthProperty().addListener((ignored, ignored2, newVal) ->
                        VisualFX.adjustResponsiveScale(scene, newVal.doubleValue())
                );
                VisualFX.adjustResponsiveScale(scene, scene.getWidth());
                if (leftSidebar != null) leftSidebar.prefWidthProperty().bind(Bindings.max(250, Bindings.min(450, scene.widthProperty().multiply(0.22))));
                if (rightSidebar != null) rightSidebar.prefWidthProperty().bind(Bindings.max(250, Bindings.min(450, scene.widthProperty().multiply(0.22))));
            }
        });
    }

    private void setSimulationLocked(boolean locked) {
        toggleBoxLockState(populationBox, populationOverlay, locked);
        toggleBoxLockState(partyBox, partyOverlay, locked);
        toggleBoxLockState(budgetBox, budgetOverlay, locked);
        toggleBoxLockState(durationBox, durationOverlay, locked);
    }

    private void toggleBoxLockState(VBox box, Label overlay, boolean locked) {
        if (box == null) return;
        box.setDisable(locked);
        if (overlay != null) overlay.setVisible(locked);
        Parent container = box.getParent();
        if (container != null) {
            if (locked) {
                if (!container.getStyleClass().contains("locked-zone")) container.getStyleClass().add("locked-zone");
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
                fade.setFromValue(1.0); fade.setToValue(0.5);
                fade.setCycleCount(Animation.INDEFINITE); fade.setAutoReverse(true);
                fade.play(); activeBlinks.put(node, fade);
            }
        } else {
            if (activeBlinks.containsKey(node)) {
                FadeTransition fade = activeBlinks.get(node);
                fade.stop(); node.setOpacity(1.0); activeBlinks.remove(node);
            }
        }
    }

    public void updateDashboard(List<Party> parties, List<VoterTransition> transitions, ScandalEvent scandal, int step) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> updateDashboard(parties, transitions, scandal, step));
            return;
        }
        this.currentTick = step;
        if (step == 0) {
            chartManager.clear(); canvasRenderer.clear(parties); feedManager.clear();
        }
        updateStatusDisplay(controller.isRunning());
        feedManager.processScandal(scandal, step);
        chartManager.update(parties, step);
        canvasRenderer.update(parties, transitions, controller.getCurrentParameters().populationSize());
        if (scandal != null) VisualFX.triggerSidebarGlitch(leftSidebar, rightSidebar);
    }

    private void updateStatusDisplay(boolean isRunning) {
        if (timeStepLabel == null) return;

        // --- TRANSLATION: English ---
        String statusText = isRunning ? "RUNNING" : "PAUSED";
        String color = isRunning ? "#55ff55" : "#ff5555";

        int m = remainingSeconds / 60; int s = remainingSeconds % 60;
        String timeText = String.format("%02d:%02d", m, s);
        timeStepLabel.setText(String.format("STATUS: %s | TICK: %d | T-MINUS: %s", statusText, currentTick, timeText));
        timeStepLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-family: 'Consolas'; -fx-font-weight: bold;");
    }

    @FXML
    public void handleStartSimulation(ActionEvent ignored) {
        if (controller != null) {
            if (remainingSeconds > 0) {
                handleParameterChange(null);
                controller.startSimulation();
                updateButtonState(true);
                if (simulationTimer.getStatus() != Animation.Status.RUNNING) simulationTimer.play();
                setSimulationLocked(true); lockResultButtons(true); updateStatusDisplay(true);
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
            setSimulationLocked(false); lockResultButtons(true); updateStatusDisplay(false);
        }
    }

    @FXML
    public void handleLogout(ActionEvent ignored) {
        if (controller != null && controller.isRunning()) handlePauseSimulation(null);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);

        // --- TRANSLATION: English ---
        alert.setTitle("SYSTEM ABORT");
        alert.setHeaderText("TERMINATE SESSION");
        alert.setContentText("Are you sure you want to terminate the secure connection?");

        try {
            if (startButton != null && startButton.getScene() != null) {
                alert.getDialogPane().getStylesheets().addAll(startButton.getScene().getStylesheets());
                alert.getDialogPane().getStyleClass().add("alert-dialog");
            }
        } catch (Exception e) { /* Ignore CSS errors */ }
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            shutdown(); Platform.exit(); System.exit(0);
        }
    }

    @FXML
    public void handleDurationIncrement(ActionEvent ignored) {
        if (configDurationSeconds < 300) {
            configDurationSeconds += 30; remainingSeconds = configDurationSeconds; updateDurationDisplay();
        }
    }

    @FXML
    public void handleDurationDecrement(ActionEvent ignored) {
        if (configDurationSeconds > 30) {
            configDurationSeconds -= 30; remainingSeconds = configDurationSeconds; updateDurationDisplay();
        }
    }

    private void updateDurationDisplay() {
        int m = configDurationSeconds / 60; int s = configDurationSeconds % 60;
        durationField.setText(String.format("%02d:%02d", m, s));
    }

    @FXML
    public void handleRandomize(ActionEvent ignored) {
        removeInputFilter(voterCountField);
        removeInputFilter(partyCountField);
        removeInputFilter(budgetField);
        removeInputFilter(scandalChanceField);

        Random rand = new Random();
        int rPop = 10000 + rand.nextInt(MAX_POPULATION - 10000);
        int rParties = 2 + rand.nextInt(7);
        double rMedia = rand.nextDouble() * 100.0;
        double rVolatility = rand.nextDouble() * 100.0;
        double rLoyalty = rand.nextDouble() * 100.0;
        double rBudget = 50000.0 + rand.nextDouble() * 1950000.0;
        double rScandal = rand.nextDouble() * 15.0;
        double rChaos = 0.1 + rand.nextDouble() * 2.9;

        VisualFX.animateDecryption(voterCountField, String.format(Locale.GERMANY, "%,d", rPop));
        VisualFX.animateDecryption(partyCountField, String.valueOf(rParties));
        VisualFX.animateDecryption(budgetField, String.format(Locale.GERMANY, "%,.0f", rBudget));
        Timeline lastAnim = VisualFX.animateDecryption(scandalChanceField, String.format(Locale.US, "%.1f", rScandal));

        mediaInfluenceSlider.setValue(rMedia);
        mobilityRateSlider.setValue(rVolatility);
        loyaltyMeanSlider.setValue(rLoyalty);
        randomRangeSlider.setValue(rChaos);

        if (lastAnim != null) {
            lastAnim.setOnFinished(e -> {
                applyInputFilter(voterCountField);
                applyInputFilter(partyCountField);
                applyInputFilter(budgetField);
                applyInputFilter(scandalChanceField);
                handleParameterChange(null);
            });
        } else {
            applyInputFilter(voterCountField);
            applyInputFilter(partyCountField);
            applyInputFilter(budgetField);
            applyInputFilter(scandalChanceField);
            handleParameterChange(null);
        }
    }

    @FXML
    public void handleParameterChange(Event ignored) {
        if (controller == null) return;
        try {
            int popSize = parseIntSafe(voterCountField.getText(), 100000);
            popSize = Math.clamp(popSize, MIN_POPULATION, MAX_POPULATION);

            int parties = parseIntSafe(partyCountField.getText(), 5);
            parties = Math.clamp(parties, 2, 8);

            double scandalProb = Math.clamp(parseDoubleSafe(scandalChanceField.getText(), 5.0), MIN_SCANDAL_PROB, MAX_SCANDAL_PROB);
            double budgetInput = parseBudgetSafe(budgetField.getText());
            double budgetEffectiveness = Math.clamp(budgetInput / 500000.0, 0.1, MAX_BUDGET_FACTOR);

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

    @FXML public void handleVoterCountIncrement(ActionEvent ignored) { adjustIntField(voterCountField, 10000, MIN_POPULATION, MAX_POPULATION); }
    @FXML public void handleVoterCountDecrement(ActionEvent ignored) { adjustIntField(voterCountField, -10000, MIN_POPULATION, MAX_POPULATION); }
    @FXML public void handlePartyCountIncrement(ActionEvent ignored) { adjustIntField(partyCountField, 1, 2, 8); }
    @FXML public void handlePartyCountDecrement(ActionEvent ignored) { adjustIntField(partyCountField, -1, 2, 8); }
    @FXML public void handleScandalChanceIncrement(ActionEvent ignored) { adjustDoubleField(scandalChanceField, 0.5); }
    @FXML public void handleScandalChanceDecrement(ActionEvent ignored) { adjustDoubleField(scandalChanceField, -0.5); }

    @FXML public void handleSpeed1x(ActionEvent ignored) { controller.updateSimulationSpeed(1); }
    @FXML public void handleSpeed2x(ActionEvent ignored) { controller.updateSimulationSpeed(2); }
    @FXML public void handleSpeed4x(ActionEvent ignored) { controller.updateSimulationSpeed(4); }

    private void adjustIntField(TextField f, int delta, int min, int max) {
        int val = parseIntSafe(f.getText(), min);
        f.setText(String.format(Locale.GERMANY, "%,d", Math.clamp(val + delta, min, max)));
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

    private double parseBudgetSafe(String t) {
        try {
            String clean = t.replace(".", "").replace(",", ".");
            return Double.parseDouble(clean);
        } catch (Exception e) {
            return 500000.0;
        }
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
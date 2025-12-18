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
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.chart.LineChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DashboardController {

    // --- FXML IDs ---
    @FXML private Label timeStepLabel;
    @FXML private Button startButton;
    @FXML private Button pauseButton;
    @FXML private Button resetButton;

    // Konfiguration
    @FXML private TextField voterCountField;
    @FXML private TextField partyCountField;
    @FXML private Slider mediaInfluenceSlider;
    @FXML private Slider mobilityRateSlider;
    @FXML private Slider loyaltyMeanSlider;
    @FXML private TextField budgetField;
    @FXML private TextField scandalChanceField;
    @FXML private Slider randomRangeSlider;

    // Visualisierung
    @FXML private ScrollPane scandalTickerScroll;
    @FXML private HBox scandalTickerBox;
    @FXML private Pane eventFeedPane;
    @FXML private LineChart<Number, Number> historyChart;
    @FXML private Pane animationPane;

    // --- Interne Manager ---
    private CanvasRenderer canvasRenderer;
    private FeedManager feedManager;
    private ChartManager chartManager;
    private TooltipManager tooltipManager;

    private SimulationController controller;
    private AnimationTimer visualTimer;
    private boolean isRunning = false;

    private int currentSpeed = 4;
    private int currentStep = 0;

    // UI-Kopie der Parteien
    private List<Party> uiParties = new ArrayList<>();

    @FXML
    public void initialize() {
        // 1. Canvas Setup
        Canvas canvas = new Canvas(800, 600);
        animationPane.getChildren().add(canvas);
        canvas.widthProperty().bind(animationPane.widthProperty());
        canvas.heightProperty().bind(animationPane.heightProperty());

        // 2. Manager setup
        this.canvasRenderer = new CanvasRenderer(canvas);
        this.feedManager = new FeedManager(eventFeedPane, scandalTickerBox, scandalTickerScroll);
        this.chartManager = new ChartManager(historyChart);
        this.tooltipManager = new TooltipManager(animationPane, canvasRenderer);

        // 3. Controller starten
        this.controller = new SimulationController(this);

        // 4. Initialisierung
        currentStep = 0;

        // WICHTIG: Sofort Daten laden
        updateUiPartiesFromController();

        // Parameter initial anwenden
        handleParameterChange();

        updateButtonState();
        startVisualLoop();
    }

    // =========================================================
    //               EVENT HANDLER
    // =========================================================

    @FXML
    public void handleStartSimulation() {
        applyParameters();
        controller.startSimulation();
        isRunning = true;
        updateButtonState();
    }

    @FXML
    public void handlePauseSimulation() {
        controller.pauseSimulation();
        isRunning = false;
        updateButtonState();
    }

    @FXML
    public void handleResetSimulation() {
        handleParameterChange();
        controller.resetSimulation();
        isRunning = false;
        currentStep = 0;

        clearVisuals();
        updateUiPartiesFromController();

        timeStepLabel.setText("Status: Reset");
        updateButtonState();
    }

    private void clearVisuals() {
        feedManager.clear();
        chartManager.clear();
        canvasRenderer.clearParticles();
    }

    @FXML
    public void handleParameterChange(Event event) {
        if (controller == null) return;
        applyParameters();
    }

    @FXML public void handleParameterChange() { applyParameters(); }

    private void applyParameters() {
        try {
            int voters = parseIntSafe(voterCountField.getText(), 100000);
            int parties = parseIntSafe(partyCountField.getText(), 5);
            parties = Math.max(2, Math.min(8, parties));

            double scandalChance = parseDoubleSafe(scandalChanceField.getText(), 5.0);
            scandalChance = Math.max(0.0, Math.min(60.0, scandalChance));

            double budgetInput = parseDoubleSafe(budgetField.getText(), 500000.0);
            double budgetFactor = budgetInput / 500000.0;
            budgetFactor = Math.max(0.1, Math.min(10.0, budgetFactor));

            // KORREKTUR: Reihenfolge an Main-Branch angepasst!
            // (Parteien und Budget kommen VOR Speed und Variance)
            SimulationParameters params = new SimulationParameters(
                    voters,
                    mediaInfluenceSlider.getValue(),
                    mobilityRateSlider.getValue(),
                    scandalChance,
                    loyaltyMeanSlider.getValue(),
                    parties,                        // HIER war der Fehler (vorher Speed)
                    budgetFactor,                   // HIER war der Fehler
                    currentSpeed,                   // Speed kommt danach
                    randomRangeSlider.getValue()    // Variance am Ende
            );

            controller.updateAllParameters(params);

            // Live-Update bei Änderung (wenn pausiert)
            if (!isRunning) {
                updateUiPartiesFromController();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateUiPartiesFromController() {
        if (controller != null && controller.getParties() != null) {
            this.uiParties = new ArrayList<>(controller.getParties());
        }
    }

    // --- Helper für +/- Buttons ---
    @FXML public void handleVoterCountIncrement() { adjustIntField(voterCountField, 10000, 1000, 2000000); }
    @FXML public void handleVoterCountDecrement() { adjustIntField(voterCountField, -10000, 1000, 2000000); }
    @FXML public void handlePartyCountIncrement() { adjustIntField(partyCountField, 1, 2, 8); }
    @FXML public void handlePartyCountDecrement() { adjustIntField(partyCountField, -1, 2, 8); }

    @FXML public void handleScandalChanceIncrement() { adjustDoubleField(scandalChanceField, 0.5, 0.0, 60.0); }
    @FXML public void handleScandalChanceDecrement() { adjustDoubleField(scandalChanceField, -0.5, 0.0, 60.0); }

    // --- Geschwindigkeit ---
    @FXML public void handleSpeed1x() { setSpeed(2); }
    @FXML public void handleSpeed2x() { setSpeed(10); }
    @FXML public void handleSpeed4x() { setSpeed(50); }

    private void setSpeed(int tps) {
        this.currentSpeed = tps;
        controller.updateSimulationSpeed(tps);
    }

    // =========================================================
    //               UPDATE LOOP
    // =========================================================

    public void updateDashboard(List<Party> parties, List<VoterTransition> transitions, ScandalEvent scandal, int step) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> updateDashboard(parties, transitions, scandal, step));
            return;
        }

        if (parties != null) this.uiParties = new ArrayList<>(parties);
        this.currentStep = step;

        if (step == 0) clearVisuals();

        timeStepLabel.setText("Woche: " + step + (isRunning ? " (Läuft)" : " (Pausiert)"));

        if (scandal != null) feedManager.addScandal(scandal, step);
        if (step % 5 == 0) chartManager.update(this.uiParties, step);

        // Renderer kümmert sich um die Darstellung
        canvasRenderer.spawnParticles(transitions);
        tooltipManager.updateData(this.uiParties);

        updateButtonState();
    }

    public void shutdown() {
        if (visualTimer != null) visualTimer.stop();
        if (controller != null) controller.shutdown();
    }

    private void startVisualLoop() {
        visualTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!uiParties.isEmpty()) {
                    int total = parseIntSafe(voterCountField.getText(), 100000);
                    canvasRenderer.render(uiParties, total);
                }
            }
        };
        visualTimer.start();
    }

    private int parseIntSafe(String text, int def) {
        try { return Integer.parseInt(text.replaceAll("[^0-9]", "")); }
        catch (Exception e) { return def; }
    }

    private double parseDoubleSafe(String text, double def) {
        try { return Double.parseDouble(text.replace(",", ".")); }
        catch (Exception e) { return def; }
    }

    private void adjustIntField(TextField field, int delta, int min, int max) {
        int val = parseIntSafe(field.getText(), min);
        val = Math.max(min, Math.min(max, val + delta));
        field.setText(String.valueOf(val));
        applyParameters();
    }

    private void adjustDoubleField(TextField field, double delta, double min, double max) {
        double val = parseDoubleSafe(field.getText(), min);
        val = Math.max(min, Math.min(max, val + delta));
        field.setText(String.format(Locale.US, "%.1f", val));
        applyParameters();
    }

    private void updateButtonState() {
        if (startButton != null) startButton.setDisable(isRunning);
        if (pauseButton != null) pauseButton.setDisable(!isRunning);
        if (resetButton != null) resetButton.setDisable(isRunning || currentStep == 0);
    }
}
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

import java.util.List;

public class DashboardController {

    // --- FXML IDs (müssen exakt zur .fxml Datei passen) ---
    @FXML private Label timeStepLabel; // In FXML heißt es "timeStepLabel"
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
    private int currentSpeed = 4; // Standard TPS (Ticks per Second)

    @FXML
    public void initialize() {
        // 1. Canvas initialisieren
        Canvas canvas = new Canvas(800, 600);
        animationPane.getChildren().add(canvas);
        canvas.widthProperty().bind(animationPane.widthProperty());
        canvas.heightProperty().bind(animationPane.heightProperty());

        // 2. Manager setup
        this.canvasRenderer = new CanvasRenderer(canvas);
        this.feedManager = new FeedManager(eventFeedPane, scandalTickerBox, scandalTickerScroll);
        this.chartManager = new ChartManager(historyChart);
        this.tooltipManager = new TooltipManager(canvas, canvasRenderer); // Tooltip auf Canvas binden

        // 3. Resize Listener
        animationPane.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (controller != null) canvasRenderer.recalculatePositions(controller.getParties());
        });

        // 4. Controller starten
        this.controller = new SimulationController(this);

        // Initiale Button-Status setzen
        updateUIState();

        // 5. Visuellen Loop starten
        startVisualLoop();
    }

    // =========================================================
    //               FXML EVENT HANDLER
    // =========================================================

    @FXML
    public void handleStartSimulation() {
        // Parameter einlesen bevor es losgeht
        applyParameters();
        controller.startSimulation();
        isRunning = true;
        updateUIState();
    }

    @FXML
    public void handlePauseSimulation() {
        controller.pauseSimulation();
        isRunning = false;
        updateUIState();
    }

    @FXML
    public void handleResetSimulation() {
        controller.resetSimulation();
        isRunning = false;
        // UI zurücksetzen
        feedManager.clear();
        chartManager.clear();
        canvasRenderer.clearParticles();
        timeStepLabel.setText("Status: Reset");
        updateUIState();
    }

    /**
     * Wird von allen Inputs (Slider, Textfelder) aufgerufen,
     * wenn sich ein Wert ändert.
     */
    @FXML
    public void handleParameterChange(Event event) {
        // Wir wenden die Parameter sofort an (Live-Update)
        applyParameters();
    }

    // Überladung, falls FXML die Methode ohne Argumente sucht
    @FXML
    public void handleParameterChange() {
        applyParameters();
    }

    // --- Helper für +/- Buttons ---

    @FXML
    public void handleVoterCountIncrement() {
        adjustTextFieldInteger(voterCountField, 1000);
        applyParameters();
    }

    @FXML
    public void handleVoterCountDecrement() {
        adjustTextFieldInteger(voterCountField, -1000);
        applyParameters();
    }

    @FXML
    public void handlePartyCountIncrement() {
        adjustTextFieldInteger(partyCountField, 1);
        applyParameters();
    }

    @FXML
    public void handlePartyCountDecrement() {
        adjustTextFieldInteger(partyCountField, -1);
        applyParameters();
    }

    @FXML
    public void handleScandalChanceIncrement() {
        adjustTextFieldDouble(scandalChanceField, 0.5);
        applyParameters();
    }

    @FXML
    public void handleScandalChanceDecrement() {
        adjustTextFieldDouble(scandalChanceField, -0.5);
        applyParameters();
    }

    // --- Geschwindigkeit ---

    @FXML
    public void handleSpeed1x() { setSpeed(2); }

    @FXML
    public void handleSpeed2x() { setSpeed(10); }

    @FXML
    public void handleSpeed4x() { setSpeed(50); } // Turbo

    private void setSpeed(int tps) {
        this.currentSpeed = tps;
        controller.updateSimulationSpeed(tps);
    }

    // =========================================================
    //               LOGIK & UPDATE
    // =========================================================

    private void updateUIState() {
        startButton.setDisable(isRunning);
        pauseButton.setDisable(!isRunning);
        resetButton.setDisable(isRunning); // Reset nur möglich wenn pausiert

        // Konfiguration sperren während Simulation läuft?
        // Optional. Hier lassen wir es offen für Live-Tuning.
        partyCountField.setDisable(isRunning); // Parteianzahl ändern im laufenden Betrieb ist heikel
        voterCountField.setDisable(isRunning);
    }

    private void applyParameters() {
        try {
            SimulationParameters params = readParametersFromUI();
            // Simulations-Speed beibehalten
            params.setSimulationTicksPerSecond(currentSpeed);
            controller.updateAllParameters(params);
        } catch (NumberFormatException e) {
            // Ignorieren solange User tippt (ungültige Zahl)
        }
    }

    private SimulationParameters readParametersFromUI() {
        int voters = Integer.parseInt(voterCountField.getText());
        int parties = Integer.parseInt(partyCountField.getText());
        double media = mediaInfluenceSlider.getValue();
        double mobility = mobilityRateSlider.getValue();
        double loyalty = loyaltyMeanSlider.getValue();
        double scandalChance = Double.parseDouble(scandalChanceField.getText());
        double randomVar = randomRangeSlider.getValue();

        // Budget lesen wir als Double (ggf. Multiplikator oder Absolutwert)
        double budget = 1.0;
        try {
            budget = Double.parseDouble(budgetField.getText());
        } catch (Exception e) { /* fallback 1.0 */ }

        // Parameter Objekt erstellen (Reihenfolge muss zum Konstruktor passen!)
        // Constructor: (voters, media, mobility, scandalProb, loyalty, partyCount, budget, tps, variance)
        return new SimulationParameters(
                voters,
                media,
                mobility,
                scandalChance,
                loyalty,
                parties,
                budget,
                currentSpeed,
                randomVar
        );
    }

    /**
     * Wird vom SimulationController aufgerufen (Callback).
     */
    public void updateDashboard(List<Party> parties, List<VoterTransition> transitions, ScandalEvent scandal, int step) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> updateDashboard(parties, transitions, scandal, step));
            return;
        }

        // Status Label
        timeStepLabel.setText("Woche: " + step + (isRunning ? " (Läuft)" : " (Pausiert)"));

        // Delegation an Sub-Manager
        if (scandal != null) feedManager.addScandal(scandal, step);
        if (step % 5 == 0) chartManager.update(parties, step);

        // Canvas Data Update
        if (canvasRenderer.getPartyPosition(parties.get(0).getName()) == null) {
            canvasRenderer.recalculatePositions(parties);
        }
        canvasRenderer.spawnParticles(transitions);
        tooltipManager.updateData(parties);
    }

    public void shutdown() {
        if (visualTimer != null) visualTimer.stop();
        if (controller != null) controller.shutdown();
    }

    private void startVisualLoop() {
        visualTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (controller != null && controller.getParties() != null) {
                    // Rendert den aktuellen Zustand (Partikel bewegen sich flüssig)
                    canvasRenderer.render(controller.getParties(), getCurrentTotalVoters());
                }
            }
        };
        visualTimer.start();
    }

    private int getCurrentTotalVoters() {
        try {
            return Integer.parseInt(voterCountField.getText());
        } catch (Exception e) { return 10000; }
    }

    // Hilfsmethoden für TextField Parsing
    private void adjustTextFieldInteger(TextField field, int delta) {
        try {
            int val = Integer.parseInt(field.getText());
            field.setText(String.valueOf(Math.max(0, val + delta)));
        } catch (Exception e) { field.setText("0"); }
    }

    private void adjustTextFieldDouble(TextField field, double delta) {
        try {
            double val = Double.parseDouble(field.getText());
            // Runden auf 1 Nachkommastelle
            double newVal = Math.round((val + delta) * 10.0) / 10.0;
            field.setText(String.valueOf(Math.max(0, newVal)));
        } catch (Exception e) { field.setText("0.0"); }
    }
}
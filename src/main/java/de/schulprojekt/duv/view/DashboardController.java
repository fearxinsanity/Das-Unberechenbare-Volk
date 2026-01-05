package de.schulprojekt.duv.view;

import de.schulprojekt.duv.controller.SimulationController;
import de.schulprojekt.duv.model.scandal.ScandalEvent;
import de.schulprojekt.duv.model.core.SimulationParameters;
import de.schulprojekt.duv.model.voter.VoterTransition;
import de.schulprojekt.duv.model.party.Party;
import de.schulprojekt.duv.view.components.*;
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

public class DashboardController {

    @FXML private ScrollPane scandalTickerScroll;
    @FXML private HBox scandalTickerBox;
    @FXML private LineChart<Number, Number> historyChart;
    @FXML private Pane animationPane, eventFeedPane;
    @FXML private Label timeStepLabel;
    @FXML private TextField voterCountField, partyCountField, budgetField, scandalChanceField;
    @FXML private Slider mediaInfluenceSlider, mobilityRateSlider, loyaltyMeanSlider, randomRangeSlider;
    @FXML private Button startButton, pauseButton, resetButton, statsButton;

    @FXML private VBox leftSidebar;
    @FXML private VBox rightSidebar;

    private SimulationController controller;
    private CanvasRenderer canvasRenderer;
    private ChartManager chartManager;
    private FeedManager feedManager;
    private TooltipManager tooltipManager;

    private int currentTick = 0;

    @FXML
    public void initialize() {
        this.canvasRenderer = new CanvasRenderer(animationPane);
        this.chartManager = new ChartManager(historyChart);
        this.feedManager = new FeedManager(scandalTickerBox, scandalTickerScroll, eventFeedPane);
        this.tooltipManager = new TooltipManager(animationPane);

        canvasRenderer.getCanvas().setOnMouseMoved(e ->
                tooltipManager.handleMouseMove(e.getX(), e.getY(), controller.getParties(), canvasRenderer.getPartyPositions(), controller.getCurrentParameters().getTotalVoterCount()));
        canvasRenderer.getCanvas().setOnMouseExited(e -> tooltipManager.hideTooltip());

        this.controller = new SimulationController(this);
        handleParameterChange(null);
        canvasRenderer.startVisualTimer();

        updateStatusDisplay(false);

        Platform.runLater(() -> {
            if (animationPane.getScene() != null) {
                Scene scene = animationPane.getScene();
                scene.widthProperty().addListener((obs, oldVal, newVal) -> adjustScale(newVal.doubleValue()));
                adjustScale(scene.getWidth());

                if (leftSidebar != null) {
                    leftSidebar.prefWidthProperty().bind(Bindings.max(250, Bindings.min(450, scene.widthProperty().multiply(0.22))));
                }
                if (rightSidebar != null) {
                    rightSidebar.prefWidthProperty().bind(Bindings.max(250, Bindings.min(450, scene.widthProperty().multiply(0.22))));
                }
            }
        });
    }

    private void updateStatusDisplay(boolean isRunning) {
        if (timeStepLabel == null) return;
        String statusText = isRunning ? "RUNNING" : "HALTED";
        String color = isRunning ? "#55ff55" : "#ff5555";
        timeStepLabel.setText(String.format("SYSTEM_STATUS: %s | TICK: %d", statusText, currentTick));
        timeStepLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-family: 'Consolas'; -fx-font-weight: bold;");
    }

    private void adjustScale(double windowWidth) {
        if (animationPane.getScene() == null) return;
        double baseSize = 12.0;
        double scaleFactor = windowWidth / 1280.0;
        double newSize = Math.max(11.0, Math.min(18.0, baseSize * Math.sqrt(scaleFactor)));
        animationPane.getScene().getRoot().setStyle("-fx-font-size: " + String.format(Locale.US, "%.1f", newSize) + "px;");
    }

    private void triggerGlitchEffect() {
        if (leftSidebar == null || rightSidebar == null) return;
        leftSidebar.getStyleClass().add("glitch-active");
        rightSidebar.getStyleClass().add("glitch-active");

        javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(50), leftSidebar);
        tt.setByX(5);
        tt.setCycleCount(6);
        tt.setAutoReverse(true);

        javafx.animation.TranslateTransition tt2 = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(50), rightSidebar);
        tt2.setByX(-5);
        tt2.setCycleCount(6);
        tt2.setAutoReverse(true);

        javafx.animation.ParallelTransition pt = new javafx.animation.ParallelTransition(tt, tt2);
        pt.setOnFinished(e -> {
            leftSidebar.getStyleClass().remove("glitch-active");
            rightSidebar.getStyleClass().remove("glitch-active");
        });
        pt.play();
    }

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
        canvasRenderer.update(parties, transitions, controller.getCurrentParameters().getTotalVoterCount());

        if (scandal != null) {
            triggerGlitchEffect();
        }
    }

    @FXML
    public void handleParameterChange(Event event) {
        if (controller == null) return;
        try {
            int voters = parseIntSafe(voterCountField.getText(), 100000);
            int parties = parseIntSafe(partyCountField.getText(), 5);
            parties = Math.max(2, Math.min(8, parties));
            double scandalChance = Math.max(0.0, Math.min(60.0, parseDoubleSafe(scandalChanceField.getText(), 5.0)));
            double budgetInput = parseDoubleSafe(budgetField.getText(), 500000.0);
            double budgetFactor = Math.max(0.1, Math.min(10.0, budgetInput / 500000.0));

            SimulationParameters params = new SimulationParameters(
                    voters, mediaInfluenceSlider.getValue(), mobilityRateSlider.getValue(),
                    scandalChance, loyaltyMeanSlider.getValue(),
                    controller.getCurrentParameters().getSimulationTicksPerSecond(),
                    randomRangeSlider.getValue(), parties, budgetFactor
            );
            controller.updateAllParameters(params);
        } catch (Exception e) { e.printStackTrace(); }
    }

    // --- Statistik Ansicht aufrufen ---
    @FXML
    public void handleShowStatistics(ActionEvent event) {
        if (controller == null) return;

        // 1. Simulation pausieren
        if (controller.isRunning()) {
            handlePauseSimulation(null);
        }

        try {
            // 2. FXML laden (mit Fehlerprüfung)
            var resource = getClass().getResource("/de/schulprojekt/duv/view/StatisticsView.fxml");
            if (resource == null) {
                System.err.println("FEHLER: StatisticsView.fxml wurde nicht gefunden! Pfad prüfen.");
                new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "StatisticsView.fxml nicht gefunden!").show();
                return;
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Parent statsRoot = loader.load();

            StatisticsController statsCtrl = loader.getController();

            // 3. WICHTIG: Wir holen uns die aktuelle Ansicht (das Dashboard selbst)
            // damit wir später genau hierhin zurück können.
            Parent dashboardRoot = startButton.getScene().getRoot();

            // 4. Daten übergeben
            statsCtrl.initData(
                    controller.getParties(),
                    historyChart.getData(),
                    this.currentTick,
                    dashboardRoot // Wir übergeben die View, nicht die Szene!
            );

            // 5. Ansicht tauschen
            startButton.getScene().setRoot(statsRoot);

        } catch (IOException e) {
            e.printStackTrace();
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Fehler beim Laden der Statistik: " + e.getMessage()).show();
        }
    }

    @FXML public void handleStartSimulation(ActionEvent e) {
        if (controller != null) {
            controller.startSimulation();
            updateButtonState(true);
            updateStatusDisplay(true);
        }
    }

    @FXML public void handlePauseSimulation(ActionEvent e) {
        if (controller != null) {
            controller.pauseSimulation();
            updateButtonState(false);
            updateStatusDisplay(false);
        }
    }

    @FXML public void handleResetSimulation(ActionEvent e) {
        if (controller != null) {
            handleParameterChange(null);
            controller.resetSimulation();
            updateButtonState(false);
            if (resetButton != null) resetButton.setDisable(true);
            this.currentTick = 0;
            updateStatusDisplay(false);
        }
    }

    @FXML
    public void handleShowParliament(ActionEvent event) {
        if (controller == null) return;

        // Simulation pausieren
        if (controller.isRunning()) {
            handlePauseSimulation(null);
        }

        try {
            // Lade die FXML für die Parlamentsansicht
            var resource = getClass().getResource("/de/schulprojekt/duv/view/ParliamentView.fxml");
            if (resource == null) {
                System.err.println("FEHLER: ParliamentView.fxml nicht gefunden!");
                return;
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Parent parliamentRoot = loader.load();
            ParliamentController parlCtrl = loader.getController();

            // Aktuelle View merken (für den Zurück-Button)
            Parent dashboardView = startButton.getScene().getRoot();

            // Daten übergeben
            parlCtrl.initData(controller.getParties(), dashboardView);

            // Ansicht wechseln
            startButton.getScene().setRoot(parliamentRoot);

        } catch (IOException e) {
            e.printStackTrace();
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Fehler beim Laden: " + e.getMessage()).show();
        }
    }

    @FXML public void handleVoterCountIncrement(ActionEvent e) { adjustIntField(voterCountField, 10000, 1000, 2000000); }
    @FXML public void handleVoterCountDecrement(ActionEvent e) { adjustIntField(voterCountField, -10000, 1000, 2000000); }
    @FXML public void handlePartyCountIncrement(ActionEvent e) { adjustIntField(partyCountField, 1, 2, 8); }
    @FXML public void handlePartyCountDecrement(ActionEvent e) { adjustIntField(partyCountField, -1, 2, 8); }
    @FXML public void handleScandalChanceIncrement(ActionEvent e) { adjustDoubleField(scandalChanceField, 0.5, 0.0, 60.0); }
    @FXML public void handleScandalChanceDecrement(ActionEvent e) { adjustDoubleField(scandalChanceField, -0.5, 0.0, 60.0); }
    @FXML public void handleSpeed1x(ActionEvent e) { controller.updateSimulationSpeed(1); }
    @FXML public void handleSpeed2x(ActionEvent e) { controller.updateSimulationSpeed(2); }
    @FXML public void handleSpeed4x(ActionEvent e) { controller.updateSimulationSpeed(4); }

    private void adjustIntField(TextField f, int d, int min, int max) { f.setText(String.valueOf(Math.max(min, Math.min(max, parseIntSafe(f.getText(), min) + d)))); handleParameterChange(null); }
    private void adjustDoubleField(TextField f, double d, double min, double max) { f.setText(String.format(Locale.US, "%.1f", Math.max(min, Math.min(max, parseDoubleSafe(f.getText(), min) + d)))); handleParameterChange(null); }
    private int parseIntSafe(String t, int d) { try { return Integer.parseInt(t.replaceAll("[^0-9]", "")); } catch (Exception e) { return d; } }
    private double parseDoubleSafe(String t, double d) { try { return Double.parseDouble(t.replace(",", ".")); } catch (Exception e) { return d; } }
    private void updateButtonState(boolean r) { if (startButton != null) startButton.setDisable(r); if (pauseButton != null) pauseButton.setDisable(!r); if (resetButton != null) resetButton.setDisable(r); }
    public void shutdown() { if (controller != null) controller.shutdown(); canvasRenderer.stop(); }
}
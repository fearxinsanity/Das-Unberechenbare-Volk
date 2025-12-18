package de.schulprojekt.duv.view;

import de.schulprojekt.duv.controller.SimulationController;
import de.schulprojekt.duv.model.scandal.ScandalEvent;
import de.schulprojekt.duv.model.core.SimulationParameters;
import de.schulprojekt.duv.model.voter.VoterTransition;
import de.schulprojekt.duv.model.party.Party;
import de.schulprojekt.duv.view.components.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
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
    @FXML private Button startButton, pauseButton, resetButton;

    private SimulationController controller;
    private CanvasRenderer canvasRenderer;
    private ChartManager chartManager;
    private FeedManager feedManager;
    private TooltipManager tooltipManager;

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
    }

    public void updateDashboard(List<Party> parties, List<VoterTransition> transitions, ScandalEvent scandal, int step) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> updateDashboard(parties, transitions, scandal, step));
            return;
        }

        if (step == 0) {
            chartManager.clear();
            canvasRenderer.clear(parties);
            feedManager.clear();
        }

        if (timeStepLabel != null) {
            timeStepLabel.setText(String.format("Status: %s | Tick: %d", controller.isRunning() ? "Läuft" : "Pausiert", step));
        }

        feedManager.processScandal(scandal, step);
        chartManager.update(parties, step);
        canvasRenderer.update(parties, transitions, controller.getCurrentParameters().getTotalVoterCount());
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

    @FXML public void handleStartSimulation(ActionEvent e) { if (controller != null) { controller.startSimulation(); updateButtonState(true); } }
    @FXML public void handlePauseSimulation(ActionEvent e) { if (controller != null) { controller.pauseSimulation(); updateButtonState(false); } }
    @FXML public void handleResetSimulation(ActionEvent e) { if (controller != null) { handleParameterChange(null); controller.resetSimulation(); updateButtonState(false); if (resetButton != null) resetButton.setDisable(true); } }

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
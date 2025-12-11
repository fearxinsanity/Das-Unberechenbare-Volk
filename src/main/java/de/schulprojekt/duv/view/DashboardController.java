package de.schulprojekt.duv.view;

import de.schulprojekt.duv.controller.SimulationController;
import de.schulprojekt.duv.model.engine.SimulationParameters;
import de.schulprojekt.duv.model.entities.Party;
import de.schulprojekt.duv.model.entities.Voter;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class DashboardController {

    private SimulationController simulationController;
    private static final int VOTER_STEP = 100000;
    private Map<Voter, Circle> voterNodes;
    private int currentSimTimeStep = 0; // Zeitstempel Zähler

    // --- VISUALIZING ELEMENTS ---
    @FXML private PieChart partyDistributionChart;
    @FXML private Label timeStepLabel;
    @FXML private Pane animationPane;

    // --- INPUT ELEMENTS ---
    @FXML private TextField voterCountField;
    @FXML private Slider partyCountSlider;
    @FXML private Slider mediaInfluenceSlider;
    @FXML private Slider mobilityRateSlider;
    @FXML private Slider scandalChanceSlider;
    @FXML private Slider loyaltyMeanSlider;
    @FXML private Slider randomRangeSlider;

    // --- STEUERUNGS-BUTTONS (Checklistenpunkt 2) ---
    @FXML private Button startButton;
    @FXML private Button pauseButton;
    @FXML private Button resetButton;
    @FXML private Button speed1xButton; // Optional, für visuelles Feedback
    @FXML private Button speed2xButton; // Optional
    @FXML private Button speed4xButton; // Optional


    public void setSimulationController(SimulationController controller) {
        this.simulationController = controller;
    }

    @FXML
    public void initialize() {
    }

    // --- PARAMETER COLLECTION (Alle 8 Parameter werden gelesen) ---
    public SimulationParameters collectCurrentParameters() {
        SimulationParameters currentParams = simulationController.getCurrentParameters();
        int totalVoters = currentParams.getTotalVoterCount();
        try {
            totalVoters = Integer.parseInt(voterCountField.getText());
            if (totalVoters < 0) totalVoters = 0;
        } catch (NumberFormatException e) {
            System.err.println("Ungültige Wähleranzahl im Textfeld. Verwende alten Wert: " + totalVoters);
        }

        return new SimulationParameters(
                totalVoters,
                mediaInfluenceSlider.getValue(),
                mobilityRateSlider.getValue(),
                scandalChanceSlider.getValue(),
                loyaltyMeanSlider.getValue(),
                currentParams.getSimulationTicksPerSecond(),
                randomRangeSlider.getValue(),
                (int) partyCountSlider.getValue()
        );
    }

    // --- EVENT HANDLER (Wähleranzahl Inkrement/Dekrement) ---
    private void updateVoterCountField(int step) {
        if (voterCountField != null) {
            try {
                int currentCount = Integer.parseInt(voterCountField.getText());
                int newCount = currentCount + step;
                if (newCount < 0) newCount = 0;
                voterCountField.setText(String.valueOf(newCount));
                handleParameterChange();
            } catch (NumberFormatException e) {
                System.err.println("Ungültige Wähleranzahl im Textfeld.");
            }
        }
    }

    @FXML public void handleVoterCountIncrement() { updateVoterCountField(VOTER_STEP); }
    @FXML public void handleVoterCountDecrement() { updateVoterCountField(-VOTER_STEP); }

    // --- EVENT HANDLER (Simulation Steuerung mit Button-Zuständen) ---

    @FXML
    public void handleStartSimulation() {
        if (simulationController != null) {
            simulationController.startSimulation();

            // UI-Zustand setzen: Start deaktiviert, Pause und Reset aktiviert
            if (startButton != null) startButton.setDisable(true);
            if (pauseButton != null) pauseButton.setDisable(false);
            if (resetButton != null) resetButton.setDisable(false);
        }
    }

    @FXML
    public void handlePauseSimulation() {
        if (simulationController != null) {
            simulationController.pauseSimulation();

            // UI-Zustand setzen: Start und Reset aktiviert, Pause deaktiviert
            if (startButton != null) startButton.setDisable(false);
            if (pauseButton != null) pauseButton.setDisable(true);
            if (resetButton != null) resetButton.setDisable(false);
        }
    }

    @FXML
    public void handleResetSimulation() {
        if (simulationController != null) {
            simulationController.resetSimulation(); // Ruft engine.resetState() auf
            this.currentSimTimeStep = 0;
            setupVisuals();

            if (startButton != null) startButton.setDisable(false);
            if (pauseButton != null) pauseButton.setDisable(true);
            if (resetButton != null) resetButton.setDisable(true);

            updateDashboard(simulationController.getParties(), simulationController.getVoters());
        }
    }

    @FXML
    public void handleParameterChange() {
        if (simulationController != null) {
            SimulationParameters newParams = collectCurrentParameters();
            simulationController.updateAllParameters(newParams);
        }
    }

    // --- EVENT HANDLER (Geschwindigkeit) ---
    @FXML public void handleSpeed1x() { if (simulationController != null) simulationController.updateSimulationSpeed(1); }
    @FXML public void handleSpeed2x() { if (simulationController != null) simulationController.updateSimulationSpeed(2); }
    @FXML public void handleSpeed4x() { if (simulationController != null) simulationController.updateSimulationSpeed(4); }


    // --- DATA DISPLAY (Controller -> View) ---

    public void updateDashboard(List<Party> parties, List<Voter> voters) {

        // Status-Label aktualisieren (Checkliste Punkt 2)
        if (timeStepLabel != null) {
            String status = simulationController.isRunning() ? "Laufend" : "Pausiert/Initialisiert";
            timeStepLabel.setText("Status: " + status + " | Zeitschritt: " + (++this.currentSimTimeStep));
        }

        if (partyDistributionChart != null) {
            // PieChart Logik
            partyDistributionChart.getData().clear();

            for (Party party : parties) {
                PieChart.Data slice = new PieChart.Data(party.getName(), party.getCurrentSupporterCount());
                partyDistributionChart.getData().add(slice);

                if (slice.getNode() != null) {
                    slice.getNode().setStyle("-fx-pie-color: #" + party.getColorCode() + ";");
                }
            }
        }

        // Animation Logik (Checkliste Punkt 1)
        if (animationPane != null && voterNodes != null) {
            double paneWidth = animationPane.getWidth();
            for (Voter voter : voters) {
                Circle dot = voterNodes.get(voter);
                if (dot != null) {
                    double xPos = (voter.getPoliticalPosition() / 100.0) * paneWidth;
                    dot.setCenterX(xPos);
                    dot.setFill(Color.web("#" + voter.getCurrentParty().getColorCode()));
                }
            }
        }
    }

    public void setupVisuals() {
        this.voterNodes = new HashMap<>();
        List<Voter> initialVoters = simulationController.getVoters();
        Random random = new Random();

        if (animationPane != null) {
            double paneWidth = animationPane.getPrefWidth() > 0 ? animationPane.getPrefWidth() : 1200;
            double paneHeight = animationPane.getPrefHeight() > 0 ? animationPane.getPrefHeight() : 650;

            // Initialen Status setzen
            if (timeStepLabel != null) {
                timeStepLabel.setText("Status: Initialisiert | Zeitschritt: 0");
            }

            for (Voter voter : initialVoters) {
                Circle dot = new Circle(1.0);

                double xPos = (voter.getPoliticalPosition() / 100.0) * paneWidth;
                double yPos = (random.nextDouble() * 0.8 + 0.1) * paneHeight;

                dot.setCenterX(xPos);
                dot.setCenterY(yPos);
                dot.setFill(Color.web("#" + voter.getCurrentParty().getColorCode()));

                animationPane.getChildren().add(dot);
                voterNodes.put(voter, dot);
            }
        }
    }
}
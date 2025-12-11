package de.schulprojekt.duv.view;

import de.schulprojekt.duv.controller.SimulationController;
import de.schulprojekt.duv.model.engine.SimulationParameters;
import de.schulprojekt.duv.model.entities.Party;
import de.schulprojekt.duv.model.entities.Voter;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField; // NEU: Import für TextField
import javafx.scene.layout.Pane;
import java.util.List;

public class DashboardController {

    private SimulationController simulationController;
    private static final int VOTER_STEP = 100000;

    // --- VISUALIZING ELEMENTS ---
    @FXML private PieChart partyDistributionChart;
    @FXML private Label timeStepLabel;
    @FXML private Pane animationPane;

    // --- INPUT ELEMENTS ---
    @FXML private TextField voterCountField;

    // Slider
    @FXML private Slider partyCountSlider;
    @FXML private Slider mediaInfluenceSlider;
    @FXML private Slider mobilityRateSlider;
    @FXML private Slider scandalChanceSlider;
    @FXML private Slider loyaltyMeanSlider;
    @FXML private Slider randomRangeSlider;


    public void setSimulationController(SimulationController controller) {
        this.simulationController = controller;
    }

    @FXML
    public void initialize() {
    }

    // --- PARAMETER COLLECTION ---

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
                totalVoters,                                    // 1. totalVoterCount
                mediaInfluenceSlider.getValue(),                // 2. globalMediaInfluence
                mobilityRateSlider.getValue(),                  // 3. baseMobilityRate
                scandalChanceSlider.getValue(),                 // 4. scandalChance
                loyaltyMeanSlider.getValue(),                   // 5. initialLoyaltyMean
                currentParams.getSimulationTicksPerSecond(),    // 6. simulationTicksPerSecond (vom Model, gesetzt durch Buttons)
                randomRangeSlider.getValue(),                   // 7. uniformRandomRange
                (int) partyCountSlider.getValue()               // 8. numberOfParties
        );
    }

    // --- EVENT HANDLER (Voter count Increment/Decrement) ---

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

    @FXML
    public void handleVoterCountIncrement() {
        updateVoterCountField(VOTER_STEP);
    }

    @FXML
    public void handleVoterCountDecrement() {
        updateVoterCountField(-VOTER_STEP);
    }

    // --- EVENT HANDLER (simulation control) ---

    @FXML
    public void handleStartSimulation() {
        if (simulationController != null) {
            simulationController.startSimulation();
        }
    }

    @FXML
    public void handlePauseSimulation() {
        if (simulationController != null) {
            simulationController.pauseSimulation();
        }
    }

    @FXML
    public void handleResetSimulation() {
        if (simulationController != null) {
            simulationController.resetSimulation();
        }
    }

    @FXML
    public void handleParameterChange() {
        if (simulationController != null) {
            SimulationParameters newParams = collectCurrentParameters();
            simulationController.updateAllParameters(newParams);
        }
    }

    // --- EVENT HANDLER (simulation time) ---

    @FXML
    public void handleSpeed1x() {
        if (simulationController != null) {
            simulationController.updateSimulationSpeed(1);
        }
    }

    @FXML
    public void handleSpeed2x() {
        if (simulationController != null) {
            simulationController.updateSimulationSpeed(2);
        }
    }

    @FXML
    public void handleSpeed4x() {
        if (simulationController != null) {
            simulationController.updateSimulationSpeed(4);
        }
    }

    // --- DATA DISPLAY (Controller -> View) ---

    public void updateDashboard(List<Party> parties, List<Voter> voters) {

        if (partyDistributionChart != null) {
            partyDistributionChart.getData().clear();

            for (Party party : parties) {
                PieChart.Data slice = new PieChart.Data(party.getName(), party.getCurrentSupporterCount());
                partyDistributionChart.getData().add(slice);
            }
        }

        if (animationPane != null) {
            for (Voter voter : voters) {
                double politicalPosition = voter.getPoliticalPosition();
                String color = voter.getCurrentParty().getColorCode();
            }
        }
    }
}
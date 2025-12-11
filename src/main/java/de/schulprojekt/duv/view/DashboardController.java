package de.schulprojekt.duv.view;

import de.schulprojekt.duv.controller.SimulationController;
import de.schulprojekt.duv.model.engine.SimulationParameters;
import de.schulprojekt.duv.model.entities.Party;
import de.schulprojekt.duv.model.entities.Voter;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;

import java.util.List;

/**
 * FXML Controller for the DashboardUI.
 * This class adheres to the MVC pattern by acting as a "dumb" View.
 * It holds references to all FXML UI components and provides public methods
 * for the SimulationController to update them.
 * It contains NO simulation logic.
 */
public class DashboardController {

    private SimulationController simulationController;

    // --- VISUALISIERUNG ELEMENTE ---
    @FXML private PieChart partyDistributionChart;
    @FXML private Pane animationPane;

    // --- NEU: EINGABE ELEMENTE ---
    @FXML private TextField voterCountField;
    @FXML private Slider partyCountSlider;
    @FXML private Slider mediaInfluenceSlider;
    @FXML private Slider mobilityRateSlider;
    @FXML private Slider scandalChanceSlider;
    @FXML private Slider loyaltyMeanSlider;
    @FXML private Slider randomRangeSlider;


    /**
     * Sets the SimulationController reference. This is called by the Main class
     * immediately after the FXML is loaded to connect View and Controller.
     * @param controller The main application controller.
     */
    public void setSimulationController(SimulationController controller) {
        this.simulationController = controller;
    }

    private static final int VOTER_STEP = 100000;

    @FXML
    public void initialize() {
        // Hier werden später die Startwerte der UI-Elemente gesetzt oder Events initialisiert.
    }

    public SimulationParameters collectCurrentParameters() {
        SimulationParameters currentParams = simulationController.getCurrentParameters();
        int totalVoters = 0;
        try {
            totalVoters = Integer.parseInt(voterCountField.getText());
            if (totalVoters < 0) totalVoters = 0;
        } catch (NumberFormatException e) {
            totalVoters = currentParams.getTotalVoterCount();
            System.err.println("Ungültige Wähleranzahl im Textfeld. Verwende alten Wert: " + totalVoters);
        }
        return new SimulationParameters(
                totalVoters,
                mediaInfluenceSlider.getValue(),
                currentParams.getBaseMobilityRate(),
                currentParams.getScandalChance(),
                currentParams.getInitialLoyaltyMean(),
                currentParams.getSimulationTicksPerSecond(),
                currentParams.getUniformRandomRange(),
                currentParams.getNumberOfParties()
        );
    }

    // --- EVENT HANDLER (View -> Controller) ---

    @FXML
    public void handleStartSimulation() {
        if (simulationController != null) {
            simulationController.startSimulation();
            // TODO: UI-Anpassungen hier (z.B. Start-Button deaktivieren, Pause-Button aktivieren)
        }
    }

    @FXML
    public void handlePauseSimulation() {
        if (simulationController != null) {
            simulationController.pauseSimulation();
            // TODO: UI-Anpassungen hier
        }
    }

    @FXML
    public void handleResetSimulation() {
        if (simulationController != null) {
            simulationController.resetSimulation();
            // TODO: UI-Status (z.B. Buttons deaktivieren/aktivieren)
        }
    }

    @FXML
    public void handleParameterChange() {
        if (simulationController != null) {
            SimulationParameters newParams = collectCurrentParameters();

            // Wenn sich die TicksPerSecond NICHT ändert, rufen wir die Update-Methode auf
            // (Die Update-Logik im Controller handhabt das Setzen und den nötigen Reset)
            simulationController.updateAllParameters(newParams);
        }
    }

    @FXML
    public void handleSpeed1x() {
        if (simulationController != null) {
            simulationController.updateSimulationSpeed(1);
            // TODO: UI-Status: Den 1X Button visuell markieren (z.B. per CSS-Klasse)
        }
    }

    @FXML
    public void handleSpeed2x() {
        if (simulationController != null) {
            simulationController.updateSimulationSpeed(2);
            // TODO: UI-Status: Den 2X Button visuell markieren
        }
    }

    @FXML
    public void handleSpeed4x() {
        if (simulationController != null) {
            simulationController.updateSimulationSpeed(4);
            // TODO: UI-Status: Den 4X Button visuell markieren
        }
    }

    private void updateVoterCountField(int step) {
        if (voterCountField != null) {
            try {
                int currentCount = Integer.parseInt(voterCountField.getText());
                int newCount = currentCount + step;
                if (newCount < 0) newCount = 0; // Negative Wähleranzahl verhindern

                voterCountField.setText(String.valueOf(newCount));

                // Parameter-Update sofort nach Button-Klick auslösen
                handleParameterChange();
            } catch (NumberFormatException e) {
                // Bei ungültiger Eingabe im Feld nichts tun oder Fehlermeldung anzeigen
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

    // --- DATA DISPLAY (Controller -> View) ---

    /**
     * Called by the SimulationController every tick to update the entire UI.
     * This method is the final step of the MVC data flow (View-Aktualisierung).
     * @param parties The list of current Party states.
     * @param voters The list of all Voter states (used for animation).
     */
    public void updateDashboard(List<Party> parties, List<Voter> voters) {

        if (partyDistributionChart != null) {
            partyDistributionChart.getData().clear();

            for (Party party : parties) {
                PieChart.Data slice = new PieChart.Data(party.getName(), party.getCurrentSupporterCount());
                partyDistributionChart.getData().add(slice);

                // optional: Setze die Farbe (muss im CSS oder per FX-Code erfolgen)
                // slice.getNode().setStyle("-fx-pie-color: " + party.getColorCode());
            }
        }

        // --- 2. Aktualisierung der Animation / Scatter Plot ---
        if (animationPane != null) {
            for (Voter voter : voters) {
                double politicalPosition = voter.getPoliticalPosition(); // 0.0 bis 100.0
                String color = voter.getCurrentParty().getColorCode(); // Hex-Code

                // HIER müsste die Logik zum Verschieben/Neufärben des Circle-Knotens des Wählers stehen.
            }
            // Wir müssen hier die Knoten der Animation aktualisieren, aber das ist erst nach FXML-Implementierung möglich.
        }

        // --- 3. Status-Updates ---
        // timeStepLabel.setText("Zeit: " + System.currentTimeMillis());
    }
}
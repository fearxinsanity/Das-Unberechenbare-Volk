package de.schulprojekt.duv.view;

import de.schulprojekt.duv.controller.SimulationController;
import de.schulprojekt.duv.model.engine.SimulationParameters;
import de.schulprojekt.duv.model.engine.VoterTransition;
import de.schulprojekt.duv.model.entities.Party;
import de.schulprojekt.duv.model.entities.Voter;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardController {

    private SimulationController simulationController;
    private static final int VOTER_STEP = 100000;
    private Map<Party, Circle> partyNodes;  // Partei -> Kreis
    private int currentSimTimeStep = 0;

    // Konstanten für die Visualisierung
    private static final double MIN_PARTY_RADIUS = 20.0;
    private static final double MAX_PARTY_RADIUS = 80.0;
    private static final double TRANSITION_DOT_RADIUS = 3.0;
    private static final double ANIMATION_DURATION_MS = 300.0;

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

    // --- STEUERUNGS-BUTTONS ---
    @FXML private Button startButton;
    @FXML private Button pauseButton;
    @FXML private Button resetButton;

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

    // --- EVENT HANDLER ---
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

    @FXML
    public void handleStartSimulation() {
        if (simulationController != null) {
            simulationController.startSimulation();
            if (startButton != null) startButton.setDisable(true);
            if (pauseButton != null) pauseButton.setDisable(false);
            if (resetButton != null) resetButton.setDisable(false);
        }
    }

    @FXML
    public void handlePauseSimulation() {
        if (simulationController != null) {
            simulationController.pauseSimulation();
            if (startButton != null) startButton.setDisable(false);
            if (pauseButton != null) pauseButton.setDisable(true);
            if (resetButton != null) resetButton.setDisable(false);
        }
    }

    @FXML
    public void handleResetSimulation() {
        if (simulationController != null) {
            simulationController.resetSimulation();
            this.currentSimTimeStep = 0;
            setupVisuals();
            if (startButton != null) startButton.setDisable(false);
            if (pauseButton != null) pauseButton.setDisable(true);
            if (resetButton != null) resetButton.setDisable(true);
            updateDashboard(simulationController.getParties(), simulationController.getVoters(), List.of());
        }
    }

    @FXML
    public void handleParameterChange() {
        if (simulationController != null) {
            SimulationParameters newParams = collectCurrentParameters();
            simulationController.updateAllParameters(newParams);
        }
    }

    @FXML public void handleSpeed1x() { if (simulationController != null) simulationController.updateSimulationSpeed(1); }
    @FXML public void handleSpeed2x() { if (simulationController != null) simulationController.updateSimulationSpeed(2); }
    @FXML public void handleSpeed4x() { if (simulationController != null) simulationController.updateSimulationSpeed(4); }

    // --- NEUE VISUALISIERUNG ---

    /**
     * Berechnet den Radius eines Partei-Kreises basierend auf der Unterstützerzahl.
     */
    private double calculatePartyRadius(int supporterCount, int totalVoters) {
        if (totalVoters == 0) return MIN_PARTY_RADIUS;
        double ratio = (double) supporterCount / totalVoters;
        return MIN_PARTY_RADIUS + (MAX_PARTY_RADIUS - MIN_PARTY_RADIUS) * Math.sqrt(ratio);
    }

    /**
     * Aktualisiert das Dashboard mit Partei-Kreisen und Übergangs-Animationen.
     */
    public void updateDashboard(List<Party> parties, List<Voter> voters, List<VoterTransition> transitions) {
        // Status-Label aktualisieren
        if (timeStepLabel != null) {
            String status = simulationController.isRunning() ? "Laufend" : "Pausiert/Initialisiert";
            timeStepLabel.setText("Status: " + status + " | Zeitschritt: " + (++this.currentSimTimeStep));
        }

        // PieChart aktualisieren
        if (partyDistributionChart != null) {
            partyDistributionChart.getData().clear();
            for (Party party : parties) {
                PieChart.Data slice = new PieChart.Data(party.getName(), party.getCurrentSupporterCount());
                partyDistributionChart.getData().add(slice);
                if (slice.getNode() != null) {
                    slice.getNode().setStyle("-fx-pie-color: #" + party.getColorCode() + ";");
                }
            }
        }

        // Partei-Kreise aktualisieren
        if (animationPane != null && partyNodes != null) {
            int totalVoters = voters.size();
            double paneWidth = animationPane.getWidth() > 0 ? animationPane.getWidth() : 600;
            double paneHeight = animationPane.getHeight() > 0 ? animationPane.getHeight() : 400;

            for (Party party : parties) {
                Circle partyCircle = partyNodes.get(party);
                if (partyCircle != null) {
                    // Radius basierend auf Unterstützerzahl
                    double newRadius = calculatePartyRadius(party.getCurrentSupporterCount(), totalVoters);
                    partyCircle.setRadius(newRadius);

                    // Position basierend auf politischer Position
                    double xPos = (party.getPoliticalPosition() / 100.0) * (paneWidth - 2 * MAX_PARTY_RADIUS) + MAX_PARTY_RADIUS;
                    partyCircle.setCenterX(xPos);
                    partyCircle.setCenterY(paneHeight / 2);
                }
            }

            // Übergangs-Animationen erstellen
            for (VoterTransition transition : transitions) {
                animateTransition(transition.getOldParty(), transition.getNewParty());
            }
        }
    }

    /**
     * Animiert einen Wähler-Übergang zwischen zwei Parteien.
     */
    private void animateTransition(Party fromParty, Party toParty) {
        if (animationPane == null || partyNodes == null) return;

        Circle fromCircle = partyNodes.get(fromParty);
        Circle toCircle = partyNodes.get(toParty);

        if (fromCircle == null || toCircle == null) return;

        // Kleiner Übergangs-Punkt erstellen
        Circle transitionDot = new Circle(TRANSITION_DOT_RADIUS);
        transitionDot.setFill(Color.web("#" + fromParty.getColorCode()));
        transitionDot.setCenterX(fromCircle.getCenterX());
        transitionDot.setCenterY(fromCircle.getCenterY());

        animationPane.getChildren().add(transitionDot);

        // Animation: Bewegung von alter zu neuer Partei
        TranslateTransition animation = new TranslateTransition(
                Duration.millis(ANIMATION_DURATION_MS),
                transitionDot
        );

        double deltaX = toCircle.getCenterX() - fromCircle.getCenterX();
        double deltaY = toCircle.getCenterY() - fromCircle.getCenterY();

        animation.setByX(deltaX);
        animation.setByY(deltaY);

        // Nach Animation: Punkt entfernen und Farbe wechseln
        animation.setOnFinished(event -> {
            animationPane.getChildren().remove(transitionDot);
        });

        animation.play();
    }

    /**
     * Initialisiert die Partei-Kreise.
     */
    public void setupVisuals() {
        this.partyNodes = new HashMap<>();

        if (animationPane != null) {
            animationPane.getChildren().clear();

            List<Party> parties = simulationController.getParties();
            List<Voter> voters = simulationController.getVoters();
            int totalVoters = voters.size();

            double paneWidth = animationPane.getWidth() > 0 ? animationPane.getWidth() : 600;
            double paneHeight = animationPane.getHeight() > 0 ? animationPane.getHeight() : 400;

            if (timeStepLabel != null) {
                timeStepLabel.setText("Status: Initialisiert | Zeitschritt: 0");
            }

            for (Party party : parties) {
                double radius = calculatePartyRadius(party.getCurrentSupporterCount(), totalVoters);
                double xPos = (party.getPoliticalPosition() / 100.0) * (paneWidth - 2 * MAX_PARTY_RADIUS) + MAX_PARTY_RADIUS;
                double yPos = paneHeight / 2;

                Circle partyCircle = new Circle(xPos, yPos, radius);
                partyCircle.setFill(Color.web("#" + party.getColorCode()));
                partyCircle.setOpacity(0.8);

                // Partei-Name als Tooltip oder Label (optional)
                partyCircle.setUserData(party.getName());

                animationPane.getChildren().add(partyCircle);
                partyNodes.put(party, partyCircle);
            }
        }
    }
}
package de.schulprojekt.duv.view;

import de.schulprojekt.duv.controller.SimulationController;
import de.schulprojekt.duv.model.engine.ScandalEvent;
import de.schulprojekt.duv.model.engine.SimulationParameters;
import de.schulprojekt.duv.model.engine.VoterTransition;
import de.schulprojekt.duv.model.entities.Party;
import de.schulprojekt.duv.model.entities.Voter;
import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class DashboardController {

    private SimulationController simulationController;
    private static final int VOTER_STEP = 100000;
    private Map<Party, PieChart.Data> pieDataMap;
    private int currentSimTimeStep = 0;

    // Canvas fuer Animation
    private Canvas canvas;
    private GraphicsContext gc;
    private AnimationTimer renderLoop;

    // Partei-Positionen und Zustaende
    private Map<Party, PartyVisual> partyVisuals = new HashMap<>();

    // Aktive Waehler-Animationen (verschwinden nach Ankunft!)
    private ConcurrentLinkedQueue<MovingVoter> movingVoters = new ConcurrentLinkedQueue<>();

    // Pulse-Timer
    private double pulseTime = 0;

    // Skalierung
    private int votersPerPoint = 1;
    private static final int SCALING_THRESHOLD = 10000;
    private static final int TARGET_MAX_POINTS = 100;

    // Akkumulator fuer Uebergaenge
    private Map<String, Integer> transitionAccumulator = new HashMap<>();

    // Event-Feed
    private List<String> eventFeedMessages = new ArrayList<>();
    private static final int MAX_FEED_MESSAGES = 10;

    // Konstanten
    private static final double MIN_PARTY_RADIUS = 40.0;
    private static final double MAX_PARTY_RADIUS = 90.0;
    private static final double UNDECIDED_BASE_RADIUS = 60.0;
    private static final double VOTER_SIZE = 3.0;
    private static final int MAX_TRAIL_LENGTH = 25;
    private static final double ACCELERATION = 0.4;
    private static final double DAMPING = 0.96;
    private static final double ARRIVAL_THRESHOLD = 40.0;

    private static final String UNDECIDED_NAME = "Unsicher";

    // --- VISUALIZING ELEMENTS ---
    @FXML private PieChart partyDistributionChart;
    @FXML private Label timeStepLabel;
    @FXML private Pane animationPane;
    @FXML private Pane eventFeedPane;
    // Removed: @FXML private ProgressBar simulationProgress;
    // Removed: @FXML private Label durationLabel;

    // --- INPUT ELEMENTS ---
    @FXML private TextField voterCountField;
    @FXML private Slider partyCountSlider;
    @FXML private Slider mediaInfluenceSlider;
    @FXML private Slider mobilityRateSlider;
    @FXML private Slider scandalChanceSlider;
    @FXML private Slider loyaltyMeanSlider;
    @FXML private Slider randomRangeSlider;
    // Removed: @FXML private Slider durationSlider;

    // --- STEUERUNGS-BUTTONS ---
    @FXML private Button startButton;
    @FXML private Button pauseButton;
    @FXML private Button resetButton;

    // Innere Klasse fuer Partei-Visualisierung
    private static class PartyVisual {
        double x, y;
        double radius;
        double targetRadius;
        Color color;
        Color glowColor;
        String name;
        boolean isUndecided;

        PartyVisual(double x, double y, double radius, Color color, String name, boolean isUndecided) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.targetRadius = radius;
            this.color = color;
            this.glowColor = color.brighter();
            this.name = name;
            this.isUndecided = isUndecided;
        }
    }

    // Innere Klasse fuer bewegende Waehler
    private static class MovingVoter {
        double x, y;
        double vx, vy;
        double targetX, targetY;
        Color color;
        List<double[]> trail = new ArrayList<>();
        boolean arrived = false;
        int representedVoters;

        MovingVoter(double startX, double startY, double targetX, double targetY, Color color, int representedVoters) {
            this.x = startX;
            this.y = startY;
            this.targetX = targetX;
            this.targetY = targetY;
            this.color = color;
            this.vx = 0;
            this.vy = 0;
            this.representedVoters = representedVoters;
        }

        void update() {
            if (arrived) return;

            double dx = targetX - x;
            double dy = targetY - y;
            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance < ARRIVAL_THRESHOLD) {
                arrived = true;
                return;
            }

            vx += (dx / distance) * ACCELERATION;
            vy += (dy / distance) * ACCELERATION;

            vx *= DAMPING;
            vy *= DAMPING;

            x += vx;
            y += vy;

            trail.add(new double[]{x, y});
            if (trail.size() > MAX_TRAIL_LENGTH) {
                trail.remove(0);
            }
        }
    }

    public void setSimulationController(SimulationController controller) {
        this.simulationController = controller;
    }

    @FXML
    public void initialize() {
        // Event-Feed initialisieren
        if (eventFeedPane != null) {
            VBox feedBox = new VBox(5);
            feedBox.setStyle("-fx-padding: 10;");
            eventFeedPane.getChildren().add(feedBox);
        }
        // Removed: Duration-Slider Listener
    }

    // Removed: updateDurationLabel method

    private void setupCanvas() {
        if (animationPane == null) return;

        canvas = new Canvas();
        canvas.widthProperty().bind(animationPane.widthProperty());
        canvas.heightProperty().bind(animationPane.heightProperty());
        animationPane.getChildren().add(canvas);
        gc = canvas.getGraphicsContext2D();

        renderLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                render();
            }
        };
        renderLoop.start();
    }

    private void updateScaling() {
        int totalVoters = simulationController.getVoters().size();
        if (totalVoters <= SCALING_THRESHOLD) {
            votersPerPoint = 1;
        } else {
            votersPerPoint = Math.max(1, totalVoters / TARGET_MAX_POINTS);
        }
    }

    private void render() {
        if (gc == null || canvas == null) return;

        double width = canvas.getWidth();
        double height = canvas.getHeight();

        // Hintergrund
        gc.setFill(Color.web("#0a0a0a"));
        gc.fillRect(0, 0, width, height);

        pulseTime += 0.03;

        updatePartyPositions(width, height);

        // Parteien zeichnen (Unsicher zuerst, damit er "hinten" ist)
        List<PartyVisual> sortedVisuals = new ArrayList<>(partyVisuals.values());
        sortedVisuals.sort((a, b) -> Boolean.compare(b.isUndecided, a.isUndecided));

        for (PartyVisual pv : sortedVisuals) {
            drawParty(pv);
        }

        // Bewegende Waehler updaten und zeichnen
        // WICHTIG: Angekommene Waehler werden direkt entfernt (keine fadingTrails!)
        List<MovingVoter> toRemove = new ArrayList<>();
        for (MovingVoter voter : movingVoters) {
            voter.update();
            if (voter.arrived) {
                toRemove.add(voter);
            } else {
                drawMovingVoter(voter);
            }
        }
        movingVoters.removeAll(toRemove);
    }

    private void updatePartyPositions(double width, double height) {
        List<Party> parties = simulationController.getParties();
        int count = parties.size();
        int totalVoters = simulationController.getVoters().size();

        double centerX = width / 2;
        double centerY = height / 2;
        double spacing = Math.min(width, height) * 0.32;

        for (int i = 0; i < parties.size(); i++) {
            Party party = parties.get(i);
            PartyVisual pv = partyVisuals.get(party);

            boolean isUndecided = party.getName().equals(UNDECIDED_NAME);
            double x, y;

            if (isUndecided) {
                // Unsicher immer in der Mitte
                x = centerX;
                y = centerY;
            } else {
                // Andere Parteien kreisfoermig um die Mitte
                int realPartyIndex = i - 1; // Index ohne Unsicher
                int realPartyCount = count - 1;

                double angle = (2 * Math.PI * realPartyIndex / realPartyCount) - Math.PI / 2;
                x = centerX + Math.cos(angle) * spacing;
                y = centerY + Math.sin(angle) * spacing;
            }

            if (pv == null) {
                Color color = Color.web("#" + party.getColorCode());
                double baseRadius = isUndecided ? UNDECIDED_BASE_RADIUS : MIN_PARTY_RADIUS;
                pv = new PartyVisual(x, y, baseRadius, color, party.getName(), isUndecided);
                partyVisuals.put(party, pv);
            } else {
                pv.x = x;
                pv.y = y;
            }

            // Radius basierend auf Unterstuetzern
            double ratio = totalVoters > 0 ? (double) party.getCurrentSupporterCount() / totalVoters : 0.1;
            double baseRadius = isUndecided ? UNDECIDED_BASE_RADIUS : MIN_PARTY_RADIUS;
            double maxRadius = isUndecided ? MAX_PARTY_RADIUS * 1.2 : MAX_PARTY_RADIUS;
            pv.targetRadius = baseRadius + (maxRadius - baseRadius) * Math.sqrt(ratio);

            pv.radius += (pv.targetRadius - pv.radius) * 0.08;
        }
    }

    private void drawParty(PartyVisual pv) {
        double pulseSize = Math.sin(pulseTime) * (pv.isUndecided ? 2 : 4);
        double radius = pv.radius + pulseSize;

        // Aeusseres Glow (gedaempfter fuer Unsicher)
        double glowOpacity = pv.isUndecided ? 0.15 : 0.25;
        RadialGradient outerGlow = new RadialGradient(
                0, 0, pv.x, pv.y, radius + 50, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(pv.color.getRed(), pv.color.getGreen(), pv.color.getBlue(), glowOpacity)),
                new Stop(0.5, Color.color(pv.color.getRed(), pv.color.getGreen(), pv.color.getBlue(), glowOpacity * 0.4)),
                new Stop(1, Color.TRANSPARENT)
        );
        gc.setFill(outerGlow);
        gc.fillOval(pv.x - radius - 50, pv.y - radius - 50, (radius + 50) * 2, (radius + 50) * 2);

        // Haupt-Kreis
        RadialGradient mainGradient = new RadialGradient(
                0, 0, pv.x - radius * 0.3, pv.y - radius * 0.3, radius * 1.5, false, CycleMethod.NO_CYCLE,
                new Stop(0, pv.color.brighter()),
                new Stop(1, pv.color.darker())
        );
        gc.setFill(mainGradient);
        gc.fillOval(pv.x - radius, pv.y - radius, radius * 2, radius * 2);

        // Rand (gestrichelt fuer Unsicher)
        gc.setStroke(pv.glowColor);
        gc.setLineWidth(pv.isUndecided ? 1.5 : 2.5);
        if (pv.isUndecided) {
            gc.setLineDashes(5, 5);
        } else {
            gc.setLineDashes();
        }
        gc.strokeOval(pv.x - radius, pv.y - radius, radius * 2, radius * 2);
        gc.setLineDashes();

        // Partei-Name
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, pv.isUndecided ? 12 : 14));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText(pv.name, pv.x, pv.y);
    }

    private void drawMovingVoter(MovingVoter voter) {
        // Trail zeichnen
        if (voter.trail.size() > 1) {
            gc.setStroke(Color.color(voter.color.getRed(), voter.color.getGreen(), voter.color.getBlue(), 0.5));
            gc.setLineWidth(2);
            gc.beginPath();
            gc.moveTo(voter.trail.get(0)[0], voter.trail.get(0)[1]);
            for (int i = 1; i < voter.trail.size(); i++) {
                gc.lineTo(voter.trail.get(i)[0], voter.trail.get(i)[1]);
            }
            gc.stroke();
        }

        double size = VOTER_SIZE;
        if (voter.representedVoters > 1) {
            size = VOTER_SIZE + Math.log10(voter.representedVoters) * 1.5;
        }

        // Waehler-Punkt
        gc.setFill(voter.color);
        gc.fillOval(voter.x - size, voter.y - size, size * 2, size * 2);

        // Glow
        gc.setFill(Color.color(voter.color.getRed(), voter.color.getGreen(), voter.color.getBlue(), 0.3));
        gc.fillOval(voter.x - size * 2, voter.y - size * 2, size * 4, size * 4);
    }

    public SimulationParameters collectCurrentParameters() {
        // SimulationParameters currentParams = simulationController.getCurrentParameters(); // Nicht mehr nötig für Dauer

        int totalVoters = simulationController.getCurrentParameters().getTotalVoterCount(); // Initialwert
        try {
            totalVoters = Integer.parseInt(voterCountField.getText());
            if (totalVoters < 0) totalVoters = 0;
        } catch (NumberFormatException e) {
            System.err.println("Ungueltige Waehleranzahl im Textfeld.");
        }

        // Removed: duration parameter collection

        return new SimulationParameters(
                totalVoters,
                mediaInfluenceSlider.getValue(),
                mobilityRateSlider.getValue(),
                scandalChanceSlider.getValue(),
                loyaltyMeanSlider.getValue(),
                simulationController.getCurrentParameters().getSimulationTicksPerSecond(),
                randomRangeSlider.getValue(),
                (int) partyCountSlider.getValue()
                // Removed: duration parameter in constructor
        );
    }

    private void updateVoterCountField(int step) {
        if (voterCountField != null) {
            try {
                int currentCount = Integer.parseInt(voterCountField.getText());
                int newCount = currentCount + step;
                if (newCount < 0) newCount = 0;
                voterCountField.setText(String.valueOf(newCount));
                handleParameterChange();
            } catch (NumberFormatException e) {
                System.err.println("Ungueltige Waehleranzahl im Textfeld.");
            }
        }
    }

    private void addEventFeedMessage(String message) {
        eventFeedMessages.add(0, message);
        if (eventFeedMessages.size() > MAX_FEED_MESSAGES) {
            eventFeedMessages.remove(eventFeedMessages.size() - 1);
        }
        updateEventFeedDisplay();
    }

    private void updateEventFeedDisplay() {
        if (eventFeedPane == null) return;

        eventFeedPane.getChildren().clear();
        VBox feedBox = new VBox(3);
        feedBox.setStyle("-fx-padding: 5;");

        for (String msg : eventFeedMessages) {
            Label label = new Label(msg);
            label.setStyle("-fx-text-fill: #ffcc00; -fx-font-size: 11px;");
            label.setWrapText(true);
            label.setMaxWidth(eventFeedPane.getWidth() - 10);
            feedBox.getChildren().add(label);
        }

        eventFeedPane.getChildren().add(feedBox);
    }

    @FXML
    public void handleVoterCountIncrement() {
        updateVoterCountField(VOTER_STEP);
    }

    @FXML
    public void handleVoterCountDecrement() {
        updateVoterCountField(-VOTER_STEP);
    }

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

            movingVoters.clear();
            partyVisuals.clear();
            transitionAccumulator.clear();
            eventFeedMessages.clear();
            updateEventFeedDisplay();

            rebuildPieChart();

            if (startButton != null) startButton.setDisable(false);
            if (pauseButton != null) pauseButton.setDisable(true);
            if (resetButton != null) resetButton.setDisable(true);

            // Removed: simulationProgress logic

            updateDashboard(simulationController.getParties(), simulationController.getVoters(),
                    List.of(), null, 0); // Removed totalSteps argument
        }
    }

    @FXML
    public void handleParameterChange() {
        if (simulationController != null) {
            SimulationParameters newParams = collectCurrentParameters();
            simulationController.updateAllParameters(newParams);
            partyVisuals.clear();
            transitionAccumulator.clear();
            updateScaling();
            rebuildPieChart();
            updateDashboard(simulationController.getParties(), simulationController.getVoters(),
                    List.of(), null, 0); // Removed totalSteps argument
        }
    }

    @FXML
    public void handleSpeed1x() {
        if (simulationController != null) simulationController.updateSimulationSpeed(1);
    }

    @FXML
    public void handleSpeed2x() {
        if (simulationController != null) simulationController.updateSimulationSpeed(2);
    }

    @FXML
    public void handleSpeed4x() {
        if (simulationController != null) simulationController.updateSimulationSpeed(4);
    }

    // Removed: onSimulationComplete method

    private void rebuildPieChart() {
        if (partyDistributionChart == null) return;

        partyDistributionChart.getData().clear();
        pieDataMap = new HashMap<>();

        List<Party> parties = simulationController.getParties();
        for (Party party : parties) {
            PieChart.Data slice = new PieChart.Data(party.getName(), party.getCurrentSupporterCount());
            partyDistributionChart.getData().add(slice);
            pieDataMap.put(party, slice);
        }

        partyDistributionChart.applyCss();
        for (Party party : parties) {
            PieChart.Data slice = pieDataMap.get(party);
            if (slice != null && slice.getNode() != null) {
                slice.getNode().setStyle("-fx-pie-color: #" + party.getColorCode() + ";");
            }
        }
    }

    private void updatePieChartSmoothly(List<Party> parties) {
        if (partyDistributionChart == null || pieDataMap == null) return;

        for (Party party : parties) {
            PieChart.Data slice = pieDataMap.get(party);
            if (slice != null) {
                slice.setPieValue(party.getCurrentSupporterCount());
            }
        }
    }

    public void updateDashboard(List<Party> parties, List<Voter> voters,
                                List<VoterTransition> transitions, ScandalEvent scandal,
                                int currentStep) { // Removed totalSteps

        this.currentSimTimeStep = currentStep;

        if (timeStepLabel != null) {
            String status = simulationController.isRunning() ? "Laufend" : "Pausiert";
            // Updated text format to remove total steps
            timeStepLabel.setText(String.format("Status: %s | Tick: %d", status, currentStep));
        }

        // Removed: simulationProgress logic

        updatePieChartSmoothly(parties);
        updateScaling();

        // Skandal-Event verarbeiten
        if (scandal != null) {
            addEventFeedMessage(scandal.getEventMessage());
        }

        // Uebergaenge animieren
        for (VoterTransition transition : transitions) {
            String key = transition.getOldParty().getName() + "->" + transition.getNewParty().getName();
            int count = transitionAccumulator.getOrDefault(key, 0) + 1;

            if (count >= votersPerPoint) {
                spawnMovingVoter(transition.getOldParty(), transition.getNewParty(), count);
                transitionAccumulator.put(key, 0);
            } else {
                transitionAccumulator.put(key, count);
            }
        }
    }

    // Ueberladene Methode fuer Kompatibilitaet
    public void updateDashboard(List<Party> parties, List<Voter> voters, List<VoterTransition> transitions) {
        updateDashboard(parties, voters, transitions, null, currentSimTimeStep); // Removed totalTicks argument
    }

    private void spawnMovingVoter(Party fromParty, Party toParty, int representedVoters) {
        PartyVisual from = partyVisuals.get(fromParty);
        PartyVisual to = partyVisuals.get(toParty);

        if (from == null || to == null) return;

        double angle = Math.random() * Math.PI * 2;
        double startX = from.x + Math.cos(angle) * from.radius;
        double startY = from.y + Math.sin(angle) * from.radius;

        Color color = Color.web("#" + toParty.getColorCode());

        MovingVoter voter = new MovingVoter(startX, startY, to.x, to.y, color, representedVoters);
        movingVoters.add(voter);
    }

    public void setupVisuals() {
        partyVisuals.clear();
        movingVoters.clear();
        transitionAccumulator.clear();
        eventFeedMessages.clear();

        if (animationPane != null && canvas == null) {
            setupCanvas();
        }

        updateScaling();

        if (timeStepLabel != null) {
            timeStepLabel.setText("Status: Initialisiert | Tick: 0");
        }
    }
}
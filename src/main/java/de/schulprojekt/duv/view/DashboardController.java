package de.schulprojekt.duv.view;

import de.schulprojekt.duv.controller.SimulationController;
import de.schulprojekt.duv.model.engine.SimulationParameters;
import de.schulprojekt.duv.model.engine.VoterTransition;
import de.schulprojekt.duv.model.entities.Party;
import de.schulprojekt.duv.model.entities.Voter;
import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

public class DashboardController {

    private SimulationController simulationController;
    private static final int VOTER_STEP = 100000;
    private Map<Party, PieChart.Data> pieDataMap;
    private int currentSimTimeStep = 0;

    // Canvas für flüssige Animation
    private Canvas canvas;
    private GraphicsContext gc;
    private AnimationTimer renderLoop;

    // Partei-Positionen und Zustände
    private Map<Party, PartyVisual> partyVisuals = new HashMap<>();

    // Aktive Wähler-Animationen (nur sichtbar während Wechsel)
    private ConcurrentLinkedQueue<MovingVoter> movingVoters = new ConcurrentLinkedQueue<>();

    // Statische Trails die nach Ankunft bleiben
    private List<FadingTrail> fadingTrails = new ArrayList<>();

    // Pulse-Timer
    private double pulseTime = 0;

    // Skalierung: Wie viele Wähler repräsentiert ein Punkt?
    private int votersPerPoint = 1;
    private static final int SCALING_THRESHOLD = 10000;  // Ab dieser Wählerzahl skalieren
    private static final int TARGET_MAX_POINTS = 100;    // Maximale Anzahl gleichzeitiger Animationen

    // Akkumulator für Übergänge (für Skalierung)
    private Map<String, Integer> transitionAccumulator = new HashMap<>();

    // Konstanten
    private static final double MIN_PARTY_RADIUS = 40.0;
    private static final double MAX_PARTY_RADIUS = 90.0;
    private static final double VOTER_SIZE = 3.0;
    private static final int MAX_TRAIL_LENGTH = 25;
    private static final double ACCELERATION = 0.4;
    private static final double DAMPING = 0.96;
    private static final double ARRIVAL_THRESHOLD = 40.0;
    private static final long TRAIL_FADE_DURATION_MS = 15000;  // 15 Sekunden

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

    // Innere Klasse für Partei-Visualisierung
    private static class PartyVisual {
        double x, y;
        double radius;
        double targetRadius;
        Color color;
        Color glowColor;
        String name;

        PartyVisual(double x, double y, double radius, Color color, String name) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.targetRadius = radius;
            this.color = color;
            this.glowColor = color.brighter();
            this.name = name;
        }
    }

    // Innere Klasse für bewegende Wähler
    private static class MovingVoter {
        double x, y;
        double vx, vy;
        double targetX, targetY;
        Color color;
        List<double[]> trail = new ArrayList<>();
        boolean arrived = false;
        int representedVoters;  // Wie viele Wähler dieser Punkt repräsentiert

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

    // Innere Klasse für verblassende Trails nach Ankunft
    private static class FadingTrail {
        List<double[]> points;
        Color color;
        long creationTime;
        double endX, endY;

        FadingTrail(List<double[]> points, Color color, double endX, double endY) {
            this.points = new ArrayList<>(points);
            this.points.add(new double[]{endX, endY});
            this.color = color;
            this.creationTime = System.currentTimeMillis();
            this.endX = endX;
            this.endY = endY;
        }

        double getOpacity() {
            long age = System.currentTimeMillis() - creationTime;
            if (age >= TRAIL_FADE_DURATION_MS) return 0;
            return 1.0 - ((double) age / TRAIL_FADE_DURATION_MS);
        }

        boolean isExpired() {
            return System.currentTimeMillis() - creationTime >= TRAIL_FADE_DURATION_MS;
        }
    }

    public void setSimulationController(SimulationController controller) {
        this.simulationController = controller;
    }

    @FXML
    public void initialize() {
    }

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

    /**
     * Berechnet die Skalierung basierend auf der Wähleranzahl.
     */
    private void updateScaling() {
        int totalVoters = simulationController.getVoters().size();

        if (totalVoters <= SCALING_THRESHOLD) {
            votersPerPoint = 1;
        } else {
            // Berechne wie viele Wähler ein Punkt repräsentieren soll
            // Bei 100.000 Wählern = 100 Wähler pro Punkt
            // Bei 1.000.000 Wählern = 1000 Wähler pro Punkt
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

        // Pulse-Zeit updaten
        pulseTime += 0.03;

        // Partei-Positionen updaten
        updatePartyPositions(width, height);

        // Verblassende Trails zeichnen und aufräumen
        fadingTrails.removeIf(FadingTrail::isExpired);
        for (FadingTrail trail : fadingTrails) {
            drawFadingTrail(trail);
        }

        // Parteien zeichnen
        for (PartyVisual pv : partyVisuals.values()) {
            drawParty(pv);
        }

        // Bewegende Wähler updaten und zeichnen
        List<MovingVoter> toRemove = new ArrayList<>();
        for (MovingVoter voter : movingVoters) {
            voter.update();
            if (voter.arrived) {
                // Trail als verblassenden Trail speichern
                if (!voter.trail.isEmpty()) {
                    fadingTrails.add(new FadingTrail(voter.trail, voter.color, voter.x, voter.y));
                }
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

        double[][] positions = calculateGridPositions(count, centerX, centerY, spacing);

        for (int i = 0; i < parties.size(); i++) {
            Party party = parties.get(i);
            PartyVisual pv = partyVisuals.get(party);

            if (pv == null) {
                Color color = Color.web("#" + party.getColorCode());
                pv = new PartyVisual(positions[i][0], positions[i][1], MIN_PARTY_RADIUS, color, party.getName());
                partyVisuals.put(party, pv);
            } else {
                pv.x = positions[i][0];
                pv.y = positions[i][1];
            }

            double ratio = totalVoters > 0 ? (double) party.getCurrentSupporterCount() / totalVoters : 0.25;
            pv.targetRadius = MIN_PARTY_RADIUS + (MAX_PARTY_RADIUS - MIN_PARTY_RADIUS) * Math.sqrt(ratio);

            pv.radius += (pv.targetRadius - pv.radius) * 0.08;
        }
    }

    private double[][] calculateGridPositions(int count, double centerX, double centerY, double spacing) {
        double[][] positions = new double[count][2];

        if (count <= 2) {
            positions[0] = new double[]{centerX - spacing, centerY};
            if (count > 1) positions[1] = new double[]{centerX + spacing, centerY};
        } else if (count <= 4) {
            positions[0] = new double[]{centerX - spacing, centerY - spacing * 0.7};
            if (count > 1) positions[1] = new double[]{centerX + spacing, centerY - spacing * 0.7};
            if (count > 2) positions[2] = new double[]{centerX - spacing, centerY + spacing * 0.7};
            if (count > 3) positions[3] = new double[]{centerX + spacing, centerY + spacing * 0.7};
        } else {
            for (int i = 0; i < count; i++) {
                double angle = (2 * Math.PI * i / count) - Math.PI / 2;
                positions[i] = new double[]{
                        centerX + Math.cos(angle) * spacing,
                        centerY + Math.sin(angle) * spacing
                };
            }
        }

        return positions;
    }

    private void drawFadingTrail(FadingTrail trail) {
        double opacity = trail.getOpacity();
        if (opacity <= 0 || trail.points.size() < 2) return;

        gc.setStroke(Color.color(trail.color.getRed(), trail.color.getGreen(), trail.color.getBlue(), opacity * 0.5));
        gc.setLineWidth(2);
        gc.beginPath();
        gc.moveTo(trail.points.get(0)[0], trail.points.get(0)[1]);
        for (int i = 1; i < trail.points.size(); i++) {
            gc.lineTo(trail.points.get(i)[0], trail.points.get(i)[1]);
        }
        gc.stroke();

        // Endpunkt
        gc.setFill(Color.color(trail.color.getRed(), trail.color.getGreen(), trail.color.getBlue(), opacity * 0.6));
        gc.fillOval(trail.endX - VOTER_SIZE, trail.endY - VOTER_SIZE, VOTER_SIZE * 2, VOTER_SIZE * 2);
    }

    private void drawParty(PartyVisual pv) {
        double pulseSize = Math.sin(pulseTime) * 4;
        double radius = pv.radius + pulseSize;

        // Äußeres Glow
        RadialGradient outerGlow = new RadialGradient(
                0, 0, pv.x, pv.y, radius + 50, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(pv.color.getRed(), pv.color.getGreen(), pv.color.getBlue(), 0.25)),
                new Stop(0.5, Color.color(pv.color.getRed(), pv.color.getGreen(), pv.color.getBlue(), 0.1)),
                new Stop(1, Color.TRANSPARENT)
        );
        gc.setFill(outerGlow);
        gc.fillOval(pv.x - radius - 50, pv.y - radius - 50, (radius + 50) * 2, (radius + 50) * 2);

        // Haupt-Kreis mit Gradient
        RadialGradient mainGradient = new RadialGradient(
                0, 0, pv.x - radius * 0.3, pv.y - radius * 0.3, radius * 1.5, false, CycleMethod.NO_CYCLE,
                new Stop(0, pv.color.brighter()),
                new Stop(1, pv.color.darker())
        );
        gc.setFill(mainGradient);
        gc.fillOval(pv.x - radius, pv.y - radius, radius * 2, radius * 2);

        // Rand
        gc.setStroke(pv.glowColor);
        gc.setLineWidth(2.5);
        gc.strokeOval(pv.x - radius, pv.y - radius, radius * 2, radius * 2);

        // Partei-Name
        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font("Segoe UI", javafx.scene.text.FontWeight.BOLD, 14));
        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.setTextBaseline(javafx.geometry.VPos.CENTER);
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

        // Größe basierend auf repräsentierten Wählern
        double size = VOTER_SIZE;
        if (voter.representedVoters > 1) {
            size = VOTER_SIZE + Math.log10(voter.representedVoters) * 1.5;
        }

        // Wähler-Punkt mit Glow
        gc.setFill(voter.color);
        gc.fillOval(voter.x - size, voter.y - size, size * 2, size * 2);

        // Glow-Effekt
        gc.setFill(Color.color(voter.color.getRed(), voter.color.getGreen(), voter.color.getBlue(), 0.3));
        gc.fillOval(voter.x - size * 2, voter.y - size * 2, size * 4, size * 4);
    }

    public SimulationParameters collectCurrentParameters() {
        SimulationParameters currentParams = simulationController.getCurrentParameters();
        int totalVoters = currentParams.getTotalVoterCount();
        try {
            totalVoters = Integer.parseInt(voterCountField.getText());
            if (totalVoters < 0) totalVoters = 0;
        } catch (NumberFormatException e) {
            System.err.println("Ungültige Wähleranzahl im Textfeld.");
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
            fadingTrails.clear();
            partyVisuals.clear();
            transitionAccumulator.clear();

            rebuildPieChart();

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
            partyVisuals.clear();
            transitionAccumulator.clear();
            updateScaling();
            rebuildPieChart();
            updateDashboard(simulationController.getParties(), simulationController.getVoters(), List.of());
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

    public void updateDashboard(List<Party> parties, List<Voter> voters, List<VoterTransition> transitions) {
        if (timeStepLabel != null) {
            String status = simulationController.isRunning() ? "Laufend" : "Pausiert/Initialisiert";
            timeStepLabel.setText("Status: " + status + " | Zeitschritt: " + (++this.currentSimTimeStep));
        }

        updatePieChartSmoothly(parties);
        updateScaling();

        // Übergänge akkumulieren und bei Erreichen der Schwelle animieren
        for (VoterTransition transition : transitions) {
            String key = transition.getOldParty().getName() + "->" + transition.getNewParty().getName();
            int count = transitionAccumulator.getOrDefault(key, 0) + 1;

            if (count >= votersPerPoint) {
                // Genug Übergänge akkumuliert - Animation spawnen
                spawnMovingVoter(transition.getOldParty(), transition.getNewParty(), count);
                transitionAccumulator.put(key, 0);
            } else {
                transitionAccumulator.put(key, count);
            }
        }
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
        fadingTrails.clear();
        transitionAccumulator.clear();

        if (animationPane != null && canvas == null) {
            setupCanvas();
        }

        updateScaling();

        if (timeStepLabel != null) {
            timeStepLabel.setText("Status: Initialisiert | Zeitschritt: 0");
        }
    }
}
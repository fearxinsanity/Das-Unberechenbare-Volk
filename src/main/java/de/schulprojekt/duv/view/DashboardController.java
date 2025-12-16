package de.schulprojekt.duv.view;

import de.schulprojekt.duv.controller.SimulationController;
import de.schulprojekt.duv.model.engine.ScandalEvent;
import de.schulprojekt.duv.model.engine.SimulationConfig;
import de.schulprojekt.duv.model.engine.SimulationParameters;
import de.schulprojekt.duv.model.engine.VoterTransition;
import de.schulprojekt.duv.model.entities.Party;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.*;

public class DashboardController {

    // --- FXML UI Elemente ---
    @FXML private LineChart<Number, Number> historyChart;
    @FXML private Pane animationPane;
    @FXML private Pane eventFeedPane;
    @FXML private Label timeStepLabel;

    // Legenden-Container (Neu)
    @FXML private FlowPane legendPane;

    // Eingabefelder & Slider
    @FXML private TextField voterCountField;
    @FXML private Slider partyCountSlider;
    @FXML private Slider mediaInfluenceSlider;
    @FXML private Slider mobilityRateSlider;
    @FXML private Slider loyaltyMeanSlider;
    @FXML private Slider scandalChanceSlider;
    @FXML private Slider randomRangeSlider;

    // Buttons
    @FXML private Button startButton;
    @FXML private Button pauseButton;
    @FXML private Button resetButton;

    // --- Interne Logik ---
    private SimulationController controller;
    private Canvas canvas;
    private GraphicsContext gc;
    private AnimationTimer visualTimer;

    // Visualisierung (Pooling & Caching)
    private final Stack<MovingVoter> particlePool = new Stack<>();
    private final List<MovingVoter> activeParticles = new ArrayList<>();
    private final Map<String, XYChart.Series<Number, Number>> historySeriesMap = new HashMap<>();
    private final Map<String, Point> partyPositions = new HashMap<>();

    @FXML
    public void initialize() {
        // 1. Canvas Setup
        canvas = new Canvas(800, 600);
        if (animationPane != null) {
            animationPane.getChildren().add(canvas);
            canvas.widthProperty().bind(animationPane.widthProperty());
            canvas.heightProperty().bind(animationPane.heightProperty());
            animationPane.widthProperty().addListener((obs, old, val) -> recalculatePartyPositions());
            animationPane.heightProperty().addListener((obs, old, val) -> recalculatePartyPositions());
        }
        gc = canvas.getGraphicsContext2D();

        // 2. Charts konfigurieren
        setupCharts();

        // 3. Controller initialisieren (startet Logik-Thread)
        this.controller = new SimulationController(this);

        // 4. Initialwerte der UI an Controller senden (Synchronisierung Start)
        handleParameterChange(null);

        // 5. Render-Loop starten
        startVisualTimer();
    }

    private void setupCharts() {
        if (historyChart != null) {
            historyChart.setAnimated(false);
            historyChart.setCreateSymbols(false);
            historyChart.setLegendVisible(false); // Eigene Legende unten

            // Achsenbeschriftung explizit setzen (optional)
            if (historyChart.getXAxis() instanceof NumberAxis) {
                ((NumberAxis) historyChart.getXAxis()).setLabel("Zeit");
            }
            if (historyChart.getYAxis() instanceof NumberAxis) {
                ((NumberAxis) historyChart.getYAxis()).setLabel("Stimmen");
            }
        }
    }

    // --- FXML Event Handler ---

    @FXML
    public void handleStartSimulation(ActionEvent event) {
        if (controller != null) {
            controller.startSimulation();
            updateButtonState(true);
        }
    }

    @FXML
    public void handlePauseSimulation(ActionEvent event) {
        if (controller != null) {
            controller.pauseSimulation();
            updateButtonState(false);
        }
    }

    @FXML
    public void handleResetSimulation(ActionEvent event) {
        if (controller != null) {
            handleParameterChange(null);
            controller.resetSimulation();
            clearVisuals();
            updateButtonState(false);
            if (resetButton != null) resetButton.setDisable(true);
        }
    }

    @FXML
    public void handleParameterChange(Event event) {
        if (controller == null) return;
        try {
            int voters = Integer.parseInt(voterCountField.getText());

            SimulationParameters params = new SimulationParameters(
                    voters,
                    mediaInfluenceSlider.getValue(),
                    mobilityRateSlider.getValue(),
                    scandalChanceSlider.getValue(),
                    loyaltyMeanSlider.getValue(),
                    controller.getCurrentParameters().getSimulationTicksPerSecond(),
                    randomRangeSlider.getValue(),
                    (int) partyCountSlider.getValue()
            );
            controller.updateAllParameters(params);
        } catch (NumberFormatException e) {
            // Ignorieren
        }
    }

    @FXML public void handleVoterCountIncrement(ActionEvent event) { adjustVoterCount(1000); }
    @FXML public void handleVoterCountDecrement(ActionEvent event) { adjustVoterCount(-1000); }
    @FXML public void handleSpeed1x(ActionEvent event) { controller.updateSimulationSpeed(1); }
    @FXML public void handleSpeed2x(ActionEvent event) { controller.updateSimulationSpeed(2); }
    @FXML public void handleSpeed4x(ActionEvent event) { controller.updateSimulationSpeed(4); }


    // --- Hilfsmethoden UI ---

    private void adjustVoterCount(int delta) {
        try {
            int current = Integer.parseInt(voterCountField.getText());
            int newVal = Math.max(0, current + delta);
            voterCountField.setText(String.valueOf(newVal));
            handleParameterChange(null);
        } catch (NumberFormatException ignored) { }
    }

    private void updateButtonState(boolean running) {
        if (startButton != null) startButton.setDisable(running);
        if (pauseButton != null) pauseButton.setDisable(!running);
        if (resetButton != null) resetButton.setDisable(running);
    }

    private void updateChartYAxis(int maxVoters) {
        if (historyChart != null && historyChart.getYAxis() instanceof NumberAxis) {
            NumberAxis yAxis = (NumberAxis) historyChart.getYAxis();
            yAxis.setAutoRanging(false);
            yAxis.setLowerBound(0);
            yAxis.setUpperBound(maxVoters);
            yAxis.setTickUnit(maxVoters / 10.0);
        }
    }

    // --- Update vom SimulationController (im UI Thread) ---

    public void updateDashboard(List<Party> parties, List<VoterTransition> transitions, ScandalEvent scandal, int step) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> updateDashboard(parties, transitions, scandal, step));
            return;
        }

        // Reset-Logik bei Schritt 0
        if (step == 0) {
            historySeriesMap.clear();
            if (historyChart != null) historyChart.getData().clear();
            activeParticles.forEach(particlePool::push);
            activeParticles.clear();
            if (gc != null) gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
            partyPositions.clear();

            // Legende initial aufbauen
            updateLegend(parties);
        }

        // Falls sich Parteienanzahl ändert, Legende neu aufbauen
        if (legendPane != null && legendPane.getChildren().size() != parties.size()) {
            updateLegend(parties);
        }

        // Labels
        if (timeStepLabel != null) {
            String status = controller.isRunning() ? "Läuft" : "Pausiert";
            timeStepLabel.setText(String.format("Status: %s | Tick: %d", status, step));
        }

        // Skandal-Logik
        if (scandal != null && eventFeedPane != null) {
            Label msg = new Label("⚠ " + scandal.getScandal().getTitle() + " (" + scandal.getAffectedParty().getName() + ")");
            msg.setTextFill(Color.web("#ff5555"));
            msg.setWrapText(true);
            msg.setMaxWidth(eventFeedPane.getWidth() - 10);

            VBox box;
            if (eventFeedPane.getChildren().isEmpty()) {
                box = new VBox(5);
                eventFeedPane.getChildren().add(box);
            } else {
                box = (VBox) eventFeedPane.getChildren().get(0);
            }
            box.getChildren().add(0, msg);
            if (box.getChildren().size() > 8) box.getChildren().remove(8);
        }

        // --- CHART UPDATE (FLÜSSIG) ---
        // Früher: if (step % 5 == 0) -> Das hat das Ruckeln verursacht.
        // Jetzt: Update jeden Tick für maximale Smoothness.
        if (historyChart != null) {
            updateHistoryChart(parties, step);
        }

        // Canvas Positionen prüfen
        if (partyPositions.size() != parties.size()) {
            recalculatePartyPositions(parties);
        }

        spawnParticles(transitions);
    }

    // Neue Legenden-Methode (aus vorherigem Schritt)
    private void updateLegend(List<Party> parties) {
        if (legendPane == null) return;
        legendPane.getChildren().clear();

        for (Party p : parties) {
            HBox item = new HBox(6);
            item.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            item.setPadding(new javafx.geometry.Insets(0, 10, 0, 0));

            Circle icon = new Circle(6);
            icon.setFill(Color.web(p.getColorCode()));
            icon.setStroke(Color.WHITE);
            icon.setStrokeWidth(1);

            Label nameLabel = new Label(p.getName());
            nameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

            // Prozentanzeige
            int total = controller.getCurrentParameters().getTotalVoterCount();
            double percent = total > 0 ? (double)p.getCurrentSupporterCount() / total * 100 : 0;
            Label percentLabel = new Label(String.format("(%.1f%%)", percent));
            percentLabel.setStyle("-fx-text-fill: #aaa; -fx-font-size: 10px;");

            item.getChildren().addAll(icon, nameLabel, percentLabel);
            legendPane.getChildren().add(item);
        }
    }

    private void updateHistoryChart(List<Party> parties, int step) {
        double currentMaxSupporters = 0;

        for (Party p : parties) {
            // Wir ignorieren "Unsicher" für die Skalierung, wenn wir nur Parteien vergleichen wollen.
            // Falls du "Unsicher" auch im Graphen hast, nimm die Zeile raus.
            if (p.getName().equals(SimulationConfig.UNDECIDED_NAME)) continue;

            // 1. Datenpunkte hinzufügen (wie bisher)
            XYChart.Series<Number, Number> series = historySeriesMap.computeIfAbsent(p.getName(), k -> {
                XYChart.Series<Number, Number> s = new XYChart.Series<>();
                s.setName(k);
                historyChart.getData().add(s);
                s.getNode().setStyle("-fx-stroke: #" + p.getColorCode() + "; -fx-stroke-width: 2px;");
                return s;
            });

            int supporters = p.getCurrentSupporterCount();
            series.getData().add(new XYChart.Data<>(step, supporters));

            // Datenmengen begrenzen
            if (series.getData().size() > SimulationConfig.HISTORY_LENGTH) {
                series.getData().remove(0);
            }

            // 2. Maximum finden
            if (supporters > currentMaxSupporters) {
                currentMaxSupporters = supporters;
            }
        }

        // 3. Achse dynamisch anpassen (Smart Scaling)
        if (historyChart.getYAxis() instanceof NumberAxis) {
            NumberAxis yAxis = (NumberAxis) historyChart.getYAxis();

            // Auto-Ranging aus, damit wir volle Kontrolle haben
            yAxis.setAutoRanging(false);
            yAxis.setLowerBound(0);

            // Ziel: Maximum + 10% Puffer
            double targetUpperBound = currentMaxSupporters * 1.15;

            // Damit es nicht zittert: Auf "schöne" Zahlen runden.
            // Wenn wir bei 12.340 sind, runden wir auf 13.000 oder 15.000.
            double stepSize = (targetUpperBound > 10000) ? 5000 : 1000;
            double roundedUpperBound = Math.ceil(targetUpperBound / stepSize) * stepSize;

            // Mindestens 1000 als Obergrenze, damit der Graph am Anfang nicht spinnt
            if (roundedUpperBound < 1000) roundedUpperBound = 1000;

            // Nur updaten, wenn sich der Wert geändert hat (spart Performance)
            if (Math.abs(yAxis.getUpperBound() - roundedUpperBound) > 1.0) {
                yAxis.setUpperBound(roundedUpperBound);
                // TickUnit anpassen: Immer ca. 5-6 Linien anzeigen
                yAxis.setTickUnit(roundedUpperBound / 5.0);
            }
        }
    }

    // --- Visualisierung ---

    private void startVisualTimer() {
        visualTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                renderCanvas();
            }
        };
        visualTimer.start();
    }

    private void recalculatePartyPositions() {
        if (controller == null || controller.getParties() == null) return;
        recalculatePartyPositions(controller.getParties());
    }

    private void recalculatePartyPositions(List<Party> parties) {
        partyPositions.clear();
        double cx = canvas.getWidth() / 2;
        double cy = canvas.getHeight() / 2;
        double radius = Math.min(cx, cy) * 0.75;

        int count = parties.size();
        for (int i = 0; i < count; i++) {
            if (parties.get(i).getName().equals(SimulationConfig.UNDECIDED_NAME)) {
                partyPositions.put(parties.get(i).getName(), new Point(cx, cy));
                continue;
            }
            double angle = 2 * Math.PI * i / count - Math.PI / 2;
            double x = cx + radius * Math.cos(angle);
            double y = cy + radius * Math.sin(angle);
            partyPositions.put(parties.get(i).getName(), new Point(x, y));
        }
    }

    private void spawnParticles(List<VoterTransition> transitions) {
        int limit = 0;
        for (VoterTransition t : transitions) {
            if (limit++ > 50) break;

            Point start = partyPositions.get(t.getOldParty().getName());
            Point end = partyPositions.get(t.getNewParty().getName());

            if (start != null && end != null) {
                Color c = Color.web(t.getNewParty().getColorCode());
                MovingVoter p = particlePool.isEmpty() ? new MovingVoter() : particlePool.pop();
                p.reset(start.x, start.y, end.x, end.y, c);
                activeParticles.add(p);
            }
        }
    }

    private void renderCanvas() {
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        if (controller != null) {
            for (Party p : controller.getParties()) {
                Point pt = partyPositions.get(p.getName());
                if (pt != null) {
                    gc.setGlobalAlpha(0.2);
                    gc.setFill(Color.web(p.getColorCode()));
                    gc.fillOval(pt.x - 35, pt.y - 35, 70, 70);

                    gc.setGlobalAlpha(1.0);
                    gc.setFill(Color.web(p.getColorCode()));
                    gc.fillOval(pt.x - 10, pt.y - 10, 20, 20);

                    gc.setFill(Color.WHITE);
                    gc.fillText(p.getName(), pt.x - 15, pt.y + 25);
                }
            }
        }

        Iterator<MovingVoter> it = activeParticles.iterator();
        while (it.hasNext()) {
            MovingVoter p = it.next();
            p.move();
            gc.setFill(p.color);
            gc.fillOval(p.x - 2, p.y - 2, 4, 4);

            if (p.hasArrived()) {
                it.remove();
                particlePool.push(p);
            }
        }
    }

    private void clearVisuals() {
        activeParticles.forEach(particlePool::push);
        activeParticles.clear();
        historySeriesMap.clear();
        partyPositions.clear();

        if (historyChart != null) historyChart.getData().clear();
        if (gc != null) gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        if (eventFeedPane != null) eventFeedPane.getChildren().clear();
        if (legendPane != null) legendPane.getChildren().clear();
    }

    public void shutdown() {
        if (controller != null) controller.shutdown();
        if (visualTimer != null) visualTimer.stop();
    }

    // --- Helper Klassen ---
    private static class Point {
        double x, y;
        Point(double x, double y) { this.x = x; this.y = y; }
    }

    private static class MovingVoter {
        double x, y, targetX, targetY, speed = 4.0;
        Color color;
        boolean arrived;

        void reset(double sx, double sy, double tx, double ty, Color c) {
            this.x = sx + (Math.random() - 0.5) * 20;
            this.y = sy + (Math.random() - 0.5) * 20;
            this.targetX = tx;
            this.targetY = ty;
            this.color = c;
            this.arrived = false;
        }

        void move() {
            double dx = targetX - x;
            double dy = targetY - y;
            double dist = Math.sqrt(dx*dx + dy*dy);
            if (dist < speed) { x = targetX; y = targetY; arrived = true; }
            else { x += (dx/dist)*speed; y += (dy/dist)*speed; }
        }
        boolean hasArrived() { return arrived; }
    }
}
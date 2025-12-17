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
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.*;

public class DashboardController {

    // --- FXML UI Elemente ---
    @FXML private PieChart partyDistributionChart;
    @FXML private LineChart<Number, Number> historyChart;
    @FXML private Pane animationPane;
    @FXML private Pane eventFeedPane;
    @FXML private Label timeStepLabel;

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
        if (partyDistributionChart != null) partyDistributionChart.setAnimated(false);
        if (historyChart != null) {
            historyChart.setAnimated(false);
            historyChart.setCreateSymbols(false);
            historyChart.setLegendVisible(false);
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
            // Parameter neu senden (falls Slider geändert wurden ohne Übernehmen)
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

    @FXML
    public void handleVoterCountIncrement(ActionEvent event) {
        adjustVoterCount(1000);
    }

    @FXML
    public void handleVoterCountDecrement(ActionEvent event) {
        adjustVoterCount(-1000);
    }

    @FXML
    public void handleSpeed1x(ActionEvent event) { controller.updateSimulationSpeed(1); }
    @FXML
    public void handleSpeed2x(ActionEvent event) { controller.updateSimulationSpeed(2); }
    @FXML
    public void handleSpeed4x(ActionEvent event) { controller.updateSimulationSpeed(4); }


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

    // --- Update vom SimulationController (im UI Thread) ---

    public void updateDashboard(List<Party> parties, List<VoterTransition> transitions, ScandalEvent scandal, int step) {
        if (!Platform.isFxApplicationThread()) {
            // Falls wir im falschen Thread sind -> Auftrag an JavaFX übergeben und abbrechen
            Platform.runLater(() -> updateDashboard(parties, transitions, scandal, step));
            return;
        }
        if (step == 0) {
            historySeriesMap.clear();
            if (historyChart != null) historyChart.getData().clear();
            activeParticles.forEach(particlePool::push);
            activeParticles.clear();
            if (gc != null) gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

            // WICHTIGSTER FIX: Positionen löschen, damit sie für die NEUEN Parteien berechnet werden!
            partyPositions.clear();
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

            javafx.scene.layout.VBox box;
            if (eventFeedPane.getChildren().isEmpty()) {
                box = new javafx.scene.layout.VBox(5);
                eventFeedPane.getChildren().add(box);
            } else {
                box = (javafx.scene.layout.VBox) eventFeedPane.getChildren().get(0);
            }
            box.getChildren().add(0, msg);
            if (box.getChildren().size() > 8) box.getChildren().remove(8);
        }

        // Charts aktualisieren
        updateStandardCharts(parties);

        if (historyChart != null && step % 5 == 0) {
            updateHistoryChart(parties, step);
        }

        // Canvas Positionen prüfen und ggf. neu berechnen
        // Falls partyPositions leer ist (durch Reset oben), wird hier neu berechnet.
        if (partyPositions.size() != parties.size()) {
            recalculatePartyPositions(parties);
        }

        spawnParticles(transitions);
    }

    private void updateStandardCharts(List<Party> parties) {
        if (partyDistributionChart == null) return;

        // 1. Daten aktualisieren oder neu bauen
        boolean needsRebuild = partyDistributionChart.getData().size() != parties.size();
        if (!needsRebuild && !partyDistributionChart.getData().isEmpty()) {
            for (int i = 0; i < parties.size(); i++) {
                if (!partyDistributionChart.getData().get(i).getName().equals(parties.get(i).getName())) {
                    needsRebuild = true;
                    break;
                }
            }
        }

        if (partyDistributionChart.getData().isEmpty() || needsRebuild) {
            partyDistributionChart.getData().clear();
            for (Party p : parties) {
                PieChart.Data data = new PieChart.Data(p.getName(), p.getCurrentSupporterCount());
                partyDistributionChart.getData().add(data);

                // Listener für Tortenstück-Farbe (Slice)
                data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                    if (newNode != null) {
                        newNode.setStyle("-fx-pie-color: #" + p.getColorCode() + ";");
                    }
                });

                // Fallback falls Node schon da
                if (data.getNode() != null) {
                    data.getNode().setStyle("-fx-pie-color: #" + p.getColorCode() + ";");
                }
            }
        } else {
            // Nur Werte updaten
            for (int i = 0; i < parties.size(); i++) {
                PieChart.Data data = partyDistributionChart.getData().get(i);
                Party p = parties.get(i);
                data.setPieValue(p.getCurrentSupporterCount());

                if (data.getNode() != null) {
                    String style = "-fx-pie-color: #" + p.getColorCode() + ";";
                    if (!style.equals(data.getNode().getStyle())) {
                        data.getNode().setStyle(style);
                    }
                }
            }
        }

        // 2. WICHTIG: Legenden-Farben korrigieren
        // Wir führen das verzögert aus, damit JavaFX Zeit hat, die Legende zu rendern
        Platform.runLater(() -> fixLegendColors(parties));
    }

    /**
     * Sucht die Legenden-Einträge im Chart und färbt die Symbole korrekt ein.
     */
    private void fixLegendColors(List<Party> parties) {
        if (partyDistributionChart == null) return;

        // Erzwingt Layout-Update, damit Legenden-Nodes gefunden werden
        partyDistributionChart.applyCss();
        partyDistributionChart.layout();

        // Alle Legenden-Items suchen (CSS Selektor)
        Set<javafx.scene.Node> items = partyDistributionChart.lookupAll(".chart-legend-item");

        for (javafx.scene.Node item : items) {
            if (item instanceof Label) {
                Label label = (Label) item;

                // Den passenden Parteinamen suchen
                for (Party p : parties) {
                    if (p.getName().equals(label.getText())) {
                        // Das Symbol (der Punkt) ist das "Graphic" des Labels
                        if (label.getGraphic() != null) {
                            label.getGraphic().setStyle("-fx-background-color: #" + p.getColorCode() + ";");
                        }
                        break;
                    }
                }
            }
        }
    }

    private void updateHistoryChart(List<Party> parties, int step) {
        for (Party p : parties) {
            if (p.getName().equals(SimulationConfig.UNDECIDED_NAME)) continue;

            XYChart.Series<Number, Number> series = historySeriesMap.computeIfAbsent(p.getName(), k -> {
                XYChart.Series<Number, Number> s = new XYChart.Series<>();
                s.setName(k);
                historyChart.getData().add(s);
                s.getNode().setStyle("-fx-stroke: #" + p.getColorCode() + "; -fx-stroke-width: 2px;");
                return s;
            });

            series.getData().add(new XYChart.Data<>(step, p.getCurrentSupporterCount()));
            if (series.getData().size() > SimulationConfig.HISTORY_LENGTH) {
                series.getData().remove(0);
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

            // Wenn Cache leer (z.B. direkt nach Reset), nichts spawnen bis Neuberechnung
            if (start != null && end != null) {
                Color c = Color.web(t.getNewParty().getColorCode());
                MovingVoter p = particlePool.isEmpty() ? new MovingVoter() : particlePool.pop();
                p.reset(start.x, start.y, end.x, end.y, c);
                activeParticles.add(p);
            }
        }
    }

    private void renderCanvas() {
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        gc.clearRect(0, 0, width, height);

        // --- NEU: PHASE 1 - Das verborgene Netzwerk zeichnen ---
        // Wir zeichnen sehr subtile Linien zwischen allen Parteien, um Verbindungen anzudeuten.
        if (controller != null && !controller.getParties().isEmpty()) {
            gc.setStroke(Color.web("#D4AF37", 0.15)); // Gold, aber sehr transparent (15%)
            gc.setLineWidth(0.8);

            List<Party> parties = controller.getParties();
            for (int i = 0; i < parties.size(); i++) {
                Point p1 = partyPositions.get(parties.get(i).getName());
                if (p1 == null) continue;

                // Verbinde jeden mit jedem anderen (nur einmal)
                for (int j = i + 1; j < parties.size(); j++) {
                    Point p2 = partyPositions.get(parties.get(j).getName());
                    if (p2 == null) continue;
                    gc.strokeLine(p1.x, p1.y, p2.x, p2.y);
                }
            }
        }

        // --- PHASE 2 - Parteien (Die Machtzentren) ---
        // (Dieser Teil ist fast gleich wie vorher, nur etwas düsterere Farben)
        if (controller != null) {
            int totalVoters = controller.getCurrentParameters().getTotalVoterCount();
            if (totalVoters <= 0) totalVoters = 1;

            for (Party p : controller.getParties()) {
                Point pt = partyPositions.get(p.getName());
                if (pt != null) {
                    Color pColor = Color.web(p.getColorCode());
                    // Farbe etwas entsättigen und abdunkeln für den düsteren Look
                    Color mysteryColor = pColor.deriveColor(0, 0.8, 0.9, 1.0);

                    double share = (double) p.getCurrentSupporterCount() / totalVoters;
                    double dynamicRadius = 30.0 + (share * 60.0);

                    // Glow-Effekt (stärkerer Kontrast innen/außen)
                    RadialGradient glow = new RadialGradient(
                            0, 0, pt.x, pt.y, dynamicRadius, false, CycleMethod.NO_CYCLE,
                            new Stop(0.0, mysteryColor.deriveColor(0, 1.0, 1.0, 0.7)),
                            new Stop(0.6, mysteryColor.deriveColor(0, 1.0, 0.6, 0.2)),
                            new Stop(1.0, Color.TRANSPARENT)
                    );
                    gc.setFill(glow);
                    gc.fillOval(pt.x - dynamicRadius, pt.y - dynamicRadius, dynamicRadius * 2, dynamicRadius * 2);

                    // Kern
                    gc.setGlobalAlpha(1.0);
                    gc.setFill(mysteryColor.brighter()); // Kern leuchtet heller
                    gc.fillOval(pt.x - 10, pt.y - 10, 20, 20);
                    gc.setStroke(Color.web("#D4AF37")); // Goldener Rand statt weiß
                    gc.setLineWidth(1.5);
                    gc.strokeOval(pt.x - 10, pt.y - 10, 20, 20);

                    // Labels (Monospace Font hier auch nutzen)
                    gc.setFill(Color.web("#e0e0e0"));
                    gc.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
                    gc.fillText(p.getName(), pt.x - 20, pt.y + 35);
                    gc.setFill(Color.web("#D4AF37")); // Prozent in Gold
                    gc.fillText(String.format("%.1f%%", share * 100), pt.x - 10, pt.y + 48);
                }
            }
        }

        // --- NEU: PHASE 3 - Wähler als "Datenströme" mit Schweif ---
        Iterator<MovingVoter> it = activeParticles.iterator();
        gc.setLineWidth(2.0); // Dicke der Datenpakete

        while (it.hasNext()) {
            MovingVoter p = it.next();
            p.move();

            // Wir zeichnen den Partikel mehrmals, um einen "Schweif" zu simulieren
            double trailLength = 3; // Anzahl der Schweif-Segmente
            for (int i = 0; i < trailLength; i++) {
                // Je weiter hinten im Schweif, desto transparenter
                double alpha = 0.8 - (i * (0.8 / trailLength));
                gc.setGlobalAlpha(alpha);
                gc.setStroke(p.color.deriveColor(0, 1.0, 1.5, 1.0)); // Heller leuchtend

                // Berechnung der "Rückwärts"-Position für den Schweif
                // Wir nutzen die aktuelle Geschwindigkeit (dx, dy)
                double backX = p.x - (p.dx * i * 2.0); // Faktor 2.0 streckt den Schweif
                double backY = p.y - (p.dy * i * 2.0);
                // Ein kleines Stück Linie zeichnen, das in Bewegungsrichtung zeigt
                gc.strokeLine(backX, backY, backX - p.dx*2, backY - p.dy*2);
            }

            if (p.hasArrived()) {
                it.remove();
                particlePool.push(p);
            }
        }
        gc.setGlobalAlpha(1.0);
    }

    private void clearVisuals() {
        // Partikel zurücksetzen
        activeParticles.forEach(particlePool::push);
        activeParticles.clear();

        // Historie zurücksetzen
        historySeriesMap.clear();

        // Cache für Positionen löschen
        partyPositions.clear();

        // Diagramme leeren
        if (historyChart != null) historyChart.getData().clear();
        if (partyDistributionChart != null) partyDistributionChart.getData().clear();

        // Canvas leeren
        if (gc != null) gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // NEU: Skandal-Feed leeren
        if (eventFeedPane != null) {
            eventFeedPane.getChildren().clear();
        }
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
        // HIER SIND DIE NEUEN FELDER dx UND dy:
        double x, y, targetX, targetY, speed = 4.0;
        double dx, dy; // <-- Das hat gefehlt!

        Color color;
        boolean arrived;

        void reset(double sx, double sy, double tx, double ty, Color c) {
            this.x = sx + (Math.random() - 0.5) * 20;
            this.y = sy + (Math.random() - 0.5) * 20;
            this.targetX = tx;
            this.targetY = ty;
            this.color = c;
            this.arrived = false;
            // Reset dx/dy
            this.dx = 0;
            this.dy = 0;
        }

        void move() {
            double distX = targetX - x;
            double distY = targetY - y;
            double dist = Math.sqrt(distX*distX + distY*distY);

            if (dist < speed) {
                x = targetX;
                y = targetY;
                arrived = true;
                // Wenn angekommen, keine Bewegung mehr
                this.dx = 0;
                this.dy = 0;
            } else {
                // Speichern der aktuellen Bewegung für den Schweif
                this.dx = (distX/dist)*speed;
                this.dy = (distY/dist)*speed;

                x += this.dx;
                y += this.dy;
            }
        }

        boolean hasArrived() { return arrived; }
    }
}
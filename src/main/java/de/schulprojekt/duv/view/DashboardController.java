package de.schulprojekt.duv.view;

import de.schulprojekt.duv.controller.SimulationController;
import de.schulprojekt.duv.model.scandal.ScandalEvent;
import de.schulprojekt.duv.util.SimulationConfig;
import de.schulprojekt.duv.model.core.SimulationParameters;
import de.schulprojekt.duv.model.voter.VoterTransition;
import de.schulprojekt.duv.model.party.Party;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.*;

public class DashboardController {

    // --- FXML UI Elemente ---
    @FXML private ScrollPane scandalTickerScroll;
    @FXML private HBox scandalTickerBox;
    @FXML private LineChart<Number, Number> historyChart;
    @FXML private Pane animationPane;
    @FXML private Pane eventFeedPane;
    @FXML private Label timeStepLabel;

    @FXML private TextField voterCountField;
    @FXML private TextField partyCountField;
    @FXML private TextField budgetField;
    @FXML private TextField scandalChanceField;

    @FXML private Slider mediaInfluenceSlider;
    @FXML private Slider mobilityRateSlider;
    @FXML private Slider loyaltyMeanSlider;
    @FXML private Slider randomRangeSlider;

    @FXML private Button startButton;
    @FXML private Button pauseButton;
    @FXML private Button resetButton;

    // --- Interne Logik ---
    private SimulationController controller;
    private Canvas canvas;
    private GraphicsContext gc;
    private AnimationTimer visualTimer;

    // Visualisierung
    private final Stack<MovingVoter> particlePool = new Stack<>();
    private final List<MovingVoter> activeParticles = new ArrayList<>();
    private final Map<String, XYChart.Series<Number, Number>> historySeriesMap = new HashMap<>();
    private final Map<String, Point> partyPositions = new HashMap<>();

    // Tooltip-Elemente
    private VBox tooltipBox;
    private Label tooltipNameLabel, tooltipAbbrLabel, tooltipVotersLabel, tooltipPositionLabel, tooltipScandalsLabel;

    @FXML
    public void initialize() {
        canvas = new Canvas(800, 600);
        if (animationPane != null) {
            animationPane.getChildren().add(canvas);
            canvas.widthProperty().bind(animationPane.widthProperty());
            canvas.heightProperty().bind(animationPane.heightProperty());
            animationPane.widthProperty().addListener((obs, old, val) -> recalculatePartyPositions());
            animationPane.heightProperty().addListener((obs, old, val) -> recalculatePartyPositions());

            setupTooltip();
            canvas.setOnMouseMoved(e -> handleMouseMove(e.getX(), e.getY()));
            canvas.setOnMouseExited(e -> hideTooltip());
        }
        gc = canvas.getGraphicsContext2D();

        setupCharts();

        if (scandalTickerBox != null) {
            scandalTickerBox.getStyleClass().add("ticker-container");
        }

        this.controller = new SimulationController(this);
        handleParameterChange(null);
        startVisualTimer();
    }

    // --- Tooltip Logik für die Karte ---
    private void setupTooltip() {
        tooltipBox = new VBox(5);
        tooltipBox.setPadding(new Insets(10));
        tooltipBox.setStyle("-fx-background-color: rgba(30, 30, 35, 0.95); -fx-border-color: #D4AF37; -fx-border-width: 1; -fx-background-radius: 5;");
        tooltipBox.setEffect(new DropShadow(10, Color.BLACK));
        tooltipBox.setVisible(false);
        tooltipBox.setMouseTransparent(true);

        tooltipNameLabel = new Label();
        tooltipNameLabel.setStyle("-fx-text-fill: #D4AF37; -fx-font-weight: bold; -fx-font-size: 14px;");

        tooltipAbbrLabel = new Label();
        tooltipVotersLabel = new Label();
        tooltipPositionLabel = new Label();
        tooltipScandalsLabel = new Label();

        String infoStyle = "-fx-text-fill: #e0e0e0; -fx-font-size: 12px;";
        tooltipAbbrLabel.setStyle(infoStyle);
        tooltipVotersLabel.setStyle(infoStyle);
        tooltipPositionLabel.setStyle(infoStyle);
        tooltipScandalsLabel.setStyle(infoStyle);

        tooltipBox.getChildren().addAll(tooltipNameLabel, tooltipAbbrLabel, new Separator(), tooltipVotersLabel, tooltipPositionLabel, tooltipScandalsLabel);
        if (animationPane != null) animationPane.getChildren().add(tooltipBox);
    }

    private void handleMouseMove(double mouseX, double mouseY) {
        if (controller == null || controller.getParties() == null) return;
        boolean found = false;
        int totalVoters = Math.max(1, controller.getCurrentParameters().getTotalVoterCount());

        for (Party p : controller.getParties()) {
            Point pt = partyPositions.get(p.getName());
            if (pt != null) {
                double share = (double) p.getCurrentSupporterCount() / totalVoters;
                double dynamicRadius = 30.0 + (share * 60.0);
                double dist = Math.sqrt(Math.pow(mouseX - pt.x, 2) + Math.pow(mouseY - pt.y, 2));

                if (dist <= dynamicRadius) {
                    showTooltip(p, mouseX, mouseY);
                    found = true;
                    break;
                }
            }
        }
        if (!found) hideTooltip();
    }

    private void showTooltip(Party p, double x, double y) {
        tooltipNameLabel.setText(p.getName());
        tooltipAbbrLabel.setText("Kürzel: " + p.getAbbreviation());
        tooltipVotersLabel.setText(String.format("Wähler: %,d", p.getCurrentSupporterCount()));
        tooltipPositionLabel.setText("Ausrichtung: " + p.getPoliticalOrientationName());
        tooltipScandalsLabel.setText("Skandale: " + p.getScandalCount());

        double boxWidth = 180;
        double boxHeight = 120;
        double layoutX = x + 15;
        double layoutY = y + 15;

        if (layoutX + boxWidth > animationPane.getWidth()) layoutX = x - boxWidth - 10;
        if (layoutY + boxHeight > animationPane.getHeight()) layoutY = y - boxHeight - 10;

        tooltipBox.setLayoutX(layoutX);
        tooltipBox.setLayoutY(layoutY);
        tooltipBox.setVisible(true);
        tooltipBox.toFront();
    }

    private void hideTooltip() {
        if (tooltipBox != null) tooltipBox.setVisible(false);
    }

    // --- Chart Setup ---
    private void setupCharts() {
        if (historyChart != null) {
            historyChart.setAnimated(false);
            historyChart.setCreateSymbols(false);
            historyChart.setLegendVisible(false);
        }
    }

    // --- Button Actions ---
    @FXML public void handleStartSimulation(ActionEvent event) { if (controller != null) { controller.startSimulation(); updateButtonState(true); } }
    @FXML public void handlePauseSimulation(ActionEvent event) { if (controller != null) { controller.pauseSimulation(); updateButtonState(false); } }
    @FXML public void handleResetSimulation(ActionEvent event) { if (controller != null) { handleParameterChange(null); controller.resetSimulation(); clearVisuals(); updateButtonState(false); if (resetButton != null) resetButton.setDisable(true); } }

    // --- Parameter Logic ---
    @FXML public void handleParameterChange(Event event) {
        if (controller == null) return;
        try {
            int voters = parseIntSafe(voterCountField.getText(), 100000);
            int parties = Math.max(2, Math.min(8, parseIntSafe(partyCountField.getText(), 5)));
            double scandalChance = Math.max(0.0, Math.min(60.0, parseDoubleSafe(scandalChanceField.getText(), 5.0)));
            double budgetFactor = Math.max(0.1, Math.min(10.0, parseDoubleSafe(budgetField.getText(), 500000.0) / 500000.0));

            SimulationParameters params = new SimulationParameters(
                    voters, mediaInfluenceSlider.getValue(), mobilityRateSlider.getValue(),
                    scandalChance, loyaltyMeanSlider.getValue(),
                    controller.getCurrentParameters().getSimulationTicksPerSecond(),
                    randomRangeSlider.getValue(), parties, budgetFactor
            );
            controller.updateAllParameters(params);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private int parseIntSafe(String text, int def) { try { return Integer.parseInt(text.replaceAll("[^0-9]", "")); } catch (Exception e) { return def; } }
    private double parseDoubleSafe(String text, double def) { try { return Double.parseDouble(text.replace(",", ".")); } catch (Exception e) { return def; } }

    @FXML public void handleVoterCountIncrement(ActionEvent event) { adjustIntField(voterCountField, 10000, 1000, 2000000); }
    @FXML public void handleVoterCountDecrement(ActionEvent event) { adjustIntField(voterCountField, -10000, 1000, 2000000); }
    @FXML public void handlePartyCountIncrement(ActionEvent event) { adjustIntField(partyCountField, 1, 2, 8); }
    @FXML public void handlePartyCountDecrement(ActionEvent event) { adjustIntField(partyCountField, -1, 2, 8); }
    @FXML public void handleScandalChanceIncrement(ActionEvent event) { adjustDoubleField(scandalChanceField, 0.5, 0.0, 60.0); }
    @FXML public void handleScandalChanceDecrement(ActionEvent event) { adjustDoubleField(scandalChanceField, -0.5, 0.0, 60.0); }
    @FXML public void handleSpeed1x(ActionEvent event) { controller.updateSimulationSpeed(1); }
    @FXML public void handleSpeed2x(ActionEvent event) { controller.updateSimulationSpeed(2); }
    @FXML public void handleSpeed4x(ActionEvent event) { controller.updateSimulationSpeed(4); }

    private void adjustIntField(TextField field, int delta, int min, int max) { int v = parseIntSafe(field.getText(), min); field.setText(String.valueOf(Math.max(min, Math.min(max, v + delta)))); handleParameterChange(null); }
    private void adjustDoubleField(TextField field, double delta, double min, double max) { double v = parseDoubleSafe(field.getText(), min); field.setText(String.format(Locale.US, "%.1f", Math.max(min, Math.min(max, v + delta)))); handleParameterChange(null); }
    private void updateButtonState(boolean running) { if (startButton != null) startButton.setDisable(running); if (pauseButton != null) pauseButton.setDisable(!running); if (resetButton != null) resetButton.setDisable(running); }

    // === HAUPT UPDATE LOOP ===
    public void updateDashboard(List<Party> parties, List<VoterTransition> transitions, ScandalEvent scandal, int step) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> updateDashboard(parties, transitions, scandal, step));
            return;
        }

        if (step == 0) clearVisuals();

        if (timeStepLabel != null) {
            String status = controller.isRunning() ? "Läuft" : "Pausiert";
            timeStepLabel.setText(String.format("Status: %s | Tick: %d", status, step));
        }

        if (scandal != null) {
            if (eventFeedPane != null) addToVerticalFeed(scandal, step);
            if (scandalTickerBox != null) addScandalToTicker(scandal, step);
        }

        if (historyChart != null && step % 5 == 0) updateHistoryChart(parties, step);
        if (partyPositions.size() != parties.size()) recalculatePartyPositions(parties);
        spawnParticles(transitions);
    }

    // --- "Letzte Meldung" (Vertikal) - OHNE TOOLTIP ---
    private void addToVerticalFeed(ScandalEvent event, int step) {
        VBox feedBox;
        if (eventFeedPane.getChildren().isEmpty()) {
            feedBox = new VBox();
            feedBox.getStyleClass().add("event-feed-container");
            feedBox.prefWidthProperty().bind(eventFeedPane.widthProperty());
            eventFeedPane.getChildren().add(feedBox);
        } else {
            feedBox = (VBox) eventFeedPane.getChildren().get(0);
        }
        feedBox.getChildren().clear();
        feedBox.getChildren().add(createVerticalEventCard(event, step));
    }

    private HBox createVerticalEventCard(ScandalEvent event, int step) {
        VBox leftCol = new VBox();
        leftCol.getStyleClass().add("event-timeline-col");
        leftCol.setAlignment(Pos.TOP_CENTER);
        leftCol.setMinWidth(40);

        String typeStyle = getScandalStyle(event);
        String symbol = getScandalSymbol(event);

        Circle iconBg = new Circle(14);
        iconBg.getStyleClass().addAll("event-icon-bg", typeStyle);
        Text iconSymbol = new Text(symbol);
        iconSymbol.getStyleClass().add("event-icon-symbol");
        StackPane iconStack = new StackPane(iconBg, iconSymbol);
        leftCol.getChildren().add(iconStack);

        VBox rightCol = new VBox(2);
        rightCol.getStyleClass().add("event-content-col");
        HBox.setHgrow(rightCol, Priority.ALWAYS);

        Label timeLbl = new Label("Tick: " + step);
        timeLbl.getStyleClass().add("event-time");
        Label titleLbl = new Label(event.getScandal().getTitle() + " (" + event.getAffectedParty().getAbbreviation() + ")");
        titleLbl.getStyleClass().add("event-title");
        titleLbl.setWrapText(true);
        Label descLbl = new Label("Prognose: -" + (int)(event.getScandal().getStrength() * 50) + "% Beliebtheit.");
        descLbl.getStyleClass().add("event-desc");
        descLbl.setWrapText(true);

        rightCol.getChildren().addAll(timeLbl, titleLbl, descLbl);

        HBox card = new HBox(0);
        card.getStyleClass().add("event-card");
        card.getChildren().addAll(leftCol, rightCol);

        // HIER WURDE DER TOOLTIP ENTFERNT (WIE GEWÜNSCHT)

        return card;
    }

    // --- "Timeline" (Horizontal) - MIT TOOLTIP (INKL. BESCHREIBUNG) ---
    private void addScandalToTicker(ScandalEvent event, int step) {
        scandalTickerBox.setAlignment(Pos.TOP_LEFT);
        if (!scandalTickerBox.getChildren().isEmpty()) {
            Line connector = new Line(0, 0, 50, 0);
            connector.getStyleClass().add("ticker-connector");
            connector.setTranslateY(16);
            scandalTickerBox.getChildren().add(connector);
        }

        String typeStyle = getScandalStyle(event);
        String symbol = getScandalSymbol(event);
        String typeName = (event.getScandal() != null) ? event.getScandal().getType() : "Skandal";

        Circle iconBg = new Circle(16);
        iconBg.getStyleClass().addAll("event-icon-bg", typeStyle);
        Text iconSymbol = new Text(symbol);
        iconSymbol.getStyleClass().add("event-icon-symbol");
        StackPane iconStack = new StackPane(iconBg, iconSymbol);
        iconStack.getStyleClass().add("ticker-item");
        iconStack.setMinSize(32, 32); iconStack.setMaxSize(32, 32);

        Label tickLabel = new Label("Tick " + step);
        tickLabel.getStyleClass().add("ticker-time");
        VBox tickerEntry = new VBox(iconStack, tickLabel);
        tickerEntry.getStyleClass().add("ticker-box");

        String partyName = event.getAffectedParty().getAbbreviation();
        double impact = event.getScandal().getStrength() * 100;

        // --- HIER IST DIE BESCHREIBUNG JETZT ENTHALTEN ---
        String tooltipText = String.format(
                "TICK: %d\nPARTEI: %s\nTYP: %s\n\n%s\n\n%s\n\nAUSWIRKUNG: -%.0f%%",
                step,
                partyName,
                typeName,
                event.getScandal().getTitle(),
                event.getScandal().getDescription(), // <--- BESCHREIBUNG WIEDER EINGEFÜGT
                impact * 0.5
        );

        Tooltip tooltip = new Tooltip(tooltipText);
        tooltip.getStyleClass().add("scandal-tooltip");
        tooltip.setShowDelay(Duration.ZERO);
        tooltip.setShowDuration(Duration.seconds(10));
        Tooltip.install(iconStack, tooltip);

        scandalTickerBox.getChildren().add(tickerEntry);
        scandalTickerBox.applyCss(); scandalTickerBox.layout();
        scandalTickerScroll.layout(); scandalTickerScroll.setHvalue(1.0);
    }

    private String getScandalStyle(ScandalEvent event) {
        if (event.getScandal() == null) return "type-default";
        String type = event.getScandal().getType().toUpperCase();
        if (type.contains("CORRUPTION")) return "type-corruption";
        if (type.contains("FINANCIAL")) return "type-financial";
        if (type.contains("POLITICAL")) return "type-political";
        if (type.contains("PERSONAL")) return "type-personal";
        if (type.contains("SCANDAL")) return "type-scandal";
        return "type-default";
    }

    private String getScandalSymbol(ScandalEvent event) {
        if (event.getScandal() == null) return "!";
        String type = event.getScandal().getType().toUpperCase();
        if (type.contains("CORRUPTION")) return "⚖";
        if (type.contains("FINANCIAL")) return "$";
        if (type.contains("POLITICAL")) return "♟";
        if (type.contains("PERSONAL")) return "☹";
        if (type.contains("SCANDAL")) return "⚠";
        return "!";
    }

    private void updateHistoryChart(List<Party> parties, int step) {
        for (Party p : parties) {
            if (p.getName().equals(SimulationConfig.UNDECIDED_NAME)) continue;
            XYChart.Series<Number, Number> s = historySeriesMap.computeIfAbsent(p.getName(), k -> {
                XYChart.Series<Number, Number> series = new XYChart.Series<>();
                series.setName(p.getAbbreviation());
                historyChart.getData().add(series);
                String hex = p.getColorCode();
                if(!hex.startsWith("#")) hex = "#" + hex;
                series.getNode().setStyle("-fx-stroke: " + hex + "; -fx-stroke-width: 2px;");
                return series;
            });
            s.getData().add(new XYChart.Data<>(step, p.getCurrentSupporterCount()));
            if (s.getData().size() > SimulationConfig.HISTORY_LENGTH) s.getData().remove(0);
        }
    }

    // --- Visuals ---
    private void startVisualTimer() { visualTimer = new AnimationTimer() { @Override public void handle(long now) { renderCanvas(); } }; visualTimer.start(); }
    private void recalculatePartyPositions() { if (controller != null && controller.getParties() != null) recalculatePartyPositions(controller.getParties()); }
    private void recalculatePartyPositions(List<Party> parties) {
        partyPositions.clear();
        double cx = canvas.getWidth() / 2, cy = canvas.getHeight() / 2, radius = Math.min(cx, cy) * 0.75;
        int count = parties.size();
        for (int i = 0; i < count; i++) {
            if (parties.get(i).getName().equals(SimulationConfig.UNDECIDED_NAME)) { partyPositions.put(parties.get(i).getName(), new Point(cx, cy)); continue; }
            double angle = 2 * Math.PI * i / count - Math.PI / 2;
            partyPositions.put(parties.get(i).getName(), new Point(cx + radius * Math.cos(angle), cy + radius * Math.sin(angle)));
        }
    }
    private void spawnParticles(List<VoterTransition> transitions) {
        int limit = 0;
        for (VoterTransition t : transitions) {
            if (limit++ > 50) break;
            Point start = partyPositions.get(t.getOldParty().getName()), end = partyPositions.get(t.getNewParty().getName());
            if (start != null && end != null) {
                String hex = t.getNewParty().getColorCode();
                if(!hex.startsWith("#")) hex = "#" + hex;
                MovingVoter p = particlePool.isEmpty() ? new MovingVoter() : particlePool.pop();
                p.reset(start.x, start.y, end.x, end.y, Color.web(hex));
                activeParticles.add(p);
            }
        }
    }
    private void renderCanvas() {
        double w = canvas.getWidth(), h = canvas.getHeight();
        gc.clearRect(0, 0, w, h);
        if (controller == null) return;
        List<Party> parties = controller.getParties();

        gc.setStroke(Color.web("#D4AF37", 0.15)); gc.setLineWidth(0.8);
        for (int i = 0; i < parties.size(); i++) {
            Point p1 = partyPositions.get(parties.get(i).getName());
            if (p1 == null) continue;
            for (int j = i + 1; j < parties.size(); j++) {
                Point p2 = partyPositions.get(parties.get(j).getName());
                if (p2 != null) gc.strokeLine(p1.x, p1.y, p2.x, p2.y);
            }
        }

        int totalVoters = Math.max(1, controller.getCurrentParameters().getTotalVoterCount());
        for (Party p : parties) {
            Point pt = partyPositions.get(p.getName());
            if (pt != null) {
                String hex = p.getColorCode();
                if(!hex.startsWith("#")) hex = "#" + hex;
                Color c = Color.web(hex);
                double r = 30.0 + ((double) p.getCurrentSupporterCount() / totalVoters * 60.0);
                gc.setFill(new RadialGradient(0, 0, pt.x, pt.y, r, false, CycleMethod.NO_CYCLE, new Stop(0, c.deriveColor(0, 0.8, 0.9, 0.7)), new Stop(1, Color.TRANSPARENT)));
                gc.fillOval(pt.x - r, pt.y - r, r * 2, r * 2);
                gc.setFill(c.brighter()); gc.fillOval(pt.x - 10, pt.y - 10, 20, 20);
                gc.setStroke(Color.web("#D4AF37")); gc.setLineWidth(1.5); gc.strokeOval(pt.x - 10, pt.y - 10, 20, 20);
                gc.setFill(Color.web("#e0e0e0")); gc.setFont(Font.font("Consolas", FontWeight.BOLD, 12)); gc.fillText(p.getAbbreviation(), pt.x - 20, pt.y + 35);
                gc.setFill(Color.web("#D4AF37")); gc.fillText(String.format("%.1f%%", (double) p.getCurrentSupporterCount() / totalVoters * 100), pt.x - 10, pt.y + 48);
            }
        }

        Iterator<MovingVoter> it = activeParticles.iterator();
        gc.setLineWidth(2.5);
        while (it.hasNext()) {
            MovingVoter p = it.next(); p.move();
            double alpha = p.getOpacity();
            for (int i = 0; i < 5; i++) {
                if (alpha * (1 - i/5.0) < 0.05) continue;
                gc.setGlobalAlpha(alpha * (1 - i/5.0)); gc.setStroke(p.color);
                gc.strokeLine(p.x - p.dx*i*1.5, p.y - p.dy*i*1.5, p.x - p.dx*(i*1.5+1), p.y - p.dy*(i*1.5+1));
            }
            if (p.hasArrived()) { it.remove(); particlePool.push(p); }
        }
        gc.setGlobalAlpha(1.0);
    }

    private void clearVisuals() {
        activeParticles.forEach(particlePool::push); activeParticles.clear();
        historySeriesMap.clear(); partyPositions.clear();
        if (historyChart != null) historyChart.getData().clear();
        if (gc != null) gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        if (eventFeedPane != null) eventFeedPane.getChildren().clear();
        if (scandalTickerBox != null) scandalTickerBox.getChildren().clear();
    }
    public void shutdown() { if (controller != null) controller.shutdown(); if (visualTimer != null) visualTimer.stop(); }

    private static class Point { double x, y; Point(double x, double y) { this.x = x; this.y = y; } }
    private static class MovingVoter {
        double x, y, startX, startY, targetX, targetY, dx, dy, progress, speed; Color color; boolean arrived;
        void reset(double sx, double sy, double tx, double ty, Color c) {
            double s = 15.0; startX = sx + (Math.random()-0.5)*s; startY = sy + (Math.random()-0.5)*s;
            targetX = tx + (Math.random()-0.5)*s; targetY = ty + (Math.random()-0.5)*s;
            x = startX; y = startY; color = c; progress = 0; arrived = false; speed = 0.01 + Math.random()*0.015; dx = 0; dy = 0;
        }
        void move() {
            if (arrived) return; progress += speed; if (progress >= 1) { progress = 1; arrived = true; }
            double t = progress * progress * (3 - 2 * progress);
            double nx = startX + (targetX - startX) * t, ny = startY + (targetY - startY) * t;
            dx = nx - x; dy = ny - y; x = nx; y = ny;
        }
        double getOpacity() { return progress < 0.15 ? progress/0.15 : (progress > 0.85 ? (1-progress)/0.15 : 1); }
        boolean hasArrived() { return arrived; }
    }
}
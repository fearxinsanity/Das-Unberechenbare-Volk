package de.schulprojekt.duv.view;

import de.schulprojekt.duv.controller.SimulationController;
import de.schulprojekt.duv.model.core.SimulationParameters;
import de.schulprojekt.duv.model.party.Party;
import de.schulprojekt.duv.model.scandal.ScandalEvent;
import de.schulprojekt.duv.model.voter.VoterTransition;
import de.schulprojekt.duv.util.SimulationConfig;
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
    private Label tooltipNameLabel;
    private Label tooltipAbbrLabel;
    private Label tooltipVotersLabel;
    private Label tooltipPositionLabel;
    private Label tooltipScandalsLabel;

    @FXML
    public void initialize() {
        // Canvas Setup
        canvas = new Canvas(800, 600);
        if (animationPane != null) {
            animationPane.getChildren().add(canvas);
            canvas.widthProperty().bind(animationPane.widthProperty());
            canvas.heightProperty().bind(animationPane.heightProperty());

            // Listener für Resizing
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

        // Controller initialisieren
        this.controller = new SimulationController(this);

        // Initiale Parameter setzen
        handleParameterChange(null);
        startVisualTimer();
    }

    // --- Tooltip Logik ---
    private void setupTooltip() {
        tooltipBox = new VBox(5);
        tooltipBox.setPadding(new Insets(10));
        tooltipBox.setStyle("-fx-background-color: rgba(30, 30, 35, 0.95); " +
                "-fx-border-color: #D4AF37; -fx-border-width: 1; " +
                "-fx-background-radius: 5; -fx-border-radius: 5;");
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

        tooltipBox.getChildren().addAll(
                tooltipNameLabel, tooltipAbbrLabel, new Separator(),
                tooltipVotersLabel, tooltipPositionLabel, tooltipScandalsLabel
        );

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
        tooltipPositionLabel.setText("Ausrichtung: " + getOrientationName(p.getPoliticalPosition()));
        tooltipScandalsLabel.setText("Skandale: " + p.getScandalCount());

        double boxWidth = 180;
        double boxHeight = 120;
        double layoutX = x + 15;
        double layoutY = y + 15;

        if (animationPane != null) {
            if (layoutX + boxWidth > animationPane.getWidth()) layoutX = x - boxWidth - 10;
            if (layoutY + boxHeight > animationPane.getHeight()) layoutY = y - boxHeight - 10;
        }

        tooltipBox.setLayoutX(layoutX);
        tooltipBox.setLayoutY(layoutY);
        tooltipBox.setVisible(true);
        tooltipBox.toFront();
    }

    private void hideTooltip() {
        if (tooltipBox != null) tooltipBox.setVisible(false);
    }

    // Hilfsmethode: Politische Ausrichtung benennen
    private String getOrientationName(double pos) {
        if (pos < 20) return "Sehr Links";
        if (pos < 40) return "Links-Mitte";
        if (pos < 60) return "Zentristisch";
        if (pos < 80) return "Rechts-Mitte";
        return "Sehr Rechts";
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

    // --- Parameter Logic ---
    @FXML
    public void handleParameterChange(Event event) {
        if (controller == null) return;
        try {
            int voters = parseIntSafe(voterCountField.getText(), 100000);
            int parties = parseIntSafe(partyCountField.getText(), 5);
            parties = Math.max(2, Math.min(8, parties));

            double scandalChance = parseDoubleSafe(scandalChanceField.getText(), 5.0);
            scandalChance = Math.max(0.0, Math.min(60.0, scandalChance));

            double budgetInput = parseDoubleSafe(budgetField.getText(), 500000.0);
            double budgetFactor = budgetInput / 500000.0;
            budgetFactor = Math.max(0.1, Math.min(10.0, budgetFactor));

            SimulationParameters current = controller.getCurrentParameters();

            // Neue Parameter erstellen (Konstruktor muss zur neuen Parameter-Klasse passen)
            // Falls dein Constructor anders aussieht, hier anpassen!
            SimulationParameters params = new SimulationParameters();
            params.setTotalVoterCount(voters);
            params.setNumberOfParties(parties);
            params.setScandalChance(scandalChance);
            params.setCampaignBudgetFactor(budgetFactor);
            params.setGlobalMediaInfluence(mediaInfluenceSlider.getValue());
            params.setBaseMobilityRate(mobilityRateSlider.getValue());
            params.setInitialLoyaltyMean(loyaltyMeanSlider.getValue());
            params.setUniformRandomRange(randomRangeSlider.getValue());

            // SimulationTicksPerSecond übernehmen
            params.setSimulationTicksPerSecond(current != null ? current.getSimulationTicksPerSecond() : 60);

            controller.updateAllParameters(params);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int parseIntSafe(String text, int def) {
        try { return Integer.parseInt(text.replaceAll("[^0-9]", "")); }
        catch (Exception e) { return def; }
    }

    private double parseDoubleSafe(String text, double def) {
        try { return Double.parseDouble(text.replace(",", ".")); }
        catch (Exception e) { return def; }
    }

    @FXML public void handleVoterCountIncrement(ActionEvent event) { adjustIntField(voterCountField, 10000, 1000, 2000000); }
    @FXML public void handleVoterCountDecrement(ActionEvent event) { adjustIntField(voterCountField, -10000, 1000, 2000000); }
    @FXML public void handlePartyCountIncrement(ActionEvent event) { adjustIntField(partyCountField, 1, 2, 8); }
    @FXML public void handlePartyCountDecrement(ActionEvent event) { adjustIntField(partyCountField, -1, 2, 8); }

    @FXML public void handleScandalChanceIncrement(ActionEvent event) { adjustDoubleField(scandalChanceField, 0.5, 0.0, 60.0); }
    @FXML public void handleScandalChanceDecrement(ActionEvent event) { adjustDoubleField(scandalChanceField, -0.5, 0.0, 60.0); }

    @FXML public void handleSpeed1x(ActionEvent event) { controller.updateSimulationSpeed(1); }
    @FXML public void handleSpeed2x(ActionEvent event) { controller.updateSimulationSpeed(2); }
    @FXML public void handleSpeed4x(ActionEvent event) { controller.updateSimulationSpeed(4); }

    private void adjustIntField(TextField field, int delta, int min, int max) {
        int current = parseIntSafe(field.getText(), min);
        int newVal = Math.max(min, Math.min(max, current + delta));
        field.setText(String.valueOf(newVal));
        handleParameterChange(null);
    }

    private void adjustDoubleField(TextField field, double delta, double min, double max) {
        double current = parseDoubleSafe(field.getText(), min);
        double newVal = Math.max(min, Math.min(max, current + delta));
        field.setText(String.format(Locale.US, "%.1f", newVal));
        handleParameterChange(null);
    }

    private void updateButtonState(boolean running) {
        if (startButton != null) startButton.setDisable(running);
        if (pauseButton != null) pauseButton.setDisable(!running);
        if (resetButton != null) resetButton.setDisable(running);
    }

    // === HAUPT UPDATE LOOP ===
    public void updateDashboard(List<Party> parties, List<VoterTransition> transitions, ScandalEvent scandal, int step) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> updateDashboard(parties, transitions, scandal, step));
            return;
        }

        // Reset Handling
        if (step == 0) {
            clearVisuals();
        }

        if (timeStepLabel != null) {
            String status = controller.isRunning() ? "Läuft" : "Pausiert";
            timeStepLabel.setText(String.format("Status: %s | Tick: %d", status, step));
        }

        // --- SKANDAL ---
        if (scandal != null) {
            if (eventFeedPane != null) addToVerticalFeed(scandal, step);
            if (scandalTickerBox != null) addScandalToTicker(scandal, step);
        }

        // --- CHART ---
        if (historyChart != null && step % 5 == 0) {
            updateHistoryChart(parties, step);
        }

        // --- CANVAS ---
        if (partyPositions.size() != parties.size()) {
            recalculatePartyPositions(parties);
        }
        spawnParticles(transitions);
    }

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
        feedBox.getChildren().clear(); // Nur neueste anzeigen
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

        // getTitle -> getName (angepasst an neue Struktur)
        Label titleLbl = new Label(event.getScandal().getName() + " (" + event.getAffectedParty().getAbbreviation() + ")");
        titleLbl.getStyleClass().add("event-title");
        titleLbl.setWrapText(true);

        Label descLbl = new Label("Prognose: -" + (int)(event.getScandal().getStrength() * 50) + "% Beliebtheit.");
        descLbl.getStyleClass().add("event-desc");
        descLbl.setWrapText(true);

        rightCol.getChildren().addAll(timeLbl, titleLbl, descLbl);

        HBox card = new HBox(0);
        card.getStyleClass().add("event-card");
        card.getChildren().addAll(leftCol, rightCol);
        return card;
    }

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

        Circle iconBg = new Circle(16);
        iconBg.getStyleClass().addAll("event-icon-bg", typeStyle);
        Text iconSymbol = new Text(symbol);
        iconSymbol.getStyleClass().add("event-icon-symbol");

        StackPane iconStack = new StackPane(iconBg, iconSymbol);
        iconStack.getStyleClass().add("ticker-item");
        iconStack.setMinSize(32, 32);
        iconStack.setMaxSize(32, 32);

        Label tickLabel = new Label("Tick " + step);
        tickLabel.getStyleClass().add("ticker-time");

        VBox tickerEntry = new VBox(iconStack, tickLabel);
        tickerEntry.getStyleClass().add("ticker-box");

        String tooltipText = String.format("PARTEI: %s\n%s\n-%.0f%%",
                event.getAffectedParty().getAbbreviation(),
                event.getScandal().getName(),
                event.getScandal().getStrength() * 100);

        Tooltip tooltip = new Tooltip(tooltipText);
        Tooltip.install(iconStack, tooltip);

        scandalTickerBox.getChildren().add(tickerEntry);

        // Auto Scroll
        scandalTickerBox.applyCss();
        scandalTickerBox.layout();
        scandalTickerScroll.layout();
        scandalTickerScroll.setHvalue(1.0);
    }

    private String getScandalStyle(ScandalEvent event) {
        // Einfache Heuristik anhand des Namens, da Type evtl. in CSV fehlt
        String name = event.getScandal().getName().toLowerCase();
        if (name.contains("korruption") || name.contains("bestechung")) return "type-corruption";
        if (name.contains("steuer") || name.contains("geld") || name.contains("finanz")) return "type-financial";
        if (name.contains("lüge") || name.contains("versprechen")) return "type-political";
        return "type-default";
    }

    private String getScandalSymbol(ScandalEvent event) {
        String name = event.getScandal().getName().toLowerCase();
        if (name.contains("korruption")) return "⚖";
        if (name.contains("geld")) return "$";
        if (name.contains("lüge")) return "♟";
        return "!";
    }

    private void updateHistoryChart(List<Party> parties, int step) {
        for (Party p : parties) {
            if (p.getName().equals(SimulationConfig.UNDECIDED_NAME)) continue;
            XYChart.Series<Number, Number> series = historySeriesMap.computeIfAbsent(p.getName(), k -> {
                XYChart.Series<Number, Number> s = new XYChart.Series<>();
                s.setName(p.getAbbreviation());
                historyChart.getData().add(s);
                // Style via Helper, da Color Objekt
                String hex = toHexString(p.getColor());
                s.getNode().setStyle("-fx-stroke: " + hex + "; -fx-stroke-width: 2px;");
                return s;
            });
            series.getData().add(new XYChart.Data<>(step, p.getCurrentSupporterCount()));
            if (series.getData().size() > SimulationConfig.HISTORY_LENGTH) {
                series.getData().remove(0);
            }
        }
    }

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
                // Farbe direkt aus Objekt
                Color c = t.getNewParty().getColor();
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

        // Linien
        if (controller != null && !controller.getParties().isEmpty()) {
            gc.setStroke(Color.web("#D4AF37", 0.15));
            gc.setLineWidth(0.8);
            List<Party> parties = controller.getParties();
            for (int i = 0; i < parties.size(); i++) {
                Point p1 = partyPositions.get(parties.get(i).getName());
                if (p1 == null) continue;
                for (int j = i + 1; j < parties.size(); j++) {
                    Point p2 = partyPositions.get(parties.get(j).getName());
                    if (p2 == null) continue;
                    gc.strokeLine(p1.x, p1.y, p2.x, p2.y);
                }
            }
        }

        // Parteien
        if (controller != null) {
            int totalVoters = Math.max(1, controller.getCurrentParameters().getTotalVoterCount());
            for (Party p : controller.getParties()) {
                Point pt = partyPositions.get(p.getName());
                if (pt != null) {
                    Color pColor = p.getColor();
                    Color mysteryColor = pColor.deriveColor(0, 0.8, 0.9, 1.0);
                    double share = (double) p.getCurrentSupporterCount() / totalVoters;
                    double dynamicRadius = 30.0 + (share * 60.0);

                    RadialGradient glow = new RadialGradient(
                            0, 0, pt.x, pt.y, dynamicRadius, false, CycleMethod.NO_CYCLE,
                            new Stop(0.0, mysteryColor.deriveColor(0, 1.0, 1.0, 0.7)),
                            new Stop(0.6, mysteryColor.deriveColor(0, 1.0, 0.6, 0.2)),
                            new Stop(1.0, Color.TRANSPARENT)
                    );
                    gc.setFill(glow);
                    gc.fillOval(pt.x - dynamicRadius, pt.y - dynamicRadius, dynamicRadius * 2, dynamicRadius * 2);

                    gc.setGlobalAlpha(1.0);
                    gc.setFill(mysteryColor.brighter());
                    gc.fillOval(pt.x - 10, pt.y - 10, 20, 20);

                    gc.setStroke(Color.web("#D4AF37"));
                    gc.setLineWidth(1.5);
                    gc.strokeOval(pt.x - 10, pt.y - 10, 20, 20);

                    gc.setFill(Color.web("#e0e0e0"));
                    gc.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
                    gc.fillText(p.getAbbreviation(), pt.x - 20, pt.y + 35);

                    gc.setFill(Color.web("#D4AF37"));
                    gc.fillText(String.format("%.1f%%", share * 100), pt.x - 10, pt.y + 48);
                }
            }
        }

        // Partikel
        Iterator<MovingVoter> it = activeParticles.iterator();
        gc.setLineWidth(2.5);
        while (it.hasNext()) {
            MovingVoter p = it.next();
            p.move();
            double baseAlpha = p.getOpacity();
            double trailLength = 5;
            for (int i = 0; i < trailLength; i++) {
                double segmentAlpha = baseAlpha * (1.0 - ((double)i / trailLength));
                if (segmentAlpha < 0.05) continue;
                gc.setGlobalAlpha(segmentAlpha);
                gc.setStroke(p.color.deriveColor(0, 1.0, 1.0, 1.0));
                double backX = p.x - (p.dx * i * 1.5);
                double backY = p.y - (p.dy * i * 1.5);
                gc.strokeLine(backX, backY, backX - p.dx, backY - p.dy);
            }
            if (p.hasArrived()) {
                it.remove();
                particlePool.push(p);
            }
        }
        gc.setGlobalAlpha(1.0);
    }

    private void clearVisuals() {
        activeParticles.forEach(particlePool::push);
        activeParticles.clear();
        historySeriesMap.clear();
        partyPositions.clear();
        if (historyChart != null) historyChart.getData().clear();
        if (gc != null) gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        if (eventFeedPane != null) eventFeedPane.getChildren().clear();
        if (scandalTickerBox != null) scandalTickerBox.getChildren().clear();
    }

    // Hilfsmethode: Color zu Hex String
    private String toHexString(Color c) {
        return String.format("#%02X%02X%02X",
                (int) (c.getRed() * 255),
                (int) (c.getGreen() * 255),
                (int) (c.getBlue() * 255));
    }

    public void shutdown() {
        if (controller != null) controller.shutdown();
        if (visualTimer != null) visualTimer.stop();
    }

    // --- Innere Klassen ---
    private static class Point {
        double x, y;
        Point(double x, double y) { this.x = x; this.y = y; }
    }

    private static class MovingVoter {
        double startX, startY, targetX, targetY, x, y, dx, dy, progress, speedStep;
        Color color;
        boolean arrived;

        void reset(double sx, double sy, double tx, double ty, Color c) {
            double spread = 15.0;
            this.startX = sx + (Math.random() - 0.5) * spread;
            this.startY = sy + (Math.random() - 0.5) * spread;
            this.targetX = tx + (Math.random() - 0.5) * spread;
            this.targetY = ty + (Math.random() - 0.5) * spread;
            this.x = startX; this.y = startY; this.color = c;
            this.progress = 0.0; this.arrived = false;
            this.speedStep = 0.010 + (Math.random() * 0.015);
            this.dx = 0; this.dy = 0;
        }

        void move() {
            if (arrived) return;
            progress += speedStep;
            if (progress >= 1.0) { progress = 1.0; arrived = true; }
            double t = progress * progress * (3 - 2 * progress);
            double newX = startX + (targetX - startX) * t;
            double newY = startY + (targetY - startY) * t;
            this.dx = newX - x; this.dy = newY - y;
            this.x = newX; this.y = newY;
        }

        double getOpacity() {
            if (progress < 0.15) return progress / 0.15;
            else if (progress > 0.85) return (1.0 - progress) / 0.15;
            return 1.0;
        }

        boolean hasArrived() { return arrived; }
    }
}
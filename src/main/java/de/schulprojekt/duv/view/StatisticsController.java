package de.schulprojekt.duv.view;

import de.schulprojekt.duv.model.party.Party;
import de.schulprojekt.duv.util.SimulationConfig;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Controller for the detailed post-simulation statistics view.
 * Displays history, vote distribution, scandals, and budget usage.
 * Includes REAL-TIME HOST SYSTEM telemetry (Real CPU/RAM usage).
 */
public class StatisticsController {

    // --- CONSTANTS ---
    private static final String STYLE_PIE_COLOR = "-fx-pie-color: %s;";
    private static final String STYLE_BAR_FILL = "-fx-bar-fill: %s;";
    private static final String STYLE_LEGEND_SYMBOL = "-fx-background-color: %s;";
    private static final String FORMAT_TOOLTIP_PIE = "FACTION: %s\nSIZE: %.0f\nQUOTA: %.1f%%";
    private static final String FORMAT_TOOLTIP_SCANDAL = "TARGET: %s\nINCIDENTS: %s";
    private static final String FORMAT_TOOLTIP_BUDGET = "TARGET: %s\nBUDGET: %.2f M€";

    // --- FXML Components ---
    @FXML private LineChart<Number, Number> historyChart;
    @FXML private PieChart distributionChart;
    @FXML private BarChart<String, Number> scandalChart;
    @FXML private BarChart<String, Number> budgetChart;
    @FXML private Label totalTicksLabel;

    // --- Status Labels (Pulse) ---
    @FXML private Label statusLabelArchived;
    @FXML private Label statusLabelCalculated;
    @FXML private Label statusLabelReview;
    @FXML private Label statusLabelVerified;

    // --- Telemetry Labels ---
    @FXML private Label cpuLabel;
    @FXML private Label gpuLabel;
    @FXML private Label serverLoadLabel;
    @FXML private Label ramLabel;
    @FXML private Label uptimeLabel;

    // --- State ---
    private Parent dashboardRoot;
    private ScheduledExecutorService telemetryExecutor;

    // --- Initialization ---

    public void initData(List<Party> parties, ObservableList<XYChart.Series<Number, Number>> historyData, int currentTick, Parent dashboardRoot) {
        this.dashboardRoot = dashboardRoot;
        this.totalTicksLabel.setText("ANALYSIS COMPLETE (TICKS: " + currentTick + ")");

        // 1. Charts (Mit "Live Trace" Animation - sauber & technisch)
        setupHistoryChartAnimated(historyData);
        setupDistributionChart(parties);
        setupScandalChart(parties);
        setupBudgetChart(parties);

        // 2. Animationen (Sanftes Pulsieren)
        startStatusPulse(statusLabelArchived);
        startStatusPulse(statusLabelCalculated);
        startStatusPulse(statusLabelReview);
        startStatusPulse(statusLabelVerified);

        // 3. Static Hardware Info
        fetchStaticHardwareInfo();

        // 4. Real-Time Monitor start
        startTelemetryService();
    }

    // --- Animation Helper ---

    private void setupHistoryChartAnimated(ObservableList<XYChart.Series<Number, Number>> historyData) {
        historyChart.getData().clear();

        for (XYChart.Series<Number, Number> sourceSeries : historyData) {
            XYChart.Series<Number, Number> newSeries = new XYChart.Series<>();
            newSeries.setName(sourceSeries.getName());
            historyChart.getData().add(newSeries);

            if (sourceSeries.getNode() != null) {
                String style = sourceSeries.getNode().getStyle();
                runOnNode(newSeries, node -> node.setStyle(style));
            }
            runOnNode(newSeries, node -> installTooltipOnNode(node, "TRACE: " + newSeries.getName()));

            Timeline trace = new Timeline();
            int delayCounter = 0;

            for (XYChart.Data<Number, Number> data : sourceSeries.getData()) {
                KeyFrame kf = new KeyFrame(Duration.millis(delayCounter), ignored -> {
                    XYChart.Data<Number, Number> point = new XYChart.Data<>(data.getXValue(), data.getYValue());
                    newSeries.getData().add(point);
                });
                trace.getKeyFrames().add(kf);
                delayCounter += 20;
            }

            trace.setDelay(Duration.millis(500));
            trace.play();
        }
    }

    private void startStatusPulse(Node node) {
        if (node == null) return;

        FadeTransition ft = new FadeTransition(Duration.seconds(1.5), node);
        ft.setFromValue(1.0);
        ft.setToValue(0.4);
        ft.setAutoReverse(true);
        ft.setCycleCount(Animation.INDEFINITE);
        ft.play();
    }

    // --- Hardware Info (Static) ---

    private void fetchStaticHardwareInfo() {
        // Versuche echten CPU Namen zu holen
        String cpuName = System.getenv("PROCESSOR_IDENTIFIER");
        if (cpuName == null || cpuName.isBlank()) {
            cpuName = System.getProperty("os.arch") + " PROCESSOR";
        }
        // Kürzen für UI
        if (cpuName.length() > 28) cpuName = cpuName.substring(0, 28) + "...";

        if (cpuLabel != null) cpuLabel.setText(cpuName);

        // GPU Pipeline Info (Standard Java kann keine GPU Load % lesen)
        String gpuText = "JFX DEFAULT";
        try {
            // Prism ist der JavaFX Renderer. Zeigt z.B. "D3D" (DirectX) oder "ES2" (OpenGL)
            Object prism = System.getProperty("prism.order");
            if (prism == null) prism = System.getProperty("prism.verbose");
            if (prism != null) gpuText = "PIPELINE: " + prism.toString().toUpperCase();
        } catch (Exception ignored) {}

        if (gpuLabel != null) gpuLabel.setText(gpuText);
    }

    // --- Telemetry Logic (Real-Time HOST DATA) ---

    private void startTelemetryService() {
        if (telemetryExecutor != null && !telemetryExecutor.isShutdown()) return;

        telemetryExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Telemetry-Monitor");
            t.setDaemon(true);
            return t;
        });

        telemetryExecutor.scheduleAtFixedRate(this::updateSystemTelemetry, 0, 1000, TimeUnit.MILLISECONDS);
    }

    private void stopTelemetryService() {
        if (telemetryExecutor != null) {
            telemetryExecutor.shutdownNow();
            telemetryExecutor = null;
        }
    }

    private void updateSystemTelemetry() {
        // Wir nutzen com.sun.management Schnittstellen für echte Host-Daten
        // (Das funktioniert auf Standard Oracle/OpenJDK JVMs)

        String ramText = getRealSystemRam();
        String loadText = getRealCpuLoad();
        String timeText = getUptimeText();

        Platform.runLater(() -> {
            if (ramLabel != null) ramLabel.setText(ramText);
            if (serverLoadLabel != null) serverLoadLabel.setText(loadText);
            if (uptimeLabel != null) uptimeLabel.setText(timeText);
        });
    }

    // --- Helper Methods for Real Data ---

    /**
     * Liest den ECHTEN physischen RAM des Host-PCs aus.
     */
    private String getRealSystemRam() {
        try {
            java.lang.management.OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();

            // Check ob wir auf die erweiterte Bean zugreifen können
            if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOsBean) {
                long totalRamBytes = sunOsBean.getTotalMemorySize(); // Gesamt RAM des PCs
                long freeRamBytes = sunOsBean.getFreeMemorySize();   // Freier RAM
                long usedRamBytes = totalRamBytes - freeRamBytes;

                double totalGB = totalRamBytes / (1024.0 * 1024.0 * 1024.0);
                double usedGB = usedRamBytes / (1024.0 * 1024.0 * 1024.0);

                return String.format(Locale.US, "%.1f / %.1f GB (PHYSICAL)", usedGB, totalGB);
            }
        } catch (Exception ignored) {
            // Fallback auf Java Heap, falls Zugriff verweigert
        }

        // Fallback: Java Heap
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        double usedMB = memoryBean.getHeapMemoryUsage().getUsed() / (1024.0 * 1024.0);
        return String.format(Locale.US, "JVM HEAP: %.1f MB", usedMB);
    }

    /**
     * Liest die ECHTE CPU-Last des Systems aus (nicht nur Prozess).
     */
    private String getRealCpuLoad() {
        try {
            java.lang.management.OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();

            if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOsBean) {
                double systemLoad = sunOsBean.getCpuLoad(); // Wert von 0.0 bis 1.0
                int cores = osBean.getAvailableProcessors();

                if (systemLoad < 0) return "CALCULATING..."; // Erster Tick ist oft -1

                return String.format(Locale.US, "LOAD: %.1f%% (CORES: %d)", systemLoad * 100, cores);
            }
        } catch (Exception ignored) {}

        // Fallback: Load Average (funktioniert meist nur unter Linux/Mac gut)
        double load = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
        if (load < 0) return "CPU ACTIVE";
        return String.format(Locale.US, "AVG: %.2f", load);
    }

    private String getUptimeText() {
        long uptimeMillis = ManagementFactory.getRuntimeMXBean().getUptime();
        long s = (uptimeMillis / 1000) % 60;
        long m = (uptimeMillis / (1000 * 60)) % 60;
        long h = (uptimeMillis / (1000 * 60 * 60));
        return String.format("SESSION: %02d:%02d:%02d", h, m, s);
    }

    // --- Chart Setup Logic (Standard) ---

    private void setupDistributionChart(List<Party> parties) {
        double totalVotes = parties.stream().mapToDouble(Party::getCurrentSupporterCount).sum();
        for (Party p : parties) {
            PieChart.Data data = new PieChart.Data(p.getAbbreviation(), p.getCurrentSupporterCount());
            distributionChart.getData().add(data);
            double percent = (totalVotes > 0) ? (data.getPieValue() / totalVotes) * 100.0 : 0.0;
            String info = String.format(FORMAT_TOOLTIP_PIE, p.getName(), data.getPieValue(), percent);
            runOnNode(data, node -> {
                installTooltipOnNode(node, info);
                String color = getPartyColorString(p);
                node.setStyle(String.format(STYLE_PIE_COLOR, color));
            });
        }
        fixPieLegendColors(parties);
    }

    private void setupScandalChart(List<Party> parties) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (Party p : parties) {
            if (p.getName().equals(SimulationConfig.UNDECIDED_NAME)) continue;
            XYChart.Data<String, Number> data = new XYChart.Data<>(p.getAbbreviation(), p.getScandalCount());
            series.getData().add(data);
            String color = "#" + p.getColorCode();
            runOnNode(data, node -> {
                node.setStyle(String.format(STYLE_BAR_FILL, color));
                installTooltipOnNode(node, String.format(FORMAT_TOOLTIP_SCANDAL, p.getName(), data.getYValue()));
            });
        }
        scandalChart.getData().add(series);
    }

    private void setupBudgetChart(List<Party> parties) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (Party p : parties) {
            if (p.getName().equals(SimulationConfig.UNDECIDED_NAME)) continue;
            double budgetM = p.getCampaignBudget() / 1_000_000.0;
            XYChart.Data<String, Number> data = new XYChart.Data<>(p.getAbbreviation(), budgetM);
            series.getData().add(data);
            String color = "#" + p.getColorCode();
            runOnNode(data, node -> {
                node.setStyle(String.format(STYLE_BAR_FILL, color));
                installTooltipOnNode(node, String.format(FORMAT_TOOLTIP_BUDGET, p.getName(), data.getYValue().doubleValue()));
            });
        }
        budgetChart.getData().add(series);
    }

    // --- Helper Methods ---

    private void fixPieLegendColors(List<Party> parties) {
        Platform.runLater(() -> {
            Node legend = distributionChart.lookup(".chart-legend");
            if (legend instanceof Pane pane) {
                for (Node item : pane.getChildren()) {
                    if (item instanceof Label label) {
                        Party p = findPartyByAbbr(parties, label.getText());
                        if (p != null && label.getGraphic() != null) {
                            String color = getPartyColorString(p);
                            label.getGraphic().setStyle(String.format(STYLE_LEGEND_SYMBOL, color));
                        }
                    }
                }
            }
        });
    }

    private void runOnNode(Object chartData, Consumer<Node> action) {
        Node node = null;
        javafx.beans.property.ReadOnlyObjectProperty<Node> nodeProperty = null;
        if (chartData instanceof XYChart.Series<?, ?> series) {
            node = series.getNode();
            nodeProperty = series.nodeProperty();
        } else if (chartData instanceof XYChart.Data<?, ?> data) {
            node = data.getNode();
            nodeProperty = data.nodeProperty();
        } else if (chartData instanceof PieChart.Data data) {
            node = data.getNode();
            nodeProperty = data.nodeProperty();
        }
        if (node != null) action.accept(node);
        if (nodeProperty != null) {
            nodeProperty.addListener((ignored, ignoredOld, newNode) -> {
                if (newNode != null) action.accept(newNode);
            });
        }
    }

    private void installTooltipOnNode(Node node, String text) {
        Tooltip t = new Tooltip(text);
        t.setShowDelay(Duration.millis(50));
        t.setHideDelay(Duration.millis(200));
        Tooltip.install(node, t);
    }

    private Party findPartyByAbbr(List<Party> parties, String abbr) {
        return parties.stream().filter(p -> p.getAbbreviation().equals(abbr)).findFirst().orElse(null);
    }

    private String getPartyColorString(Party p) {
        return p.getName().equals(SimulationConfig.UNDECIDED_NAME) ? "#666666" : "#" + p.getColorCode();
    }

    // --- Navigation ---
    @FXML
    public void handleBackToDashboard(ActionEvent event) {
        stopTelemetryService();
        if (dashboardRoot != null && event.getSource() instanceof Node sourceNode) {
            sourceNode.getScene().setRoot(dashboardRoot);
        }
    }
}
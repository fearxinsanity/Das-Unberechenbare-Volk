package de.schulprojekt.duv.view;

import de.schulprojekt.duv.model.party.Party;
import de.schulprojekt.duv.util.SimulationConfig;
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
import java.lang.management.OperatingSystemMXBean;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Controller for the detailed post-simulation statistics view.
 * Displays history, vote distribution, scandals, and budget usage.
 * Includes REAL-TIME system telemetry (RAM/CPU/UPTIME/GPU-Pipeline).
 */
public class StatisticsController {

    // ... (Konstanten wie zuvor) ...
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

    // --- Telemetry Labels ---
    @FXML private Label cpuLabel;       // NEU
    @FXML private Label gpuLabel;       // NEU
    @FXML private Label serverLoadLabel;
    @FXML private Label ramLabel;       // Umbenannt von memoryUsageLabel
    @FXML private Label uptimeLabel;

    // --- State ---
    private Parent dashboardRoot;
    private ScheduledExecutorService telemetryExecutor;

    // --- Initialization ---

    public void initData(List<Party> parties, ObservableList<XYChart.Series<Number, Number>> historyData, int currentTick, Parent dashboardRoot) {
        this.dashboardRoot = dashboardRoot;
        this.totalTicksLabel.setText("ANALYSIS COMPLETE (TICKS: " + currentTick + ")");

        setupHistoryChart(historyData);
        setupDistributionChart(parties);
        setupScandalChart(parties);
        setupBudgetChart(parties);

        // Initial Hardware Info (Static Data)
        fetchStaticHardwareInfo();

        // Start Real-Time Monitor
        startTelemetryService();
    }

    // --- Hardware Info (Static) ---

    private void fetchStaticHardwareInfo() {
        // 1. CPU Name Detection
        String cpuName = System.getenv("PROCESSOR_IDENTIFIER");
        if (cpuName == null || cpuName.isBlank()) {
            cpuName = System.getProperty("os.arch"); // Fallback: z.B. "amd64"
        }
        // Cleanup String (zu lange Strings kürzen für UI)
        if (cpuName.length() > 25) cpuName = cpuName.substring(0, 25) + "...";

        final String finalCpu = cpuName;
        if (cpuLabel != null) cpuLabel.setText(finalCpu);

        // 2. GPU / Pipeline Detection
        // JavaFX rendert über Prism. Wir können versuchen, die Pipeline zu erraten oder
        // einfach anzeigen, dass sie aktiv ist.
        String gpuText = "JFX ACCELERATED";
        try {
            // Ein kleiner Hack, oft verrät die System-Property die Pipeline (d3d, es2, sw)
            String prismOrder = System.getProperty("prism.order");
            if (prismOrder != null) gpuText = "PIPELINE: " + prismOrder.toUpperCase();
        } catch (Exception ignored) {}

        if (gpuLabel != null) gpuLabel.setText(gpuText);
    }

    // --- Telemetry Logic (Real-Time) ---

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
        // 1. RAM Calculation (Used / Total)
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long usedHeap = memoryBean.getHeapMemoryUsage().getUsed();
        double usedMB = usedHeap / (1024.0 * 1024.0);
        double usedGB = usedMB / 1024.0;

        double totalGB = 0.0;
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            // Versuch auf die Sun-Implementierung zu casten, um physischen RAM zu bekommen
            if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOsBean) {
                long totalPhysical = sunOsBean.getTotalMemorySize(); // getTotalPhysicalMemorySize() in älteren JDKs
                totalGB = totalPhysical / (1024.0 * 1024.0 * 1024.0);
            }
        } catch (Exception e) {
            // Fallback falls Zugriff verweigert oder Methode nicht existiert
        }

        String ramText;
        if (totalGB > 0) {
            // Zeige: 0.5 / 16.0 GB
            ramText = String.format(Locale.US, "%.1f / %.1f GB", usedGB, totalGB);
        } else {
            // Fallback nur Heap
            ramText = String.format(Locale.US, "HEAP: %.1f MB", usedMB);
        }

        // 2. CPU Load
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        double load = osBean.getSystemLoadAverage();
        int cores = osBean.getAvailableProcessors();
        String loadText;

        if (load < 0) {
            // Windows liefert oft -1 für Load Average. Wir zeigen dann Cores an.
            loadText = "ACTIVE (CORES: " + cores + ")";
        } else {
            loadText = String.format(Locale.US, "%.1f%% (CORES: %d)", load * 100, cores);
        }

        // 3. Uptime
        long uptimeMillis = ManagementFactory.getRuntimeMXBean().getUptime();
        long s = (uptimeMillis / 1000) % 60;
        long m = (uptimeMillis / (1000 * 60)) % 60;
        long h = (uptimeMillis / (1000 * 60 * 60));
        String timeText = String.format("SESSION: %02d:%02d:%02d", h, m, s);

        // Update UI
        Platform.runLater(() -> {
            if (ramLabel != null) ramLabel.setText(ramText);
            if (serverLoadLabel != null) serverLoadLabel.setText(loadText);
            if (uptimeLabel != null) uptimeLabel.setText(timeText);
        });
    }

    // --- Chart Setup Logic (Unverändert) ---
    private void setupHistoryChart(ObservableList<XYChart.Series<Number, Number>> historyData) {
        for (XYChart.Series<Number, Number> sourceSeries : historyData) {
            XYChart.Series<Number, Number> newSeries = new XYChart.Series<>();
            newSeries.setName(sourceSeries.getName());
            for (XYChart.Data<Number, Number> data : sourceSeries.getData()) {
                newSeries.getData().add(new XYChart.Data<>(data.getXValue(), data.getYValue()));
            }
            historyChart.getData().add(newSeries);
            if (sourceSeries.getNode() != null) {
                String style = sourceSeries.getNode().getStyle();
                runOnNode(newSeries, node -> node.setStyle(style));
            }
            runOnNode(newSeries, node -> installTooltipOnNode(node, "HISTORY_TRACE: " + newSeries.getName()));
        }
    }

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
        if (chartData instanceof XYChart.Series<?,?> series) {
            node = series.getNode();
            nodeProperty = series.nodeProperty();
        } else if (chartData instanceof XYChart.Data<?,?> data) {
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
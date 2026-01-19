package de.schulprojekt.duv.view.controllers;

import de.schulprojekt.duv.model.party.Party;
import de.schulprojekt.duv.util.config.SimulationConfig;
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
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Controller for the post-simulation statistics view.
 * Displays history, distribution, and real-time telemetry.
 * @author Nico Hoffmann
 * @version 1.1
 */
public class StatisticsController {

    // ========================================
    // Static Variables
    // ========================================

    private static final String STYLE_PIE_COLOR = "-fx-pie-color: %s;";
    private static final String STYLE_BAR_FILL = "-fx-bar-fill: %s;";
    private static final String STYLE_LEGEND_SYMBOL = "-fx-background-color: %s;";
    private static final String FORMAT_TOOLTIP_PIE = "FACTION: %s\nSIZE: %.0f\nQUOTA: %.1f%%";
    private static final String FORMAT_TOOLTIP_SCANDAL = "TARGET: %s\nINCIDENTS: %s";
    private static final String FORMAT_TOOLTIP_BUDGET = "TARGET: %s\nBUDGET: %.2f M€";

    // ========================================
    // Instance Variables
    // ========================================

    @FXML private LineChart<Number, Number> historyChart;
    @FXML private PieChart distributionChart;
    @FXML private BarChart<String, Number> scandalChart;
    @FXML private BarChart<String, Number> budgetChart;
    @FXML private Label totalTicksLabel;

    @FXML private Label statusLabelArchived;
    @FXML private Label statusLabelCalculated;
    @FXML private Label statusLabelReview;
    @FXML private Label statusLabelVerified;

    @FXML private Label cpuLabel;
    @FXML private Label gpuLabel;
    @FXML private Label serverLoadLabel;
    @FXML private Label ramLabel;
    @FXML private Label uptimeLabel;

    private Parent dashboardRoot;
    private ScheduledExecutorService telemetryExecutor;

    // ========================================
    // Constructors
    // ========================================

    public StatisticsController() {
    }

    // ========================================
    // Getter Methods
    // ========================================

    // ========================================
    // Setter Methods
    // ========================================

    // ========================================
    // Business Logic Methods
    // ========================================

    /**
     * Initializes the controller with data from the simulation.
     * @param parties list of participating parties
     * @param historyData recorded simulation history
     * @param currentTick final tick count
     * @param dashboardRoot reference to return to dashboard
     */
    public void initData(List<Party> parties, ObservableList<XYChart.Series<Number, Number>> historyData, int currentTick, Parent dashboardRoot) {
        this.dashboardRoot = dashboardRoot;
        this.totalTicksLabel.setText("ANALYSIS COMPLETE (TICKS: " + currentTick + ")");

        setupHistoryChartAnimated(historyData);
        setupDistributionChart(parties);
        setupScandalChart(parties);
        setupBudgetChart(parties);

        startStatusPulse(statusLabelArchived);
        startStatusPulse(statusLabelCalculated);
        startStatusPulse(statusLabelReview);
        startStatusPulse(statusLabelVerified);

        fetchStaticHardwareInfo();
        startTelemetryService();
    }

    public void startTelemetryService() {
        if (telemetryExecutor != null && !telemetryExecutor.isShutdown()) return;

        telemetryExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Telemetry-Monitor");
            t.setDaemon(true);
            return t;
        });

        telemetryExecutor.scheduleAtFixedRate(this::updateSystemTelemetry, 0, 1000, TimeUnit.MILLISECONDS);
    }

    public void stopTelemetryService() {
        if (telemetryExecutor != null) {
            telemetryExecutor.shutdownNow();
            telemetryExecutor = null;
        }
    }

    @FXML
    public void handleBackToDashboard(ActionEvent event) {
        stopTelemetryService();
        if (dashboardRoot != null && event.getSource() instanceof Node sourceNode) {
            sourceNode.getScene().setRoot(dashboardRoot);
        }
    }

    // ========================================
    // Utility Methods
    // ========================================

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
                KeyFrame kf = new KeyFrame(Duration.millis(delayCounter),
                        ignored -> newSeries.getData().add(new XYChart.Data<>(data.getXValue(), data.getYValue()))
                );
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

    private void fetchStaticHardwareInfo() {
        String cpuName = System.getenv("PROCESSOR_IDENTIFIER");
        if (cpuName == null || cpuName.isBlank()) {
            cpuName = System.getProperty("os.arch") + " PROCESSOR";
        }
        if (cpuName.length() > 28) cpuName = cpuName.substring(0, 28) + "...";
        if (cpuLabel != null) cpuLabel.setText(cpuName);

        String gpuText = "JFX DEFAULT";
        try {
            Object prism = System.getProperty("prism.order");
            if (prism != null) gpuText = "PIPELINE: " + prism.toString().toUpperCase();
        } catch (Exception ignored) {}
        if (gpuLabel != null) gpuLabel.setText(gpuText);
    }

    private void updateSystemTelemetry() {
        String ramText = getRealSystemRam();
        String loadText = getRealCpuLoad();
        String timeText = getUptimeText();

        Platform.runLater(() -> {
            if (ramLabel != null) ramLabel.setText(ramText);
            if (serverLoadLabel != null) serverLoadLabel.setText(loadText);
            if (uptimeLabel != null) uptimeLabel.setText(timeText);
        });
    }

    private String getRealSystemRam() {
        java.lang.management.OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOsBean) {
            long total = sunOsBean.getTotalMemorySize();
            long free = sunOsBean.getFreeMemorySize();
            return String.format(Locale.US, "%.1f / %.1f GB (PHYSICAL)", (total - free) / 1073741824.0, total / 1073741824.0);
        }
        return String.format(Locale.US, "JVM HEAP: %.1f MB", ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed() / 1048576.0);
    }

    private String getRealCpuLoad() {
        java.lang.management.OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOsBean) {
            double load = sunOsBean.getCpuLoad();
            return (load < 0) ? "CALCULATING..." : String.format(Locale.US, "LOAD: %.1f%% (CORES: %d)", load * 100, osBean.getAvailableProcessors());
        }
        return "CPU ACTIVE";
    }

    private String getUptimeText() {
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime() / 1000;
        return String.format("SESSION: %02d:%02d:%02d", uptime / 3600, (uptime % 3600) / 60, uptime % 60);
    }

    private void setupDistributionChart(List<Party> parties) {
        double total = parties.stream().mapToDouble(Party::getCurrentSupporterCount).sum();
        for (Party p : parties) {
            PieChart.Data data = new PieChart.Data(p.getAbbreviation(), p.getCurrentSupporterCount());
            distributionChart.getData().add(data);
            runOnNode(data, node -> {
                double pct = (total > 0) ? (data.getPieValue() / total) * 100.0 : 0.0;
                installTooltipOnNode(node, String.format(FORMAT_TOOLTIP_PIE, p.getName(), data.getPieValue(), pct));
                node.setStyle(String.format(STYLE_PIE_COLOR, getPartyColorString(p)));
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
            runOnNode(data, node -> {
                node.setStyle(String.format(STYLE_BAR_FILL, "#" + p.getColorCode()));
                installTooltipOnNode(node, String.format(FORMAT_TOOLTIP_SCANDAL, p.getName(), data.getYValue()));
            });
        }
        scandalChart.getData().add(series);
    }

    private void setupBudgetChart(List<Party> parties) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (Party p : parties) {
            if (p.getName().equals(SimulationConfig.UNDECIDED_NAME)) continue;
            XYChart.Data<String, Number> data = new XYChart.Data<>(p.getAbbreviation(), p.getCampaignBudget() / 1000000.0);
            series.getData().add(data);
            runOnNode(data, node -> {
                node.setStyle(String.format(STYLE_BAR_FILL, "#" + p.getColorCode()));
                installTooltipOnNode(node, String.format(FORMAT_TOOLTIP_BUDGET, p.getName(), data.getYValue().doubleValue()));
            });
        }
        budgetChart.getData().add(series);
    }

    private void fixPieLegendColors(List<Party> parties) {
        Platform.runLater(() -> {
            Node legend = distributionChart.lookup(".chart-legend");
            if (legend instanceof Pane pane) {
                for (Node item : pane.getChildren()) {
                    if (item instanceof Label label) {
                        Party p = findPartyByAbbr(parties, label.getText());
                        if (p != null && label.getGraphic() != null) {
                            label.getGraphic().setStyle(String.format(STYLE_LEGEND_SYMBOL, getPartyColorString(p)));
                        }
                    }
                }
            }
        });
    }

    private void runOnNode(Object chartData, Consumer<Node> action) {
        javafx.beans.property.ReadOnlyObjectProperty<Node> property = null;
        if (chartData instanceof XYChart.Series<?, ?> s) property = s.nodeProperty();
        else if (chartData instanceof XYChart.Data<?, ?> d) property = d.nodeProperty();
        else if (chartData instanceof PieChart.Data p) property = p.nodeProperty();

        if (property != null) {
            if (property.get() != null) action.accept(property.get());
            property.addListener((obs, nd, newVal) -> {
                if (newVal != null) action.accept(newVal);
            });
        }
    }

    private void installTooltipOnNode(Node node, String text) {
        Tooltip t = new Tooltip(text);
        t.setShowDelay(Duration.millis(50));
        Tooltip.install(node, t);
    }

    private Party findPartyByAbbr(List<Party> parties, String abbr) {
        return parties.stream().filter(p -> p.getAbbreviation().equals(abbr)).findFirst().orElse(null);
    }

    private String getPartyColorString(Party p) {
        return p.getName().equals(SimulationConfig.UNDECIDED_NAME) ? "#666666" : "#" + p.getColorCode();
    }
}
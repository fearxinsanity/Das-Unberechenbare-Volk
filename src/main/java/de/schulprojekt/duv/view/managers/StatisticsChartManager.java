package de.schulprojekt.duv.view.managers;

import de.schulprojekt.duv.model.party.Party;
import de.schulprojekt.duv.util.config.SimulationConfig;
import de.schulprojekt.duv.view.Main;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Manager für die Konfiguration und Animation der Statistik-Diagramme.
 * @author Nico Hoffmann
 * @version 1.0
 */
public class StatisticsChartManager {

    // ========================================
    // Static Constants
    // ========================================

    private static final String STYLE_PIE_COLOR = "-fx-pie-color: %s;";
    private static final String STYLE_BAR_FILL = "-fx-bar-fill: %s;";
    private static final String STYLE_LEGEND_SYMBOL = "-fx-background-color: %s;";

    // ========================================
    // Instance Variables
    // ========================================

    private final LineChart<Number, Number> historyChart;
    private final PieChart distributionChart;
    private final BarChart<String, Number> scandalChart;
    private final BarChart<String, Number> budgetChart;

    // ========================================
    // Constructor
    // ========================================

    public StatisticsChartManager(LineChart<Number, Number> history, PieChart distribution, BarChart<String, Number> scandal, BarChart<String, Number> budget) {
        this.historyChart = history;
        this.distributionChart = distribution;
        this.scandalChart = scandal;
        this.budgetChart = budget;
    }

    // ========================================
    // Business Logic Methods
    // ========================================

    /**
     * Initialisiert alle Diagramme mit den Simulationsdaten.
     * @param parties Liste der Parteien
     * @param historyData Historische Datenreihen
     */
    public void setupCharts(List<Party> parties, ObservableList<XYChart.Series<Number, Number>> historyData) {
        setupHistoryChartAnimated(historyData);
        setupDistributionChart(parties);
        setupScandalChart(parties);
        setupBudgetChart(parties);
    }

    // ========================================
    // Utility Methods (Ausgelagert aus Controller)
    // ========================================

    private void setupHistoryChartAnimated(ObservableList<XYChart.Series<Number, Number>> historyData) {
        historyChart.getData().clear();
        ResourceBundle bundle = ResourceBundle.getBundle("de.schulprojekt.duv.messages", Main.getLocale());

        for (XYChart.Series<Number, Number> sourceSeries : historyData) {
            XYChart.Series<Number, Number> newSeries = new XYChart.Series<>();
            newSeries.setName(sourceSeries.getName());
            historyChart.getData().add(newSeries);

            runOnNode(newSeries, node -> installTooltipOnNode(node, bundle.getString("tt.trace").formatted(newSeries.getName())));

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

    private void setupDistributionChart(List<Party> parties) {
        ResourceBundle bundle = ResourceBundle.getBundle("de.schulprojekt.duv.messages", Main.getLocale());
        double total = parties.stream().mapToDouble(Party::getCurrentSupporterCount).sum();
        for (Party p : parties) {
            PieChart.Data data = new PieChart.Data(p.getAbbreviation(), p.getCurrentSupporterCount());
            distributionChart.getData().add(data);
            runOnNode(data, node -> {
                double pct = (total > 0) ? (data.getPieValue() / total) * 100.0 : 0.0;
                installTooltipOnNode(node, String.format(bundle.getString("tt.faction"), p.getName(), data.getPieValue(), pct));
                node.setStyle(String.format(STYLE_PIE_COLOR, getPartyColorString(p)));
            });
        }
        fixPieLegendColors(parties);
    }

    private void setupScandalChart(List<Party> parties) {
        ResourceBundle bundle = ResourceBundle.getBundle("de.schulprojekt.duv.messages", Main.getLocale());
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (Party p : parties) {
            if (p.getName().equals(SimulationConfig.UNDECIDED_NAME)) continue;
            XYChart.Data<String, Number> data = new XYChart.Data<>(p.getAbbreviation(), p.getScandalCount());
            series.getData().add(data);
            runOnNode(data, node -> {
                node.setStyle(String.format(STYLE_BAR_FILL, getPartyColorString(p)));
                installTooltipOnNode(node, bundle.getString("tt.scandals").formatted(data.getYValue()));
            });
        }
        scandalChart.getData().add(series);
    }

    private void setupBudgetChart(List<Party> parties) {
        ResourceBundle bundle = ResourceBundle.getBundle("de.schulprojekt.duv.messages", Main.getLocale());
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (Party p : parties) {
            if (p.getName().equals(SimulationConfig.UNDECIDED_NAME)) continue;
            XYChart.Data<String, Number> data = new XYChart.Data<>(p.getAbbreviation(), p.getCampaignBudget() / 1_000_000.0);
            series.getData().add(data);
            runOnNode(data, node -> {
                node.setStyle(String.format(STYLE_BAR_FILL, getPartyColorString(p)));
                installTooltipOnNode(node, String.format(bundle.getString("tt.budget"), p.getName(), data.getYValue().doubleValue()));
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
                        Party p = parties.stream().filter(x -> x.getAbbreviation().equals(label.getText())).findFirst().orElse(null);
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
            property.addListener((obs, nd, newVal) -> { if (newVal != null) action.accept(newVal); });
        }
    }

    private void installTooltipOnNode(Node node, String text) {
        Tooltip t = new Tooltip(text);
        t.setShowDelay(Duration.millis(50));
        Tooltip.install(node, t);
    }

    private String getPartyColorString(Party p) {
        if (p.getName().equals(SimulationConfig.UNDECIDED_NAME)) return "#666666";
        String code = p.getColorCode();
        return code.startsWith("#") ? code : "#" + code;
    }
}
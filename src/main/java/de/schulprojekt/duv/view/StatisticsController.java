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

import java.util.List;
import java.util.function.Consumer;

/**
 * Controller for the detailed post-simulation statistics view.
 * Displays history, vote distribution, scandals, and budget usage.
 */
public class StatisticsController {

    // --- Constants: Styling & Formatting ---
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

    // --- State ---
    private Parent dashboardRoot;

    // --- Initialization ---

    public void initData(List<Party> parties, ObservableList<XYChart.Series<Number, Number>> historyData, int currentTick, Parent dashboardRoot) {
        this.dashboardRoot = dashboardRoot;
        this.totalTicksLabel.setText("ANALYSIS COMPLETE (TICKS: " + currentTick + ")");

        setupHistoryChart(historyData);
        setupDistributionChart(parties);
        setupScandalChart(parties);
        setupBudgetChart(parties);
    }

    // --- Chart Setup Logic ---

    private void setupHistoryChart(ObservableList<XYChart.Series<Number, Number>> historyData) {
        for (XYChart.Series<Number, Number> sourceSeries : historyData) {
            XYChart.Series<Number, Number> newSeries = new XYChart.Series<>();
            newSeries.setName(sourceSeries.getName());

            for (XYChart.Data<Number, Number> data : sourceSeries.getData()) {
                newSeries.getData().add(new XYChart.Data<>(data.getXValue(), data.getYValue()));
            }
            historyChart.getData().add(newSeries);

            // Copy style from original node if available
            if (sourceSeries.getNode() != null) {
                String style = sourceSeries.getNode().getStyle();
                runOnNode(newSeries, node -> node.setStyle(style));
            }

            // Tooltip for the whole series line
            runOnNode(newSeries, node -> installTooltipOnNode(node, "HISTORY_TRACE: " + newSeries.getName()));
        }
    }

    private void setupDistributionChart(List<Party> parties) {
        double totalVotes = parties.stream().mapToDouble(Party::getCurrentSupporterCount).sum();

        for (Party p : parties) {
            PieChart.Data data = new PieChart.Data(p.getAbbreviation(), p.getCurrentSupporterCount());
            distributionChart.getData().add(data);

            // Calculate percentage
            double percent = (totalVotes > 0) ? (data.getPieValue() / totalVotes) * 100.0 : 0.0;
            String info = String.format(FORMAT_TOOLTIP_PIE, p.getName(), data.getPieValue(), percent);

            // Apply color and tooltip
            runOnNode(data, node -> {
                installTooltipOnNode(node, info);
                String color = getPartyColorString(p);
                node.setStyle(String.format(STYLE_PIE_COLOR, color));
            });
        }

        // Fix legend colors (must happen after layout pass)
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
                // FIX: data.getYValue() returns Number, cast to double for %.2f format
                installTooltipOnNode(node, String.format(FORMAT_TOOLTIP_BUDGET, p.getName(), data.getYValue().doubleValue()));
            });
        }
        budgetChart.getData().add(series);
    }

    // --- Helper Methods ---

    /**
     * Fixes the colors of the PieChart legend items to match the party colors.
     * JavaFX auto-generates legend items, so we need to look them up manually.
     */
    private void fixPieLegendColors(List<Party> parties) {
        Platform.runLater(() -> {
            Node legend = distributionChart.lookup(".chart-legend");

            // FIX: Pattern matching for instanceof (Java 16+)
            if (legend instanceof Pane pane) {
                for (Node item : pane.getChildren()) {
                    // FIX: Pattern matching
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

    /**
     * Executes an action on the visual Node of a chart data item.
     * Handles both immediate execution (if Node exists) and delayed execution (via listener).
     */
    private void runOnNode(Object chartData, Consumer<Node> action) {
        Node node = null;
        javafx.beans.property.ReadOnlyObjectProperty<Node> nodeProperty = null;

        // FIX: Pattern Matching for cleaner type checks
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

        // 1. Execute immediately if available
        if (node != null) {
            action.accept(node);
        }

        // 2. Attach listener for future availability
        if (nodeProperty != null) {
            // FIX: Renamed unused parameters to 'ignored'
            nodeProperty.addListener((ignored, ignoredOld, newNode) -> {
                if (newNode != null) {
                    action.accept(newNode);
                }
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
        return parties.stream()
                .filter(p -> p.getAbbreviation().equals(abbr))
                .findFirst()
                .orElse(null);
    }

    private String getPartyColorString(Party p) {
        return p.getName().equals(SimulationConfig.UNDECIDED_NAME)
                ? "#666666"
                : "#" + p.getColorCode();
    }

    // --- Navigation ---

    @FXML
    public void handleBackToDashboard(ActionEvent event) {
        if (dashboardRoot != null && event.getSource() instanceof Node sourceNode) {
            sourceNode.getScene().setRoot(dashboardRoot);
        }
    }
}
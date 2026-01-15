package de.schulprojekt.duv.view.components;

import de.schulprojekt.duv.model.party.Party;
import de.schulprojekt.duv.util.SimulationConfig;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the history chart visualization for tracking party support over time.
 * Handles chart series creation, updates, and data management.
 *
 * <p>The chart updates periodically based on a configurable update interval
 * and maintains a rolling history of data points.</p>
 *
 * @author Nico Hoffmann
 * @version 1.0
 */
public class ChartManager {

    // --- Constants ---
    private static final int UPDATE_INTERVAL = 5;

    // --- Fields ---
    private final LineChart<Number, Number> historyChart;
    private final Map<String, XYChart.Series<Number, Number>> historySeriesMap = new HashMap<>();

    // --- Constructor ---

    public ChartManager(LineChart<Number, Number> historyChart) {
        this.historyChart = historyChart;

        if (historyChart != null) {
            historyChart.setAnimated(false);
            historyChart.setCreateSymbols(false);
            historyChart.setLegendVisible(false);
        }
    }

    // --- Public API ---

    public void clear() {
        historySeriesMap.clear();
        if (historyChart != null) {
            historyChart.getData().clear();
        }
    }

    public void update(List<Party> parties, int step) {
        if (historyChart == null || step % UPDATE_INTERVAL != 0) return;

        for (Party p : parties) {
            if (p.getName().equals(SimulationConfig.UNDECIDED_NAME)) continue;

            // FIX: Renamed unused parameter 'k' to 'ignored'
            XYChart.Series<Number, Number> series = historySeriesMap.computeIfAbsent(
                    p.getName(),
                    ignored -> createSeries(p)
            );

            series.getData().add(new XYChart.Data<>(step, p.getCurrentSupporterCount()));

            if (series.getData().size() > SimulationConfig.HISTORY_LENGTH) {
                // FIX: Used Java 21 removeFirst() instead of remove(0)
                series.getData().removeFirst();
            }
        }
    }

    // --- Private Helper Methods ---

    private XYChart.Series<Number, Number> createSeries(Party p) {
        XYChart.Series<Number, Number> s = new XYChart.Series<>();
        s.setName(p.getAbbreviation());

        historyChart.getData().add(s);

        // Apply styling if the node is already available (depends on JavaFX layout pass)
        if (s.getNode() != null) {
            s.getNode().setStyle("-fx-stroke: #" + p.getColorCode() + "; -fx-stroke-width: 2px;");
        }

        return s;
    }
}
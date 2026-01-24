package de.schulprojekt.duv.view.components;

import de.schulprojekt.duv.model.party.Party;
import de.schulprojekt.duv.util.config.SimulationConfig;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Verwaltet die Live-Diagramme und die Visualisierung historischer Daten.
 * @author Nico Hoffmann
 * @version 1.1
 */
public class ChartManager {

    // ========================================
    // Static Variables
    // ========================================

    private static final int UPDATE_INTERVAL = 5;

    // ========================================
    // Instance Variables
    // ========================================

    private final LineChart<Number, Number> historyChart;
    private final Map<String, XYChart.Series<Number, Number>> historySeriesMap = new HashMap<>();

    // ========================================
    // Constructors
    // ========================================

    /**
     * Initialisiert den Manager mit einer Referenz auf das UI-Diagramm.
     * @param historyChart Das Liniendiagramm für die Verlaufsvisualisierung.
     */
    public ChartManager(LineChart<Number, Number> historyChart) {
        this.historyChart = historyChart;

        if (historyChart != null) {
            historyChart.setAnimated(false);
            historyChart.setCreateSymbols(false);
            historyChart.setLegendVisible(false);
        }
    }

    // ========================================
    // Business Logic Methods
    // ========================================

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

            XYChart.Series<Number, Number> series = historySeriesMap.computeIfAbsent(
                    p.getName(),
                    ignored -> createSeries(p)
            );

            series.getData().add(new XYChart.Data<>(step, p.getCurrentSupporterCount()));

            if (series.getData().size() > SimulationConfig.HISTORY_LENGTH) {
                series.getData().removeFirst();
            }
        }
    }

    // ========================================
    // Utility Methods
    // ========================================

    private XYChart.Series<Number, Number> createSeries(Party p) {
        XYChart.Series<Number, Number> s = new XYChart.Series<>();
        s.setName(p.getAbbreviation());

        historyChart.getData().add(s);

        if (s.getNode() != null) {
            s.getNode().setStyle("-fx-stroke: " + p.getColorCode() + "; -fx-stroke-width: 2px;");
        }

        return s;
    }
}
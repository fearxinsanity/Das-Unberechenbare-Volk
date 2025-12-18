package de.schulprojekt.duv.view.components;

import de.schulprojekt.duv.model.party.Party;
import de.schulprojekt.duv.util.SimulationConfig;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChartManager {
    private final LineChart<Number, Number> historyChart;
    private final Map<String, XYChart.Series<Number, Number>> seriesMap = new HashMap<>();

    public ChartManager(LineChart<Number, Number> historyChart) {
        this.historyChart = historyChart;
        this.historyChart.setAnimated(false); // Wichtig für Performance bei vielen Updates
    }

    public void update(List<Party> parties, int step) {
        for (Party p : parties) {
            // Nichtwähler ignorieren wir im Chart (optional)
            if (p.getName().equals(SimulationConfig.UNDECIDED_NAME)) continue;

            // Serie holen oder erstellen
            XYChart.Series<Number, Number> series = seriesMap.get(p.getName());
            if (series == null) {
                series = new XYChart.Series<>();
                series.setName(p.getName());
                historyChart.getData().add(series);
                seriesMap.put(p.getName(), series);
            }

            // Datenpunkt hinzufügen
            series.getData().add(new XYChart.Data<>(step, p.getCurrentSupporterCount()));
        }
    }

    public void clear() {
        historyChart.getData().clear();
        seriesMap.clear();
    }
}
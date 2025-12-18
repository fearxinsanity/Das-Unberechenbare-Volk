package de.schulprojekt.duv.view.components;

import de.schulprojekt.duv.util.SimulationConfig;
import de.schulprojekt.duv.model.party.Party;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChartManager {
    private final LineChart<Number, Number> historyChart;
    private final Map<String, XYChart.Series<Number, Number>> historySeriesMap = new HashMap<>();

    public ChartManager(LineChart<Number, Number> historyChart) {
        this.historyChart = historyChart;
        if (historyChart != null) {
            historyChart.setAnimated(false);
            historyChart.setCreateSymbols(false);
            historyChart.setLegendVisible(false);
        }
    }

    public void clear() {
        historySeriesMap.clear();
        if (historyChart != null) historyChart.getData().clear();
    }

    public void update(List<Party> parties, int step) {
        if (historyChart == null || step % 5 != 0) return;
        for (Party p : parties) {
            if (p.getName().equals(SimulationConfig.UNDECIDED_NAME)) continue;
            XYChart.Series<Number, Number> series = historySeriesMap.computeIfAbsent(p.getName(), k -> {
                XYChart.Series<Number, Number> s = new XYChart.Series<>();
                s.setName(p.getAbbreviation());
                historyChart.getData().add(s);
                s.getNode().setStyle("-fx-stroke: #" + p.getColorCode() + "; -fx-stroke-width: 2px;");
                return s;
            });
            series.getData().add(new XYChart.Data<>(step, p.getCurrentSupporterCount()));
            if (series.getData().size() > SimulationConfig.HISTORY_LENGTH) series.getData().remove(0);
        }
    }
}
package de.schulprojekt.duv.view;

import de.schulprojekt.duv.model.party.Party;
import de.schulprojekt.duv.util.SimulationConfig;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;

import java.util.List;
import java.util.function.Consumer;

public class StatisticsController {

    @FXML private LineChart<Number, Number> historyChart;
    @FXML private PieChart distributionChart;
    @FXML private BarChart<String, Number> scandalChart;
    @FXML private BarChart<String, Number> budgetChart;
    @FXML private Label totalTicksLabel;

    private Parent dashboardRoot;

    public void initData(List<Party> parties, ObservableList<XYChart.Series<Number, Number>> historyData, int currentTick, Parent dashboardRoot) {
        this.dashboardRoot = dashboardRoot;
        this.totalTicksLabel.setText("ANALYSIS COMPLETE (TICKS: " + currentTick + ")");

        // ---------------------------------------------------------
        // 1. Verlauf (History)
        // ---------------------------------------------------------
        for (XYChart.Series<Number, Number> sourceSeries : historyData) {
            XYChart.Series<Number, Number> newSeries = new XYChart.Series<>();
            newSeries.setName(sourceSeries.getName());

            for (XYChart.Data<Number, Number> data : sourceSeries.getData()) {
                newSeries.getData().add(new XYChart.Data<>(data.getXValue(), data.getYValue()));
            }
            historyChart.getData().add(newSeries);

            // Style vom Original übernehmen
            if (sourceSeries.getNode() != null) {
                runOnNode(newSeries, node -> node.setStyle(sourceSeries.getNode().getStyle()));
            }
            // Tooltip für die ganze Linie
            runOnNode(newSeries, node -> installTooltipOnNode(node, "HISTORY_TRACE: " + newSeries.getName()));
        }

        // ---------------------------------------------------------
        // 2. Endverteilung (Pie)
        // ---------------------------------------------------------
        double totalVotes = parties.stream().mapToDouble(Party::getCurrentSupporterCount).sum();
        for (Party p : parties) {
            PieChart.Data data = new PieChart.Data(p.getAbbreviation(), p.getCurrentSupporterCount());
            distributionChart.getData().add(data);

            // Tooltip (Prozentberechnung)
            double percent = (data.getPieValue() / totalVotes) * 100.0;
            String info = String.format("FACTION: %s\nSIZE: %.0f\nQUOTA: %.1f%%",
                    p.getName(), data.getPieValue(), percent);

            // WICHTIG: Node-Zugriff sicherstellen für Tooltip & Farbe
            runOnNode(data, node -> {
                installTooltipOnNode(node, info);
                String color = p.getName().equals(SimulationConfig.UNDECIDED_NAME) ? "#666666" : "#" + p.getColorCode();
                node.setStyle("-fx-pie-color: " + color + ";");
            });
        }

        // ---------------------------------------------------------
        // 3. Skandale (Bar - SEC-C3)
        // ---------------------------------------------------------
        XYChart.Series<String, Number> scandalSeries = new XYChart.Series<>();
        for (Party p : parties) {
            if (p.getName().equals(SimulationConfig.UNDECIDED_NAME)) continue;

            XYChart.Data<String, Number> data = new XYChart.Data<>(p.getAbbreviation(), p.getScandalCount());
            scandalSeries.getData().add(data);

            // Farbe & Tooltip
            String color = "#" + p.getColorCode();
            runOnNode(data, node -> {
                node.setStyle("-fx-bar-fill: " + color + ";");
                installTooltipOnNode(node, String.format("TARGET: %s\nINCIDENTS: %s", p.getName(), data.getYValue()));
            });
        }
        scandalChart.getData().add(scandalSeries);

        // ---------------------------------------------------------
        // 4. Budget (Bar - SEC-D4)
        // ---------------------------------------------------------
        XYChart.Series<String, Number> budgetSeries = new XYChart.Series<>();
        for (Party p : parties) {
            if (p.getName().equals(SimulationConfig.UNDECIDED_NAME)) continue;

            double budgetM = p.getCampaignBudget() / 1_000_000.0;
            XYChart.Data<String, Number> data = new XYChart.Data<>(p.getAbbreviation(), budgetM);
            budgetSeries.getData().add(data);

            // Farbe & Tooltip
            String color = "#" + p.getColorCode();
            runOnNode(data, node -> {
                node.setStyle("-fx-bar-fill: " + color + ";");
                installTooltipOnNode(node, String.format("TARGET: %s\nBUDGET: %.2f M€", p.getName(), data.getYValue()));
            });
        }
        budgetChart.getData().add(budgetSeries);
    }

    /**
     * Führt eine Aktion auf dem Node eines Chart-Datenpunkts aus.
     * Prüft SOFORT, ob der Node da ist, UND setzt einen Listener für später.
     * Löst das Problem, dass Tooltips/Farben manchmal fehlen.
     */
    private void runOnNode(Object chartData, Consumer<Node> action) {
        Node node = null;
        // KORREKTUR: Typ auf ReadOnlyObjectProperty geändert, damit er für alle Charts passt
        javafx.beans.property.ReadOnlyObjectProperty<Node> nodeProperty = null;

        if (chartData instanceof XYChart.Series) {
            XYChart.Series<?,?> series = (XYChart.Series<?,?>) chartData;
            node = series.getNode();
            nodeProperty = series.nodeProperty();
        } else if (chartData instanceof XYChart.Data) {
            XYChart.Data<?,?> data = (XYChart.Data<?,?>) chartData;
            node = data.getNode();
            nodeProperty = data.nodeProperty();
        } else if (chartData instanceof PieChart.Data) {
            PieChart.Data data = (PieChart.Data) chartData;
            node = data.getNode();
            nodeProperty = data.nodeProperty(); // Das hier verursachte den Fehler
        }

        // 1. Wenn Node schon da ist -> Sofort ausführen
        if (node != null) {
            action.accept(node);
        }

        // 2. Listener anhängen (falls Node später neu erzeugt wird, z.B. bei Animation)
        if (nodeProperty != null) {
            nodeProperty.addListener((obs, oldNode, newNode) -> {
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
        return parties.stream().filter(p -> p.getAbbreviation().equals(abbr)).findFirst().orElse(null);
    }

    @FXML
    public void handleBackToDashboard(ActionEvent event) {
        if (dashboardRoot != null) {
            ((Node) event.getSource()).getScene().setRoot(dashboardRoot);
        }
    }
}
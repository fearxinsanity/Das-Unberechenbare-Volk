package de.schulprojekt.duv.view.controllers;

import de.schulprojekt.duv.model.party.Party;
import de.schulprojekt.duv.view.Main;
import de.schulprojekt.duv.view.managers.StatisticsChartManager;
import de.schulprojekt.duv.view.managers.TelemetryManager;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.util.Duration;

import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller für die Statistik-Ansicht nach der Simulation.
 * Steuert die Interaktion und koordiniert Chart- sowie Telemetrie-Manager.
 * @author Nico Hoffmann
 * @version 1.0
 */
public class StatisticsController {

    // ========================================
    // Instance Variables (UI Elements)
    // ========================================

    @FXML private LineChart<Number, Number> historyChart;
    @FXML private PieChart distributionChart;
    @FXML private BarChart<String, Number> scandalChart;
    @FXML private BarChart<String, Number> budgetChart;
    @FXML private Label totalTicksLabel;

    @FXML private Label statusLabelArchived, statusLabelCalculated, statusLabelReview, statusLabelVerified;
    @FXML private Label cpuLabel, gpuLabel, serverLoadLabel, ramLabel, uptimeLabel;

    private Parent dashboardRoot;
    private TelemetryManager telemetryManager;

    // ========================================
    // Business Logic Methods
    // ========================================

    /**
     * Initialisiert den Controller und startet die Manager-Dienste.
     * @param parties Liste der Parteien
     * @param historyData Historische Daten
     * @param currentTick Letzter Tick der Simulation
     * @param dashboardRoot Rücksprungziel
     */
    public void initData(List<Party> parties, ObservableList<XYChart.Series<Number, Number>> historyData, int currentTick, Parent dashboardRoot) {
        this.dashboardRoot = dashboardRoot;
        ResourceBundle bundle = ResourceBundle.getBundle("de.schulprojekt.duv.messages", Main.getLocale());

        this.totalTicksLabel.setText(String.format(bundle.getString("stats.analysis_complete"), currentTick));

        // Initialize Managers
        StatisticsChartManager chartManager = new StatisticsChartManager(historyChart, distributionChart, scandalChart, budgetChart);
        this.telemetryManager = new TelemetryManager(cpuLabel, gpuLabel, serverLoadLabel, ramLabel, uptimeLabel);

        // Execute Logic
        chartManager.setupCharts(parties, historyData);
        this.telemetryManager.start();

        startStatusAnimations();
    }

    @FXML
    public void handleBackToDashboard(ActionEvent event) {
        if (telemetryManager != null) telemetryManager.stop();
        if (dashboardRoot != null && event.getSource() instanceof Node sourceNode) {
            sourceNode.getScene().setRoot(dashboardRoot);
        }
    }

    // ========================================
    // Utility Methods
    // ========================================

    private void startStatusAnimations() {
        startStatusPulse(statusLabelArchived);
        startStatusPulse(statusLabelCalculated);
        startStatusPulse(statusLabelReview);
        startStatusPulse(statusLabelVerified);
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
}
package de.schulprojekt.duv.view;

import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Pane;

/**
 * FXML Controller for the DashboardUI.
 * This class adheres to the MVC pattern by acting as a "dumb" View.
 * It holds references to all FXML UI components and provides public getters
 * for the (Phase 3) SimulationController to access them.
 * It contains NO simulation logic.
 */
public class DashboardController {

    // === Header Controls (from mockup 'header') ===
    @FXML
    private Button startButton;
    @FXML
    private Button pauseButton;
    @FXML
    private Button resetButton;

    // === Parameters Panel (from 'parameters-panel') ===
    @FXML
    private Slider voterCountSlider;
    @FXML
    private Label voterCountDisplay;

    @FXML
    private Slider mediaInfluenceSlider;
    @FXML
    private Label mediaInfluenceDisplay;

    @FXML
    private Slider voterMobilitySlider;
    @FXML
    private Label voterMobilityDisplay;

    @FXML
    private ToggleGroup speedToggleGroup;
    @FXML
    private ToggleButton speedSlowButton;
    @FXML
    private ToggleButton speedNormalButton;
    @FXML
    private ToggleButton speedFastButton;

    @FXML
    private Slider partyCountSlider;
    @FXML
    private Label partyCountDisplay;

    @FXML
    private TextField budgetPartyAField;
    @FXML
    private TextField budgetPartyBField;
    @FXML
    private TextField budgetPartyCField;
    @FXML
    private TextField budgetPartyDField;

    @FXML
    private Slider scandalProbabilitySlider;
    @FXML
    private Label scandalProbabilityDisplay;

    // === Visualization Panel (from 'visualization-panel') ===
    @FXML
    private Label timestepLabel;
    @FXML
    private Label totalVotersLabel;
    @FXML
    private Label changesPerSecLabel;
    @FXML
    private Pane animationPane;
    @FXML
    private BarChart<String, Number> distributionChart;

    // === Events Panel (from 'events-panel') ===
    @FXML
    private ListView<String> eventFeedList;

    /**
     * Initializes the controller class.
     * This method is automatically called after the FXML file has been loaded.
     * Why: We bind the labels directly to the slider properties for automatic UI updates,
     * adhering to the Usability (ISO 9241-110) requirement of clear feedback.
     */
    @FXML
    private void initialize() {
        // Bind sliders to their respective display labels

        // Format for whole numbers (e.g., "2500")
        voterCountDisplay.textProperty().bind(
                voterCountSlider.valueProperty().asString("%.0f")
        );

        partyCountDisplay.textProperty().bind(
                partyCountSlider.valueProperty().asString("%.0f")
        );

        // Format for percentages (e.g., "65%")
        mediaInfluenceDisplay.textProperty().bind(
                mediaInfluenceSlider.valueProperty().asString("%.0f%%")
        );

        voterMobilityDisplay.textProperty().bind(
                voterMobilitySlider.valueProperty().asString("%.0f%%")
        );

        scandalProbabilityDisplay.textProperty().bind(
                scandalProbabilitySlider.valueProperty().asString("%.0f%%")
        );

        // Set placeholder values for budgets (from mockup)
        budgetPartyAField.setText("500000");
        budgetPartyBField.setText("350000");
        budgetPartyCField.setText("150000"); // Placeholder
        budgetPartyDField.setText("100000"); // Placeholder

        // Set placeholder data for the chart (based on mockup)
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.getData().add(new XYChart.Data<>("Partei A", 30));
        series.getData().add(new XYChart.Data<>("Partei B", 36));
        series.getData().add(new XYChart.Data<>("Partei C", 22));
        series.getData().add(new XYChart.Data<>("Partei D", 12));
        distributionChart.getData().add(series);

        // Set placeholder data for the event feed
        eventFeedList.getItems().add("Zeitschritt 127: ⚠️ Skandal bei Partei A!");
        eventFeedList.getItems().add("Zeitschritt 115: 📺 TV-Debatte erfolgreich für Partei B!");
        eventFeedList.getItems().add("Zeitschritt 103: 📊 Meinungsumfrage: Medieneinfluss erhöht.");
    }

    // --- Public Getters for the SimulationController (Phase 3) ---

    public Button getStartButton() {
        return startButton;
    }

    public Button getPauseButton() {
        return pauseButton;
    }

    public Button getResetButton() {
        return resetButton;
    }

    public Slider getVoterCountSlider() {
        return voterCountSlider;
    }

    public Slider getMediaInfluenceSlider() {
        return mediaInfluenceSlider;
    }

    public Slider getVoterMobilitySlider() {
        return voterMobilitySlider;
    }

    public ToggleGroup getSpeedToggleGroup() {
        return speedToggleGroup;
    }

    public Label getTimestepLabel() {
        return timestepLabel;
    }

    public Label getTotalVotersLabel() {
        return totalVotersLabel;
    }

    public Label getChangesPerSecLabel() {
        return changesPerSecLabel;
    }

    public Pane getAnimationPane() {
        return animationPane;
    }

    public BarChart<String, Number> getDistributionChart() {
        return distributionChart;
    }

    public ListView<String> getEventFeedList() {
        return eventFeedList;
    }

    // --- NEU HINZUGEFÜGTE GETTER (BEHEBEN DEN FEHLER) ---

    public Slider getPartyCountSlider() {
        return partyCountSlider;
    }

    public Slider getScandalProbabilitySlider() {
        return scandalProbabilitySlider;
    }

    public TextField getBudgetPartyAField() {
        return budgetPartyAField;
    }

    public TextField getBudgetPartyBField() {
        return budgetPartyBField;
    }

    public TextField getBudgetPartyCField() {
        return budgetPartyCField;
    }

    public TextField getBudgetPartyDField() {
        return budgetPartyDField;
    }
    // --- ENDE NEU ---
}
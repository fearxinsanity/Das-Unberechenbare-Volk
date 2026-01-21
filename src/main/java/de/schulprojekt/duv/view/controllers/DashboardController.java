package de.schulprojekt.duv.view.controllers;

import de.schulprojekt.duv.controller.SimulationController;
import de.schulprojekt.duv.model.core.SimulationParameters;
import de.schulprojekt.duv.model.party.Party;
import de.schulprojekt.duv.model.scandal.ScandalEvent;
import de.schulprojekt.duv.model.dto.VoterTransition;
import de.schulprojekt.duv.util.validation.ParameterValidator;
import de.schulprojekt.duv.view.components.CanvasRenderer;
import de.schulprojekt.duv.view.components.ChartManager;
import de.schulprojekt.duv.view.components.FeedManager;
import de.schulprojekt.duv.view.components.TooltipManager;
import de.schulprojekt.duv.view.managers.ParameterManager;
import de.schulprojekt.duv.view.managers.SimulationStateManager;
import de.schulprojekt.duv.view.managers.UIControlManager;
import de.schulprojekt.duv.view.util.VisualFX;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.chart.LineChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main controller for the simulation dashboard.
 * Coordinates between managers and handles user interactions.
 *
 * @author Nico Hoffmann
 * @version 1.0
 */
public class DashboardController {

    // ========================================
    // Static Variables
    // ========================================

    private static final Logger LOGGER = Logger.getLogger(DashboardController.class.getName());

    // ========================================
    // FXML: Layout & Containers
    // ========================================

    @FXML private ScrollPane scandalTickerScroll;
    @FXML private HBox scandalTickerBox;
    @FXML private Pane animationPane;
    @FXML private Pane eventFeedPane;
    @FXML private VBox leftSidebar;
    @FXML private VBox rightSidebar;

    @FXML private VBox populationBox;
    @FXML private VBox partyBox;
    @FXML private VBox budgetBox;
    @FXML private VBox durationBox;
    @FXML private VBox randomBox;

    @FXML private Label populationOverlay;
    @FXML private Label partyOverlay;
    @FXML private Label budgetOverlay;
    @FXML private Label durationOverlay;
    @FXML private Label randomOverlay;

    // ========================================
    // FXML: Visualization
    // ========================================

    @FXML private LineChart<Number, Number> historyChart;
    @FXML private Label timeStepLabel;

    // ========================================
    // FXML: Controls & Inputs
    // ========================================

    @FXML private TextField voterCountField;
    @FXML private TextField partyCountField;
    @FXML private TextField budgetField;
    @FXML private TextField scandalChanceField;
    @FXML private TextField durationField;

    @FXML private Slider mediaInfluenceSlider;
    @FXML private Slider mobilityRateSlider;
    @FXML private Slider loyaltyMeanSlider;
    @FXML private Slider randomRangeSlider;

    // ========================================
    // FXML: Buttons
    // ========================================

    @FXML private Button executeToggleButton;
    @FXML private Button resetButton;
    @FXML private Button intelButton;
    @FXML private Button parliamentButton;

    @FXML private Button speed1xBtn;
    @FXML private Button speed2xBtn;
    @FXML private Button speed4xBtn;

    // ========================================
    // Instance Variables - Managers
    // ========================================

    private SimulationController controller;
    private ParameterManager parameterManager;
    private SimulationStateManager stateManager;
    private UIControlManager uiManager;

    private CanvasRenderer canvasRenderer;
    private ChartManager chartManager;
    private FeedManager feedManager;
    private TooltipManager tooltipManager;

    // ========================================
    // Constructors
    // ========================================

    /**
     * Default constructor called by JavaFX.
     */
    public DashboardController() {
    }

    // ========================================
    // Initialization
    // ========================================

    @FXML
    public void initialize() {
        initializeManagers();
        initializeComponents();
        setupEventHandlers();
        initializeController();

        Platform.runLater(() -> {
            if (controller != null) {
                updateDashboard(controller.getParties(), List.of(), null, 0);
            }
        });
    }

    // ========================================
    // Business Logic Methods
    // ========================================

    /**
     * Applies initial settings from StartController.
     *
     * @param population initial population size
     * @param budget initial budget value
     */
    public void applyInitialSettings(long population, long budget) {
        if (parameterManager != null) {
            parameterManager.applyInitialSettings(population, budget);
        }
    }

    /**
     * Updates the dashboard with new simulation data.
     *
     * @param parties list of all parties
     * @param transitions voter transitions this step
     * @param scandal scandal event (if any)
     * @param step current simulation step
     */
    public void updateDashboard(
            List<Party> parties,
            List<VoterTransition> transitions,
            ScandalEvent scandal,
            int step
    ) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> updateDashboard(parties, transitions, scandal, step));
            return;
        }

        stateManager.setCurrentTick(step);

        if (step == 0) {
            chartManager.clear();
            canvasRenderer.clear(parties);
            feedManager.clear();
        }

        stateManager.updateStatusDisplay(controller.isRunning());
        feedManager.processScandal(scandal, step);
        chartManager.update(parties, step);
        canvasRenderer.update(parties, transitions, controller.getCurrentParameters().populationSize());

        if (scandal != null) {
            uiManager.triggerSidebarGlitch();
        }
    }

    /**
     * Shuts down all components and timers.
     */
    public void shutdown() {
        if (controller != null) {
            controller.shutdown();
        }
        if (canvasRenderer != null) {
            canvasRenderer.stop();
        }
        if (stateManager != null) {
            stateManager.stopTimer();
        }
    }

    // ========================================
    // FXML Event Handlers - Simulation Control
    // ========================================

    @FXML
    public void handleToggleSimulation() {
        if (controller == null) return;

        if (controller.isRunning()) {
            controller.pauseSimulation();
            stateManager.pauseTimer();
        } else {
            if (stateManager.getRemainingSeconds() > 0) {
                handleParameterChange();
                controller.startSimulation();
                stateManager.startTimer();
            }
        }
    }

    @FXML
    public void handleResetSimulation() {
        if (controller != null) {
            controller.resetSimulation();
            stateManager.resetSimulation();
        }
    }

    @FXML
    public void handleLogout() {
        if (controller != null && controller.isRunning()) {
            controller.pauseSimulation();
            stateManager.pauseTimer();
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("SYSTEM ABORT");
        alert.setHeaderText("TERMINATE SESSION");
        alert.setContentText("Are you sure you want to terminate the secure connection?");

        applyAlertStyling(alert);

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            shutdown();
            Platform.exit();
            System.exit(0);
        }
    }

    // ========================================
    // FXML Event Handlers - Parameters
    // ========================================

    @FXML
    public void handleParameterChange() {
        if (controller == null || parameterManager == null) return;

        Platform.runLater(() -> {
            SimulationParameters params = parameterManager.buildParametersFromUI(
                    controller.getCurrentParameters().tickRate()
            );

            if (params != null) {
                controller.updateAllParameters(params);
            }
        });
    }

    @FXML
    public void handleRandomize() {
        if (parameterManager == null) return;

        parameterManager.randomizeParameters(this::handleParameterChange);
    }

    // ========================================
    // FXML Event Handlers - Duration
    // ========================================

    @FXML
    public void handleDurationIncrement() {
        if (stateManager != null) {
            stateManager.incrementDuration();
        }
    }

    @FXML
    public void handleDurationDecrement() {
        if (stateManager != null) {
            stateManager.decrementDuration();
        }
    }

    // ========================================
    // FXML Event Handlers - Field Adjustments
    // ========================================

    @FXML
    public void handleVoterCountIncrement() {
        parameterManager.adjustIntField(
                voterCountField,
                10000,
                ParameterValidator.getMinPopulation(),
                ParameterValidator.getMaxPopulation()
        );
    }

    @FXML
    public void handleVoterCountDecrement() {
        parameterManager.adjustIntField(
                voterCountField,
                -10000,
                ParameterValidator.getMinPopulation(),
                ParameterValidator.getMaxPopulation()
        );
    }

    @FXML
    public void handlePartyCountIncrement() {
        parameterManager.adjustIntField(
                partyCountField,
                1,
                ParameterValidator.getMinParties(),
                ParameterValidator.getMaxParties()
        );
    }

    @FXML
    public void handlePartyCountDecrement() {
        parameterManager.adjustIntField(
                partyCountField,
                -1,
                ParameterValidator.getMinParties(),
                ParameterValidator.getMaxParties()
        );
    }

    @FXML
    public void handleScandalChanceIncrement() {
        parameterManager.adjustDoubleField(scandalChanceField, 0.5);
    }

    @FXML
    public void handleScandalChanceDecrement() {
        parameterManager.adjustDoubleField(scandalChanceField, -0.5);
    }

    // ========================================
    // FXML Event Handlers - Speed Control
    // ========================================

    @FXML
    public void handleSpeed1x() {
        if (controller != null) {
            controller.updateSimulationSpeed(1);
            updateSpeedSelectionUI(1);
        }
    }

    @FXML
    public void handleSpeed2x() {
        if (controller != null) {
            controller.updateSimulationSpeed(2);
            updateSpeedSelectionUI(2);
        }
    }

    @FXML
    public void handleSpeed4x() {
        if (controller != null) {
            controller.updateSimulationSpeed(4);
            updateSpeedSelectionUI(4);
        }
    }

    // ========================================
    // FXML Event Handlers - Navigation
    // ========================================

    @FXML
    public void handleShowStatistics() {
        navigate("/de/schulprojekt/duv/view/StatisticsView.fxml", (loader, ignoredRoot) -> {
            StatisticsController statsCtrl = loader.getController();
            Parent dashboardRoot = executeToggleButton.getScene().getRoot();
            statsCtrl.initData(
                    controller.getParties(),
                    historyChart.getData(),
                    stateManager.getCurrentTick(),
                    dashboardRoot
            );
        });
    }

    @FXML
    public void handleShowParliament() {
        navigate("/de/schulprojekt/duv/view/ParliamentView.fxml", (loader, ignoredRoot) -> {
            ParliamentController parliamentController = loader.getController();
            Parent dashboardView = executeToggleButton.getScene().getRoot();
            parliamentController.initData(controller.getParties(), dashboardView);
        });
    }

    // ========================================
    // Utility Methods
    // ========================================

    private void initializeManagers() {
        // Parameter Manager
        parameterManager = new ParameterManager(
                voterCountField, partyCountField, budgetField, scandalChanceField,
                mediaInfluenceSlider, mobilityRateSlider, loyaltyMeanSlider, randomRangeSlider
        );
        parameterManager.setOnParameterChangeCallback(this::handleParameterChange);
        parameterManager.initializeFields();

        // State Manager
        stateManager = new SimulationStateManager();
        stateManager.setTimeStepLabel(timeStepLabel);
        stateManager.setDurationField(durationField);
        stateManager.setButtons(executeToggleButton, resetButton, intelButton, parliamentButton);
        stateManager.setLockingContainers(
                populationBox, partyBox, budgetBox, durationBox, randomBox,
                populationOverlay, partyOverlay, budgetOverlay, durationOverlay, randomOverlay
        );
        stateManager.setSidebars(leftSidebar, rightSidebar);
        stateManager.setOnPauseCallback(() -> {
            if (controller != null && controller.isRunning()) {
                controller.pauseSimulation();
            }
        });
        stateManager.setupTimer();

        // UI Manager
        uiManager = new UIControlManager(animationPane, leftSidebar, rightSidebar);
        uiManager.setupResponsiveLayout();
    }

    private void initializeComponents() {
        canvasRenderer = new CanvasRenderer(animationPane);
        chartManager = new ChartManager(historyChart);
        feedManager = new FeedManager(scandalTickerBox, scandalTickerScroll, eventFeedPane);
        tooltipManager = new TooltipManager(animationPane);

        canvasRenderer.startVisualTimer();
        stateManager.lockResultButtons(true);
    }

    private void setupEventHandlers() {
        if (canvasRenderer != null && tooltipManager != null) {
            canvasRenderer.getCanvas().setOnMouseMoved(e -> {
                if (controller != null) {
                    tooltipManager.handleMouseMove(
                            e.getX(), e.getY(),
                            controller.getParties(),
                            canvasRenderer.getPartyPositions(),
                            controller.getCurrentParameters().populationSize()
                    );
                }
            });
            canvasRenderer.getCanvas().setOnMouseExited(ignored -> tooltipManager.hideTooltip());
        }

        if (intelButton != null) {
            intelButton.setOnMouseEntered(e -> VisualFX.stopPulse(intelButton));
        }
        if (parliamentButton != null) {
            parliamentButton.setOnMouseEntered(e -> VisualFX.stopPulse(parliamentButton));
        }
    }

    private void initializeController() {
        controller = new SimulationController(this);
        parameterManager.synchronizeWithParameters(controller.getCurrentParameters());
        stateManager.updateStatusDisplay(false);
        handleSpeed1x();
    }

    private void updateSpeedSelectionUI(int selectedSpeed) {
        List<Button> speedButtons = List.of(speed1xBtn, speed2xBtn, speed4xBtn);
        speedButtons.forEach(btn -> {
            if (btn != null) {
                btn.getStyleClass().remove("speed-button-active");
            }
        });

        if (selectedSpeed == 1 && speed1xBtn != null) speed1xBtn.getStyleClass().add("speed-button-active");
        else if (selectedSpeed == 2 && speed2xBtn != null) speed2xBtn.getStyleClass().add("speed-button-active");
        else if (selectedSpeed == 4 && speed4xBtn != null) speed4xBtn.getStyleClass().add("speed-button-active");
    }

    private void navigate(
            String fxmlPath,
            java.util.function.BiConsumer<FXMLLoader, Parent> initAction
    ) {
        if (controller == null) return;
        if (controller.isRunning()) {
            controller.pauseSimulation();
            stateManager.pauseTimer();
        }

        try {
            var resource = getClass().getResource(fxmlPath);
            if (resource == null) {
                new Alert(Alert.AlertType.ERROR, "View not found: " + fxmlPath).show();
                return;
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();
            initAction.accept(loader, root);
            executeToggleButton.getScene().setRoot(root);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Navigation failed", e);
        }
    }

    private void applyAlertStyling(Alert alert) {
        try {
            if (executeToggleButton != null && executeToggleButton.getScene() != null) {
                alert.getDialogPane().getStylesheets().addAll(
                        executeToggleButton.getScene().getStylesheets()
                );
                alert.getDialogPane().getStyleClass().add("alert-dialog");
            }
        } catch (Exception e) {
            // Ignore CSS errors
        }
    }
}
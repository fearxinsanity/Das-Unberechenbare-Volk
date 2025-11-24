package de.schulprojekt.duv.controller;

import de.schulprojekt.duv.model.engine.SimulationEngine;
import de.schulprojekt.duv.model.engine.SimulationParameters;
import de.schulprojekt.duv.model.engine.VoterTransition;
import de.schulprojekt.duv.model.entities.Party;
import de.schulprojekt.duv.view.DashboardController;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.chart.XYChart;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * The main application controller (in the MVC pattern).
 * It connects the (dumb) View {@link DashboardController} with the (dumb) Model {@link SimulationEngine}.
 * It is responsible for handling all user input (Event Handlers) and running
 * the simulation loop (Timer).
 *
 */
public class SimulationController {

    private final SimulationEngine model;
    private final DashboardController view;
    private final Timeline simulationTimer;

    // Base speed: 1000ms = 1 tick per second ("Normal" speed)
    private static final double BASE_TICK_DURATION_MS = 1000.0;
    private static final double DEFAULT_BUDGET = 50000.0; // Fallback-Budget

    /**
     * Constructs the controller and links Model and View.
     * @param model The simulation logic (Phase 1).
     * @param view The FXML-linked view controller (Phase 2).
     */
    public SimulationController(SimulationEngine model, DashboardController view) {
        this.model = model;
        this.view = view;
        this.simulationTimer = createSimulationTimer();
    }

    /**
     * Called by Main.java to activate the controller.
     * Binds all UI controls to their respective functions.
     */
    public void initializeSimulation() {
        setupEventHandlers();
        // Setup simulation based on default GUI values
        handleResetSimulation();
    }

    /**
     * Creates the main simulation loop (the "heartbeat").
     * Uses a JavaFX Timeline to repeatedly call runSimulationTick().
     */
    private Timeline createSimulationTimer() {
        // The Timeline will run at "Normal" speed (1x) by default.
        Timeline timer = new Timeline(
                new KeyFrame(Duration.millis(BASE_TICK_DURATION_MS),
                        event -> runSimulationTick())
        );
        timer.setCycleCount(Animation.INDEFINITE);
        return timer;
    }

    /**
     * Binds all buttons from the View to methods in this controller.
     */
    private void setupEventHandlers() {
        // 1. Bind Start/Pause/Reset buttons
        view.getStartButton().setOnAction(event -> handleStartSimulation());
        view.getPauseButton().setOnAction(event -> handlePauseSimulation());
        view.getResetButton().setOnAction(event -> handleResetSimulation());

        // 2. Bind Speed Toggle Buttons
        view.getSpeedToggleGroup().selectedToggleProperty().addListener(
                (obs, oldToggle, newToggle) -> handleSpeedChange((ToggleButton) newToggle)
        );
    }

    // --- Event Handler Methods (View -> Model) ---

    private void handleStartSimulation() {
        // If simulation is new, set it up first.
        if (model.getCurrentTick() == 0) {
            handleResetSimulation();
        }
        model.startSimulation();
        simulationTimer.play();
    }

    private void handlePauseSimulation() {
        model.stopSimulation();
        simulationTimer.pause();
    }

    private void handleResetSimulation() {
        handlePauseSimulation(); // Stop the timer

        // 1. Get all parameters from the View
        SimulationParameters params = gatherParametersFromView();

        // 2. Pass them to the Model to set up the simulation
        model.setupSimulation(params);

        // 3. Update the View immediately with the setup data
        updateView();
    }

    private void handleSpeedChange(ToggleButton selectedSpeedButton) {
        if (selectedSpeedButton == null) return;

        // Fulfills the 3-speed requirement
        double rate;
        String speed = selectedSpeedButton.getText();

        if ("LANGSAM".equals(speed)) {
            rate = 0.5; // 0.5x speed
        } else if ("SCHNELL".equals(speed)) {
            rate = 2.0; // 2.0x speed
        } else {
            rate = 1.0; // "NORMAL" = 1.0x speed
        }

        // Change the speed of the running timer
        simulationTimer.setRate(rate);
    }

    /**
     * This is the core simulation loop method.
     * It is called by the Timeline timer (e.g., every 1 second).
     */
    private void runSimulationTick() {
        // 1. Tell the Model to advance one step
        model.performTick();

        // 2. Get the results from the Model and update the View
        updateView();
    }

    /**
     * Gathers all data from the Model and updates the View components.
     * (Model -> View)
     */
    private void updateView() {
        // Use Platform.runLater to ensure UI updates happen on the JavaFX thread
        Platform.runLater(() -> {
            // --- Update Stats ---
            view.getTimestepLabel().setText(String.valueOf(model.getCurrentTick()));
            // Read total voter count directly from the slider (as it's the source of truth)
            view.getTotalVotersLabel().setText(
                    String.format("%,.0f", view.getVoterCountSlider().getValue())
            );

            // --- Update Event Feed ---
            String latestEvent = model.getLatestEvent();
            // Add new event only if it's not the one we just added
            if (!view.getEventFeedList().getItems().isEmpty() &&
                    !view.getEventFeedList().getItems().get(0).equals(latestEvent))
            {
                view.getEventFeedList().getItems().add(0, latestEvent);
            }

            // --- Update Bar Chart (with real data) ---
            view.getDistributionChart().getData().clear(); // Clear old data
            XYChart.Series<String, Number> series = new XYChart.Series<>();

            List<Party> parties = model.getPartyResults(); // Gets shallow copy

            // 1. Calculate total supporters to get percentages
            long totalSupporters = 0;
            for (Party party : parties) {
                totalSupporters += party.getSupporterCount();
            }

            // 2. Add data for each party to the series
            for (Party party : parties) {
                double percentage = 0;
                if (totalSupporters > 0) {
                    percentage = ((double) party.getSupporterCount() / totalSupporters) * 100.0;
                }
                series.getData().add(new XYChart.Data<>(party.getName(), percentage));
            }
            view.getDistributionChart().getData().add(series);


            // --- Update Animation (Data-Stub) ---
            // TODO: Implement the *drawing* logic for the animation
            List<VoterTransition> transitions = model.getRecentTransitions();
            if (!transitions.isEmpty()) {
                // As a stub, print the animation data to the console
                // to prove the data flow is working.
                System.out.println("--- Tick " + model.getCurrentTick() + " Transitions ---");
                for (VoterTransition trans : transitions) {
                    System.out.printf(
                            "   %d voters moved from %s to %s%n",
                            trans.getVoterCount(),
                            trans.getFromPartyName(),
                            trans.getToPartyName()
                    );
                }
            }
            // (Hier würde der Code stehen, der view.getAnimationPane() bemalt)
        });
    }

    /**
     * Helper method to read all 7+ parameters from the View controls.
     *
     * @return A new SimulationParameters DTO filled with GUI data.
     */
    private SimulationParameters gatherParametersFromView() {
        SimulationParameters params = new SimulationParameters();

        // Get values from Sliders
        params.setVoterCount((int) view.getVoterCountSlider().getValue());
        params.setMediaInfluence(view.getMediaInfluenceSlider().getValue() / 100.0);
        params.setVoterMobility(view.getVoterMobilitySlider().getValue() / 100.0);
        params.setPartyCount((int) view.getPartyCountSlider().getValue());
        params.setScandalLambda(view.getScandalProbabilitySlider().getValue() / 100.0);

        // Get values from TextFields (Budgets) with error handling
        List<Double> budgets = new ArrayList<>();
        budgets.add(parseBudget(view.getBudgetPartyAField(), DEFAULT_BUDGET));
        budgets.add(parseBudget(view.getBudgetPartyBField(), DEFAULT_BUDGET));
        budgets.add(parseBudget(view.getBudgetPartyCField(), DEFAULT_BUDGET));
        budgets.add(parseBudget(view.getBudgetPartyDField(), DEFAULT_BUDGET));
        // TODO: Dynamisch an partyCount anpassen

        params.setPartyBudgets(budgets);

        return params;
    }

    /**
     * Safely parses a budget value from a TextField.
     * @param field The TextField to read from.
     * @param defaultValue A fallback value if parsing fails.
     * @return The parsed budget as a double.
     */
    private double parseBudget(TextField field, double defaultValue) {
        try {
            // Read text, remove '€' or '.' if user uses them for formatting
            String text = field.getText().replaceAll("[€.,]", "");
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            // If text is not a valid number (e.g., "abc"), return default
            return defaultValue;
        }
    }
}
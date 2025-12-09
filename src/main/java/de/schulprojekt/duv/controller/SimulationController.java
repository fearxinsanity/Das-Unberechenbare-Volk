package de.schulprojekt.duv.controller;

import de.schulprojekt.duv.model.engine.SimulationEngine;
import de.schulprojekt.duv.model.engine.SimulationParameters;
import de.schulprojekt.duv.view.DashboardController;
import javafx.animation.AnimationTimer;

/**
 * The main application controller (in the MVC pattern).
 * It connects the (dumb) View (DashboardController) with the (dumb) Model (SimulationEngine).
 * It is responsible for handling all user input and running the simulation loop.
 */
public class SimulationController {

    private final SimulationEngine engine;
    private final AnimationTimer simulationTimer;
    private final DashboardController view;

    // NOTE: The reference to the View (DashboardController) will be added here soon!

    /**
     * Initializes the SimulationController and sets up the Model with default parameters.
     */
    public SimulationController(DashboardController view) {

        SimulationParameters defaultParams = createDefaultParameters();

        this.engine = new SimulationEngine(defaultParams);
        this.engine.initializeSimulation();
        this.view = view;
        this.simulationTimer = new AnimationTimer() {
            private long lastUpdate = 0;
            private final long updateIntervalNano = 1_000_000_000L / defaultParams.getSimulationTicksPerSecond();

            @Override
            public void handle(long now) {
                if (now - lastUpdate >= updateIntervalNano) {
                    engine.runSimulationStep();
                    updateView();
                    lastUpdate = now;
                }
            }
        };
    }

    private SimulationParameters createDefaultParameters() {
        return new SimulationParameters(
                2500,   // totalVoterCount
                65.0,   // globalMediaInfluence
                35.0,   // baseMobilityRate
                5.0,    // scandalChance
                50.0,   // initialLoyaltyMean
                1,      // simulationTicksPerSecond
                1.0,    // uniformRandomRange
                4       // numberOfParties
        );
    }

    private void updateView() {
        view.updateDashboard(engine.getParties(), engine.getVoters());
    }

    public void startSimulation() {
        simulationTimer.start();
    }

    public void pauseSimulation() {
        simulationTimer.stop();
    }

    // TODO: Add resetSimulation() and methods to update parameters from the View.
}
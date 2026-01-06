package de.schulprojekt.duv.controller;

import de.schulprojekt.duv.model.core.SimulationEngine;
import de.schulprojekt.duv.model.core.SimulationParameters;
import de.schulprojekt.duv.model.scandal.ScandalEvent;
import de.schulprojekt.duv.model.voter.VoterTransition;
import de.schulprojekt.duv.model.party.Party;
import de.schulprojekt.duv.view.DashboardController;
import javafx.application.Platform;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The controller connects the GUI (View) with the Simulation Engine (Model).
 */
public class SimulationController {

    private static final Logger LOGGER = Logger.getLogger(SimulationController.class.getName());

    // Defaults matches typical starting scenario
    private static final int DEFAULT_VOTERS = 2500;
    private static final double DEFAULT_MEDIA_INFLUENCE = 65.0;
    private static final double DEFAULT_MOBILITY = 35.0;
    private static final double DEFAULT_SCANDAL_CHANCE = 5.0;
    private static final double DEFAULT_LOYALTY = 50.0;
    private static final int DEFAULT_TPS = 5;
    private static final double DEFAULT_RANDOM_RANGE = 1.0;
    private static final int DEFAULT_PARTY_COUNT = 4;
    private static final double DEFAULT_BUDGET_FACTOR = 1.0;

    private final SimulationEngine engine;
    private final DashboardController view;
    private final ScheduledExecutorService executorService;
    private ScheduledFuture<?> simulationTask;
    private boolean isRunning = false;

    public SimulationController(DashboardController view) {
        this.view = view;

        SimulationParameters params = new SimulationParameters(
                DEFAULT_VOTERS,
                DEFAULT_MEDIA_INFLUENCE,
                DEFAULT_MOBILITY,
                DEFAULT_SCANDAL_CHANCE,
                DEFAULT_LOYALTY,
                DEFAULT_TPS,
                DEFAULT_RANDOM_RANGE,
                DEFAULT_PARTY_COUNT,
                DEFAULT_BUDGET_FACTOR
        );

        this.engine = new SimulationEngine(params);
        this.engine.initializeSimulation();

        this.executorService = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Simulation-Thread");
            t.setDaemon(true);
            return t;
        });
    }

    public void startSimulation() {
        if (isRunning) return;
        isRunning = true;
        scheduleTask();
        LOGGER.info("Simulation started.");
    }

    public void pauseSimulation() {
        isRunning = false;
        if (simulationTask != null) {
            simulationTask.cancel(false);
        }
        LOGGER.info("Simulation paused.");
    }

    public void resetSimulation() {
        pauseSimulation();
        executorService.execute(() -> {
            engine.resetState();
            // Create snapshot for safe UI update
            List<Party> partySnapshot = new ArrayList<>(engine.getParties());
            Platform.runLater(() -> view.updateDashboard(partySnapshot, List.of(), null, 0));
        });
        LOGGER.info("Simulation reset.");
    }

    public void updateSimulationSpeed(int factor) {
        SimulationParameters current = engine.getParameters();
        SimulationParameters updated = current.withSimulationTicksPerSecond(factor);

        executorService.execute(() -> {
            engine.updateParameters(updated);
            if (isRunning) {
                scheduleTask();
            }
        });
    }

    public void updateAllParameters(SimulationParameters p) {
        executorService.execute(() -> {
            engine.updateParameters(p);
            List<Party> partySnapshot = new ArrayList<>(engine.getParties());
            Platform.runLater(() ->
                    view.updateDashboard(partySnapshot, List.of(), null, engine.getCurrentStep())
            );
            if (isRunning) scheduleTask();
        });
    }

    public void shutdown() {
        executorService.shutdownNow();
        LOGGER.info("Simulation service stopped.");
    }

    private void scheduleTask() {
        if (simulationTask != null && !simulationTask.isCancelled()) {
            simulationTask.cancel(false);
        }

        int tps = engine.getParameters().getSimulationTicksPerSecond();
        long period = 1000 / (tps > 0 ? tps : 1);

        simulationTask = executorService.scheduleAtFixedRate(() -> {
            try {
                // 1. Calculation
                List<VoterTransition> transitions = engine.runSimulationStep();
                ScandalEvent scandal = engine.getLastScandal();
                int step = engine.getCurrentStep();

                // FIX: Snapshot of parties list to prevent ConcurrentModificationException in UI
                List<Party> partySnapshot = new ArrayList<>(engine.getParties());

                // 2. GUI Update
                Platform.runLater(() -> view.updateDashboard(partySnapshot, transitions, scandal, step));

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Critical error in simulation loop", e);
            }
        }, 0, period, TimeUnit.MILLISECONDS);
    }

    public SimulationParameters getCurrentParameters() {
        return engine.getParameters();
    }

    public List<Party> getParties() {
        // Return a copy to ensure safety if called from other threads
        return new ArrayList<>(engine.getParties());
    }

    public boolean isRunning() {
        return isRunning;
    }
}
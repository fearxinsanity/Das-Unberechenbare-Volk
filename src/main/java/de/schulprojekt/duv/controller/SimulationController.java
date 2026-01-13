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
 * Thread-Safety Update: All simulation control logic is now confined to the executorService.
 */
public class SimulationController {

    private static final Logger LOGGER = Logger.getLogger(SimulationController.class.getName());

    // Defaults matches typical starting scenario
    private static final int DEFAULT_POPULATION = 250000;
    private static final double DEFAULT_MEDIA_INFLUENCE = 65.0;
    private static final double DEFAULT_VOLATILITY = 35.0;
    private static final double DEFAULT_SCANDAL_PROB = 5.0;
    private static final double DEFAULT_LOYALTY = 50.0;
    private static final int DEFAULT_TICK_RATE = 5;
    private static final double DEFAULT_CHAOS = 1.0;
    private static final int DEFAULT_PARTIES = 4;
    private static final double DEFAULT_BUDGET_WEIGHT = 1.0;

    private final SimulationEngine engine;
    private final DashboardController view;
    // Single Thread ensures sequential execution of Start/Stop/Update commands
    private final ScheduledExecutorService executorService;

    private ScheduledFuture<?> simulationTask;

    // Volatile ensures visibility across threads
    private volatile boolean isRunning = false;

    public SimulationController(DashboardController view) {
        this.view = view;

        SimulationParameters params = new SimulationParameters(
                DEFAULT_POPULATION,
                DEFAULT_MEDIA_INFLUENCE,
                DEFAULT_VOLATILITY,
                DEFAULT_SCANDAL_PROB,
                DEFAULT_LOYALTY,
                DEFAULT_TICK_RATE,
                DEFAULT_CHAOS,
                DEFAULT_PARTIES,
                DEFAULT_BUDGET_WEIGHT
        );

        this.engine = new SimulationEngine(params);
        this.engine.initializeSimulation();

        this.executorService = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Simulation-Thread");
            t.setDaemon(true); // Important: Allows JVM to exit even if thread is running
            return t;
        });
    }

    public void startSimulation() {
        // Move logic to executor to prevent race conditions with updateParameters
        executorService.execute(() -> {
            if (isRunning) return;
            isRunning = true;
            scheduleTask();
            LOGGER.info("Simulation started.");
        });
    }

    public void pauseSimulation() {
        // Move logic to executor to ensure we cancel the CORRECT task reference
        executorService.execute(() -> {
            isRunning = false;
            stopCurrentTask();
            LOGGER.info("Simulation paused.");
        });
    }

    public void resetSimulation() {
        // Execute sequentially: First pause, then reset
        executorService.execute(() -> {
            // 1. Force Stop
            isRunning = false;
            stopCurrentTask();

            // 2. Reset Logic
            engine.resetState();

            // 3. UI Update (Snapshot)
            List<Party> partySnapshot = new ArrayList<>(engine.getParties());
            Platform.runLater(() -> view.updateDashboard(partySnapshot, List.of(), null, 0));

            LOGGER.info("Simulation reset.");
        });
    }

    public void updateSimulationSpeed(int factor) {
        // Parameter updates are already safe, but we fetch current inside the lambda to be sure
        executorService.execute(() -> {
            SimulationParameters current = engine.getParameters();
            SimulationParameters updated = current.withTickRate(factor);

            engine.updateParameters(updated);

            // Restart task with new speed if running
            if (isRunning) {
                scheduleTask();
            }
        });
    }

    public void updateAllParameters(SimulationParameters p) {
        executorService.execute(() -> {
            engine.updateParameters(p);

            List<Party> partySnapshot = new ArrayList<>(engine.getParties());
            int currentStep = engine.getCurrentStep();

            Platform.runLater(() ->
                    view.updateDashboard(partySnapshot, List.of(), null, currentStep)
            );

            // Restart task if running to apply new parameters (like tick rate or chaos) immediately
            if (isRunning) {
                scheduleTask();
            }
        });
    }

    public void shutdown() {
        // Disable new tasks and interrupt running ones
        executorService.shutdownNow();
        LOGGER.info("Simulation service stopped.");
    }

    /**
     * Helper to stop the scheduled task safely.
     * Must be called from inside executorService.
     */
    private void stopCurrentTask() {
        if (simulationTask != null) {
            // true = interrupt if running (helps if a step takes very long)
            simulationTask.cancel(true);
            simulationTask = null;
        }
    }

    /**
     * Schedules the simulation loop.
     * Must be called from inside executorService.
     */
    private void scheduleTask() {
        stopCurrentTask(); // Ensure old task is gone

        int tps = engine.getParameters().tickRate();
        long period = 1000 / (tps > 0 ? tps : 1);

        simulationTask = executorService.scheduleAtFixedRate(() -> {
            try {
                // Double check running state inside the loop
                if (!isRunning) return;

                // 1. Calculation
                List<VoterTransition> transitions = engine.runSimulationStep();
                ScandalEvent scandal = engine.getLastScandal();
                int step = engine.getCurrentStep();

                // Snapshot of parties list to prevent ConcurrentModificationException in UI
                List<Party> partySnapshot = new ArrayList<>(engine.getParties());

                // 2. GUI Update
                Platform.runLater(() -> view.updateDashboard(partySnapshot, transitions, scandal, step));

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Critical error in simulation loop", e);
                // Optional: Auto-Pause on error
                isRunning = false;
            }
        }, 0, period, TimeUnit.MILLISECONDS);
    }

    public SimulationParameters getCurrentParameters() {
        return engine.getParameters();
    }

    public List<Party> getParties() {
        return new ArrayList<>(engine.getParties());
    }

    public boolean isRunning() {
        return isRunning;
    }
}
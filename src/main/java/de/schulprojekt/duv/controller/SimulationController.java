package de.schulprojekt.duv.controller;

import de.schulprojekt.duv.model.core.SimulationEngine;
import de.schulprojekt.duv.model.core.SimulationParameters;
import de.schulprojekt.duv.model.party.Party;
import de.schulprojekt.duv.model.scandal.ScandalEvent;
import de.schulprojekt.duv.model.voter.VoterTransition;
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
 * Controller connecting the UI with the simulation engine.
 * * @author Nico Hoffmann
 * @version 1.1
 */
public class SimulationController {

    // ========================================
    // Static Variables
    // ========================================

    private static final Logger LOGGER = Logger.getLogger(SimulationController.class.getName());

    private static final int DEFAULT_POPULATION = 250_000;
    private static final double DEFAULT_MEDIA_INFLUENCE = 65.0;
    private static final double DEFAULT_VOLATILITY = 35.0;
    private static final double DEFAULT_SCANDAL_PROB = 5.0;
    private static final double DEFAULT_LOYALTY = 50.0;
    private static final int DEFAULT_TICK_RATE = 5;
    private static final double DEFAULT_CHAOS = 1.0;
    private static final int DEFAULT_PARTIES = 4;
    private static final double DEFAULT_BUDGET_WEIGHT = 1.0;

    // ========================================
    // Instance Variables
    // ========================================

    private final SimulationEngine engine;
    private final DashboardController view;
    private final ScheduledExecutorService executorService;
    private ScheduledFuture<?> simulationTask;
    private volatile boolean isRunning = false;

    // ========================================
    // Constructors
    // ========================================

    /**
     * Initializes the controller with default simulation values.
     * * @param view the dashboard controller for UI updates
     */
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
            t.setDaemon(true);
            return t;
        });
    }

    // ========================================
    // Getter Methods
    // ========================================

    public SimulationParameters getCurrentParameters() {
        return engine.getParameters();
    }

    public List<Party> getParties() {
        return new ArrayList<>(engine.getParties());
    }

    public boolean isRunning() {
        return isRunning;
    }

    // ========================================
    // Business Logic Methods
    // ========================================

    public void startSimulation() {
        executorService.execute(() -> {
            if (isRunning) return;
            isRunning = true;
            scheduleTask();
            LOGGER.info("Simulation started.");
        });
    }

    public void pauseSimulation() {
        executorService.execute(() -> {
            isRunning = false;
            stopCurrentTask();
            LOGGER.info("Simulation paused.");
        });
    }

    public void resetSimulation() {
        executorService.execute(() -> {
            isRunning = false;
            stopCurrentTask();
            engine.resetState();

            List<Party> partySnapshot = new ArrayList<>(engine.getParties());
            Platform.runLater(() -> view.updateDashboard(partySnapshot, List.of(), null, 0));

            LOGGER.info("Simulation reset.");
        });
    }

    public void updateSimulationSpeed(int factor) {
        executorService.execute(() -> {
            SimulationParameters current = engine.getParameters();
            engine.updateParameters(current.withTickRate(factor));

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

            Platform.runLater(() -> view.updateDashboard(partySnapshot, List.of(), null, currentStep));

            if (isRunning) {
                scheduleTask();
            }
        });
    }

    public void shutdown() {
        executorService.shutdownNow();
        LOGGER.info("Simulation service stopped.");
    }

    // ========================================
    // Utility Methods
    // ========================================

    private void stopCurrentTask() {
        if (simulationTask != null) {
            simulationTask.cancel(true);
            simulationTask = null;
        }
    }

    private void scheduleTask() {
        stopCurrentTask();

        int tps = engine.getParameters().tickRate();
        long period = 1000 / (tps > 0 ? tps : 1);

        simulationTask = executorService.scheduleAtFixedRate(this::runLoopStep, 0, period, TimeUnit.MILLISECONDS);
    }

    private void runLoopStep() {
        try {
            if (!isRunning) return;

            List<VoterTransition> transitions = engine.runSimulationStep();
            ScandalEvent scandal = engine.getLastScandal();
            int step = engine.getCurrentStep();
            List<Party> partySnapshot = new ArrayList<>(engine.getParties());

            Platform.runLater(() -> view.updateDashboard(partySnapshot, transitions, scandal, step));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Critical error in simulation loop", e);
            isRunning = false;
        }
    }
}
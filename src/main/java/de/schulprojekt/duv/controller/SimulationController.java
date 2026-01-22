package de.schulprojekt.duv.controller;

import de.schulprojekt.duv.model.core.SimulationEngine;
import de.schulprojekt.duv.model.core.SimulationParameters;
import de.schulprojekt.duv.model.party.Party;
import de.schulprojekt.duv.model.scandal.ScandalEvent;
import de.schulprojekt.duv.model.dto.VoterTransition;
import de.schulprojekt.duv.util.config.SimulationConfig;
import de.schulprojekt.duv.util.validation.ParameterValidator;
import de.schulprojekt.duv.util.validation.ValidationMessage;
import de.schulprojekt.duv.view.controllers.DashboardController;
import javafx.application.Platform;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Acts as the bridge between the user interface and the simulation logic.
 * <p>
 * It runs the simulation in its own background thread so the UI stays responsive
 * and doesn't freeze while calculations are happening.
 * </p>
 *
 * @author Nico Hoffmann
 * @version 1.2
 */
public class SimulationController {

    // ========================================
    // Static Variables
    // ========================================

    private static final Logger LOGGER = Logger.getLogger(SimulationController.class.getName());

    // ========================================
    // Instance Variables
    // ========================================

    private final SimulationEngine engine;
    private final DashboardController view;
    private final ScheduledExecutorService executorService;
    private ScheduledFuture<?> simulationTask;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    // ========================================
    // Constructors
    // ========================================

    /**
     * Sets up the controller and prepares the background thread.
     * <p>
     * I use a daemon thread here. It ensures that the simulation stops automatically when you close the application window.
     * Without it the process could still be running in the background.
     * </p>
     *
     * @param view the dashboard controller needed to update the UI
     */
    public SimulationController(DashboardController view) {
        this.view = view;

        SimulationParameters params = new SimulationParameters(
                SimulationConfig.DEFAULT_POPULATION,
                SimulationConfig.DEFAULT_MEDIA_INFLUENCE,
                SimulationConfig.DEFAULT_VOLATILITY,
                SimulationConfig.DEFAULT_SCANDAL_PROB,
                SimulationConfig.DEFAULT_LOYALTY,
                SimulationConfig.DEFAULT_TICK_RATE,
                SimulationConfig.DEFAULT_CHAOS,
                SimulationConfig.DEFAULT_PARTIES,
                SimulationConfig.DEFAULT_BUDGET_WEIGHT,
                SimulationConfig.DEFAULT_SEED
        );

        ParameterValidator.validate(params);

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

    /**
     * Gets a list of all parties.
     * <p>
     * Returns a copy instead of the original list.
     * It prevents the UI from accidentally messing up the internal data of the simulation.
     * </p>
     *
     * @return a safe copy of the party list
     */
    public List<Party> getParties() {
        return new ArrayList<>(engine.getParties());
    }

    public boolean isRunning() {
        return isRunning.get();
    }

    // ========================================
    // Business Logic Methods
    // ========================================

    /**
     * Starts the simulation loop.
     * <p>
     * Uses a special check {@code compareAndSet} to make sure I don't accidentally start
     * the simulation twice if the user clicks the button very fast.
     * </p>
     */
    public void startSimulation() {
        executorService.execute(() -> {
            if (!isRunning.compareAndSet(false, true)) return;
            scheduleTask();
            LOGGER.info("Simulation started.");
        });
    }

    public void pauseSimulation() {
        executorService.execute(() -> {
            isRunning.set(false);
            stopCurrentTask();
            LOGGER.info("Simulation paused.");
        });
    }

    public void resetSimulation() {
        executorService.execute(() -> {
            isRunning.set(false);
            stopCurrentTask();
            engine.resetState();

            List<Party> partySnapshot = new ArrayList<>(engine.getParties());
            Platform.runLater(() -> view.updateDashboard(partySnapshot, List.of(), null, 0));

            LOGGER.info("Simulation reset.");
        });
    }

    /**
     * Changes how fast the simulation runs.
     *
     * @param factor the new speed. I limit this value to make sure it doesn't
     * become too fast or invalid like 0 or negative, which would break the loop.
     */
    public void updateSimulationSpeed(int factor) {
        executorService.execute(() -> {
            int validFactor = ParameterValidator.clampInt(
                    factor,
                    ParameterValidator.getMinTickRate(),
                    ParameterValidator.getMaxTickRate()
            );

            SimulationParameters current = engine.getParameters();
            SimulationParameters updated = current.withTickRate(validFactor);

            ParameterValidator.validate(updated);

            engine.updateParameters(updated);

            if (isRunning.get()) {
                scheduleTask();
            }
        });
    }

    public void updateAllParameters(SimulationParameters p) {
        executorService.execute(() -> {
            if (ParameterValidator.isInvalid(p)) {
                LOGGER.warning(ValidationMessage.INVALID_PARAMETERS_REJECTED.format(
                        ParameterValidator.getValidationError(p)
                ));
                return;
            }

            engine.updateParameters(p);

            List<Party> partySnapshot = new ArrayList<>(engine.getParties());
            int currentStep = engine.getCurrentStep();

            Platform.runLater(() -> view.updateDashboard(partySnapshot, List.of(), null, currentStep));

            if (isRunning.get()) {
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

    /**
     * Runs a single step of the simulation.
     * <p>
     * <strong>Important:</strong> The whole logic is wrapped in a try-catch block.
     * If I don't do this, any small error would silently kill the background loop without
     * telling me why. With this I can catch and log the error.
     * </p>
     */
    private void runLoopStep() {
        try {
            if (!isRunning.get()) return;

            List<VoterTransition> transitions = engine.runSimulationStep();
            ScandalEvent scandal = engine.getLastScandal();
            int step = engine.getCurrentStep();
            List<Party> partySnapshot = new ArrayList<>(engine.getParties());

            Platform.runLater(() -> view.updateDashboard(partySnapshot, transitions, scandal, step));
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Runtime error in simulation loop - stopping simulation", e);
            isRunning.set(false);
        }
    }
}
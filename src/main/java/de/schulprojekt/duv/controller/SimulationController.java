package de.schulprojekt.duv.controller;

import de.schulprojekt.duv.model.core.SimulationEngine;
import de.schulprojekt.duv.model.core.SimulationParameters;
import de.schulprojekt.duv.model.scandal.ScandalEvent;
import de.schulprojekt.duv.model.voter.VoterTransition;
import de.schulprojekt.duv.model.party.Party;
import de.schulprojekt.duv.view.DashboardController;
import javafx.application.Platform;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SimulationController {

    private final SimulationEngine engine;
    private final DashboardController view;
    private final ScheduledExecutorService executorService;
    private ScheduledFuture<?> simulationTask;
    private boolean isRunning = false;

    public SimulationController(DashboardController view) {
        this.view = view;
        // Parameter Default
        SimulationParameters params = new SimulationParameters(2500, 65.0, 35.0, 5.0, 50.0, 5, 1.0, 4, 1.0);
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
    }

    private void scheduleTask() {
        if (simulationTask != null && !simulationTask.isCancelled()) {
            simulationTask.cancel(false);
        }

        int tps = engine.getParameters().getSimulationTicksPerSecond();
        long period = 1000 / (tps > 0 ? tps : 1);

        simulationTask = executorService.scheduleAtFixedRate(() -> {
            try {
                // Berechnung (Logik in Core -> Subsysteme)
                List<VoterTransition> transitions = engine.runSimulationStep();
                ScandalEvent scandal = engine.getLastScandal();
                int step = engine.getCurrentStep();
                List<Party> parties = engine.getParties();

                Platform.runLater(() -> {
                    view.updateDashboard(parties, transitions, scandal, step);
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, period, TimeUnit.MILLISECONDS);
    }

    public void pauseSimulation() {
        isRunning = false;
        if (simulationTask != null) simulationTask.cancel(false);
    }

    public void resetSimulation() {
        pauseSimulation();
        executorService.execute(() -> {
            engine.resetState();
            List<Party> parties = engine.getParties();
            Platform.runLater(() -> view.updateDashboard(parties, List.of(), null, 0));
        });
    }

    public void updateSimulationSpeed(int factor) {
        SimulationParameters p = engine.getParameters();
        p.setSimulationTicksPerSecond(1 * factor);
        updateAllParameters(p);
    }

    public void updateAllParameters(SimulationParameters p) {
        executorService.execute(() -> {
            engine.updateParameters(p);
            Platform.runLater(() ->
                    view.updateDashboard(engine.getParties(), List.of(), null, engine.getCurrentStep())
            );
            if (isRunning) scheduleTask();
        });
    }

    public SimulationParameters getCurrentParameters() { return engine.getParameters(); }
    public List<Party> getParties() { return engine.getParties(); }
    public boolean isRunning() { return isRunning; }
    public void shutdown() { executorService.shutdownNow(); }
}
package de.schulprojekt.duv.controller;

import de.schulprojekt.duv.model.engine.SimulationEngine;
import de.schulprojekt.duv.model.engine.SimulationParameters;
import de.schulprojekt.duv.model.entities.Party;
import de.schulprojekt.duv.model.entities.Voter;
import de.schulprojekt.duv.view.DashboardController;
import javafx.animation.AnimationTimer;
import javafx.animation.Animation;

import java.util.List;

public class SimulationController {

    private final SimulationEngine engine;
    private AnimationTimer simulationTimer;
    private boolean isRunning = false;
    private final DashboardController view;

    public SimulationController(DashboardController view) {
        SimulationParameters defaultParams = createDefaultParameters();
        this.engine = new SimulationEngine(defaultParams);
        this.engine.initializeSimulation();
        this.view = view;
        initializeTimer(defaultParams);
    }

    private SimulationParameters createDefaultParameters() {
        return new SimulationParameters(
                2500,
                65.0,
                35.0,
                5.0,
                50.0,
                1,
                1.0,
                4
        );
    }

    private void initializeTimer(SimulationParameters params) {
        int ticksPerSecond = params.getSimulationTicksPerSecond();
        if (ticksPerSecond <= 0) {
            ticksPerSecond = 1;
        }

        long updateIntervalNano = 1_000_000_000L / ticksPerSecond;

        this.simulationTimer = new AnimationTimer() {
            private long lastUpdate = 0;

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

    private void updateView() {
        view.updateDashboard(engine.getParties(), engine.getVoters());
    }

    public void startSimulation() {
        simulationTimer.start();
        this.isRunning = true;
    }

    public void pauseSimulation() {
        simulationTimer.stop();
        this.isRunning = false;
    }

    public void resetSimulation() {
        simulationTimer.stop();
        engine.resetState();
        updateView();
    }

    public SimulationParameters getCurrentParameters() {
        return engine.getParameters();
    }

    public void updateAllParameters(SimulationParameters newParams) {
        boolean wasRunning = this.isRunning;
        SimulationParameters currentParams = engine.getParameters();

        boolean needsReset =
                newParams.getTotalVoterCount() != currentParams.getTotalVoterCount() ||
                        newParams.getNumberOfParties() != currentParams.getNumberOfParties();

        engine.updateParameters(newParams);

        if (newParams.getSimulationTicksPerSecond() != currentParams.getSimulationTicksPerSecond()) {
            initializeTimer(newParams);
        }

        if (wasRunning) {
            simulationTimer.start();
        }

        if (needsReset) {
            engine.resetState();
        }

        updateView();
    }

    public void updateSimulationSpeed(int newTicksPerSecond) {
        boolean wasRunning = this.isRunning;

        SimulationParameters currentParams = engine.getParameters();

        SimulationParameters newParams = new SimulationParameters(
                currentParams.getTotalVoterCount(),
                currentParams.getGlobalMediaInfluence(),
                currentParams.getBaseMobilityRate(),
                currentParams.getScandalChance(),
                currentParams.getInitialLoyaltyMean(),
                newTicksPerSecond,
                currentParams.getUniformRandomRange(),
                currentParams.getNumberOfParties()
        );

        engine.updateParameters(newParams);
        initializeTimer(newParams);

        if (wasRunning) {
            simulationTimer.start();
        }
    }

    public List<Voter> getVoters() {
        return engine.getVoters();
    }

    public List<Party> getParties() {
        return engine.getParties();
    }

    public boolean isRunning() {
        return this.isRunning;
    }
}
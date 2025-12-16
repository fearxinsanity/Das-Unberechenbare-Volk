package de.schulprojekt.duv.controller;

import de.schulprojekt.duv.model.engine.ScandalEvent;
import de.schulprojekt.duv.model.engine.SimulationEngine;
import de.schulprojekt.duv.model.engine.SimulationParameters;
import de.schulprojekt.duv.model.engine.VoterTransition;
import de.schulprojekt.duv.model.entities.Party;
import de.schulprojekt.duv.model.entities.Voter;
import de.schulprojekt.duv.view.DashboardController;
import javafx.animation.AnimationTimer;

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
                4); // Removed: 120 seconds default value
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
                    // Simulation ausführen
                    List<VoterTransition> transitions = engine.runSimulationStep();

                    // Skandal abrufen (falls einer aufgetreten ist)
                    ScandalEvent scandal = engine.getLastScandal();

                    // Aktuelle Schritte
                    int currentStep = engine.getCurrentStep();
                    // int totalSteps = engine.getParameters().getTotalSimulationTicks(); // Removed

                    // Dashboard mit allen Daten aktualisieren
                    view.updateDashboard(
                            engine.getParties(),
                            engine.getVoters(),
                            transitions,
                            scandal,
                            currentStep
                            // Removed: totalSteps
                    );

                    // Removed: Simulation beenden wenn fertig
                    // Die Simulation läuft nun unbegrenzt.

                    lastUpdate = now;
                }
            }
        };
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
        this.isRunning = false;
        engine.resetState();
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
                currentParams.getNumberOfParties()); // Removed: currentParams.getSimulationDurationSeconds()

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
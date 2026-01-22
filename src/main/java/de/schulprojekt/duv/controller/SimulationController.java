package de.schulprojekt.duv.controller;

import de.schulprojekt.duv.model.core.SimulationEngine;
import de.schulprojekt.duv.model.core.SimulationParameters;
import de.schulprojekt.duv.model.party.Party;
import de.schulprojekt.duv.model.party.PartyRegistry;
import de.schulprojekt.duv.model.random.DistributionProvider;
import de.schulprojekt.duv.model.scandal.ScandalEvent;
import de.schulprojekt.duv.model.scandal.ScandalImpactCalculator;
import de.schulprojekt.duv.model.scandal.ScandalScheduler;
import de.schulprojekt.duv.model.voter.VoterBehavior;
import de.schulprojekt.duv.model.voter.VoterPopulation;
import de.schulprojekt.duv.model.voter.ZeitgeistManager;
import de.schulprojekt.duv.model.dto.VoterTransition;
import de.schulprojekt.duv.util.config.SimulationConfig;
import de.schulprojekt.duv.util.io.CSVLoader;
import de.schulprojekt.duv.util.validation.ParameterValidator;
import de.schulprojekt.duv.util.validation.ValidationMessage;
import de.schulprojekt.duv.view.Main;
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
 * Verbindet UI und Logik und agiert als Composition Root.
 * <p>
 * Diese Klasse hat zwei Verantwortlichkeiten:
 *  * 1. Sie baut den gesamten Objektgraphen auf.
 *  * 2. Sie steuert den Simulations-Thread, damit die Berechnung die UI nicht blockiert.
 * </p>
 *
 * @author Nico Hoffmann
 * @version 1.0
 */
public class SimulationController {

    // ========================================
    // Static Variables
    // ========================================

    private static final Logger LOGGER = Logger.getLogger(SimulationController.class.getName());
    private static final int CALCULATOR_CAPACITY_BUFFER = 10;

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
     * Initialisiert den Controller und die Simulationsumgebung.
     * <p>
     * Hier werden alle notwendigen Komponenten zentral instanziiert
     * und an die {@link SimulationEngine} übergeben.
     * Dies stellt sicher, dass die Engine ihre Abhängigkeiten kennt, ohne selbst auf globale Zustände zugreifen zu müssen.
     * </p>
     *
     * @param view der GUI, wird für die aktualisierung der UI benötigt.
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

        CSVLoader csvLoader = new CSVLoader(Main.getLocale());
        DistributionProvider distributionProvider = new DistributionProvider();
        distributionProvider.initialize(params);
        PartyRegistry partyRegistry = new PartyRegistry(csvLoader);
        VoterPopulation voterPopulation = new VoterPopulation();
        VoterBehavior voterBehavior = new VoterBehavior();
        ZeitgeistManager zeitgeistManager = new ZeitgeistManager();
        ScandalScheduler scandalScheduler = new ScandalScheduler(distributionProvider);
        ScandalImpactCalculator impactCalculator = new ScandalImpactCalculator(params.partyCount() + CALCULATOR_CAPACITY_BUFFER);

        this.engine = new SimulationEngine(
                params,
                csvLoader,
                distributionProvider,
                partyRegistry,
                voterPopulation,
                voterBehavior,
                zeitgeistManager,
                scandalScheduler,
                impactCalculator
        );
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
     * Ruft eine Liste aller Parteien ab.
     * <p>
     * Gibt eine Kopie der original Liste zurück.
     * Verhindert das Verfälschen der Daten durch die UI.
     * </p>
     *
     * @return eine sichere Kopie der Parteienliste.
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
     * Startet die Simulationsschleife.
     * <p>
     * Verwendet {@code compareAndSet} um sicherzustellen,
     * dass die Simulation nicht doppelt gestartet wird.
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
     * Ändert die Simulationsgeschwindigkeit.
     *
     * @param factor die neue Geschwindigkeit. Der Wert wird begrenzt, damit er nicht
     * zu schnell oder ungültig wird, da sonst die Schleife unterbrochen wird.
     *
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
     * Führt einen einzelnen Simulationsschritt aus.
     * <p>
     * <strong>Wichtig:</strong> Die gesamte Logik ist in einem try-catch-Block gekapselt.
     * Ohne diesen würde jeder kleine Fehler die Hintergrundschleife ohne Meldung beenden.
     * Hiermit wird der Fehler abgefangen und protokolliert.
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
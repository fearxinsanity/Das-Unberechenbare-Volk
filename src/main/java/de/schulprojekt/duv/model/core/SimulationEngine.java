package de.schulprojekt.duv.model.core;

import de.schulprojekt.duv.model.party.Party;
import de.schulprojekt.duv.model.scandal.Scandal;
import de.schulprojekt.duv.model.party.PartyRegistry;
import de.schulprojekt.duv.model.random.DistributionProvider;
import de.schulprojekt.duv.model.scandal.ScandalEvent;
import de.schulprojekt.duv.model.scandal.ScandalImpactCalculator;
import de.schulprojekt.duv.model.scandal.ScandalScheduler;
import de.schulprojekt.duv.model.voter.VoterBehavior;
import de.schulprojekt.duv.model.voter.VoterPopulation;
import de.schulprojekt.duv.model.voter.VoterTransition;
import de.schulprojekt.duv.util.CSVLoader;
import de.schulprojekt.duv.util.SimulationConfig;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Der Kern der Simulation (Orchestrator).
 * Diese Klasse verbindet alle Subsysteme (Wähler, Parteien, Skandale) und steuert den zeitlichen Ablauf.
 * Sie enthält keine Detail-Logik mehr, sondern delegiert diese an spezialisierte Klassen.
 */
public class SimulationEngine {

    // Zustand und Konfiguration
    private final SimulationState state;
    private SimulationParameters parameters;

    // Infrastruktur
    private final CSVLoader csvLoader;
    private final Random random = new Random();

    // Subsysteme (Modules)
    private final DistributionProvider distributionProvider;
    private final PartyRegistry partyRegistry;
    private final VoterPopulation voterPopulation;
    private final VoterBehavior voterBehavior;
    private final ScandalScheduler scandalScheduler;
    private final ScandalImpactCalculator impactCalculator;

    public SimulationEngine(SimulationParameters params) {
        this.parameters = params;
        this.state = new SimulationState();
        this.csvLoader = new CSVLoader();

        // 1. Zufallsverteilungen initialisieren
        this.distributionProvider = new DistributionProvider();
        this.distributionProvider.initialize(params);

        // 2. Parteien-Verwaltung erstellen
        this.partyRegistry = new PartyRegistry(csvLoader);

        // 3. Wähler-Daten und Verhalten
        this.voterPopulation = new VoterPopulation();
        this.voterBehavior = new VoterBehavior();

        // 4. Skandal-Systeme
        this.scandalScheduler = new ScandalScheduler(distributionProvider);
        // Puffergröße für Array (Anzahl Parteien + Reserve)
        this.impactCalculator = new ScandalImpactCalculator(params.getNumberOfParties() + 10);
    }

    /**
     * Setzt die Simulation komplett zurück und initialisiert Population sowie Parteien neu.
     */
    public void initializeSimulation() {
        state.reset();
        scandalScheduler.reset();
        impactCalculator.reset();

        // Parteien laden und aufstellen
        partyRegistry.initializeParties(parameters);

        // Wählerpopulation generieren (sofort parallelisiert)
        voterPopulation.initialize(
                parameters.getTotalVoterCount(),
                partyRegistry.getParties().size(),
                distributionProvider
        );

        // Initiale Zählung der Stimmen durchführen
        recalculateCounts();
    }

    /**
     * Reagiert auf Änderungen an den Parametern (z.B. Slider in der GUI).
     */
    public void updateParameters(SimulationParameters newParams) {
        // Prüfen, ob ein "harter" Reset nötig ist (Strukturänderung)
        boolean structuralChange = (newParams.getNumberOfParties() != parameters.getNumberOfParties()) ||
                (newParams.getTotalVoterCount() != parameters.getTotalVoterCount());

        this.parameters = newParams;
        distributionProvider.initialize(newParams);

        if (structuralChange) {
            initializeSimulation();
        }
    }

    /**
     * Führt einen einzelnen Simulationsschritt (Tick) aus.
     * @return Eine Liste von Wählerwanderungen für die Visualisierung.
     */
    public List<VoterTransition> runSimulationStep() {
        state.incrementStep();

        // 1. Skandal-Management
        // Alte Skandale entfernen (älter als 200 Ticks)
        state.getActiveScandals().removeIf(e -> state.getCurrentStep() - e.getOccurredAtStep() > 200);

        // Prüfen, ob ein neuer Skandal passiert (Exponentialverteilung)
        if (scandalScheduler.shouldScandalOccur() && partyRegistry.getParties().size() > 1) {
            triggerNewScandal();
        }

        // 2. Skandal-Auswirkungen berechnen
        // WICHTIG: Wir holen hier nur den AKUTEN Druck.
        // Der permanente Schaden wird im VoterBehavior separat verrechnet.
        double[] acutePressures = impactCalculator.calculateAcutePressure(
                state.getActiveScandals(),
                partyRegistry.getParties(),
                state.getCurrentStep()
        );

        // Erholung von permanenten Schäden berechnen (Gras wächst über die Sache)
        impactCalculator.processRecovery(partyRegistry.getParties(), parameters.getTotalVoterCount());

        // 3. Wähler-Entscheidungen (Parallel Processing)
        List<VoterTransition> transitions = voterBehavior.processVoterDecisions(
                voterPopulation,
                partyRegistry.getParties(),
                parameters,
                acutePressures,
                impactCalculator
        );

        return transitions;
    }

    /**
     * Löst einen neuen zufälligen Skandal aus.
     */
    private void triggerNewScandal() {
        // Filter: "Unsicher" (Index 0) kann keinen Skandal haben
        List<Party> realParties = partyRegistry.getParties().stream()
                .filter(p -> !p.getName().equals(SimulationConfig.UNDECIDED_NAME))
                .collect(Collectors.toList());

        if (!realParties.isEmpty()) {
            // Zufälliges Ziel und Skandal-Typ wählen
            Party target = realParties.get(random.nextInt(realParties.size()));
            Scandal s = csvLoader.getRandomScandal();

            // Event erzeugen und im State speichern
            ScandalEvent event = new ScandalEvent(s, target, state.getCurrentStep());
            state.addScandal(event);

            // Statistik auf der Partei erhöhen
            target.incrementScandalCount();
        }
    }

    /**
     * Zählt die Anhänger aller Parteien einmal komplett durch (für Initialisierung).
     * Im laufenden Betrieb nutzt VoterBehavior effizientere Delta-Updates.
     */
    private void recalculateCounts() {
        int[] counts = new int[partyRegistry.getParties().size()];
        int maxIdx = counts.length - 1;

        // Iteriert über alle Wähler (schneller Array-Zugriff)
        for (int i = 0; i < voterPopulation.size(); i++) {
            int idx = voterPopulation.getPartyIndex(i);
            if (idx <= maxIdx) {
                counts[idx]++;
            }
        }

        partyRegistry.updateSupporterCounts(counts);
    }

    // --- Getter und Delegate-Methoden ---

    public List<Party> getParties() {
        return partyRegistry.getParties();
    }

    public ScandalEvent getLastScandal() {
        return state.consumeLastScandal();
    }

    public int getCurrentStep() {
        return state.getCurrentStep();
    }

    public SimulationParameters getParameters() {
        return parameters;
    }

    public void resetState() {
        initializeSimulation();
    }
}
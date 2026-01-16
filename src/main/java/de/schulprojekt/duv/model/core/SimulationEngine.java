package de.schulprojekt.duv.model.core;

import de.schulprojekt.duv.model.party.Party;
import de.schulprojekt.duv.model.party.PartyRegistry;
import de.schulprojekt.duv.model.random.DistributionProvider;
import de.schulprojekt.duv.model.scandal.Scandal;
import de.schulprojekt.duv.model.scandal.ScandalEvent;
import de.schulprojekt.duv.model.scandal.ScandalImpactCalculator;
import de.schulprojekt.duv.model.scandal.ScandalScheduler;
import de.schulprojekt.duv.model.voter.VoterBehavior;
import de.schulprojekt.duv.model.voter.VoterPopulation;
import de.schulprojekt.duv.model.dto.VoterTransition;
import de.schulprojekt.duv.util.io.CSVLoader;
import de.schulprojekt.duv.util.config.SimulationConfig;

import java.util.List;
import java.util.Random;

/**
 * Orchestrator class for the simulation logic.
 * @author Nico Hoffmann
 * @version 1.1
 */
public class SimulationEngine {

    // ========================================
    // Instance Variables
    // ========================================

    private final SimulationState state;
    private SimulationParameters parameters;
    private final CSVLoader csvLoader;
    private final Random random = new Random();
    private final DistributionProvider distributionProvider;
    private final PartyRegistry partyRegistry;
    private final VoterPopulation voterPopulation;
    private final VoterBehavior voterBehavior;
    private final ScandalScheduler scandalScheduler;
    private final ScandalImpactCalculator impactCalculator;

    // ========================================
    // Constructors
    // ========================================

    /**
     * Constructs a new SimulationEngine with the given parameters.
     * @param params the initial simulation parameters
     */
    public SimulationEngine(SimulationParameters params) {
        this.parameters = params;
        this.state = new SimulationState();
        this.csvLoader = new CSVLoader();

        this.distributionProvider = new DistributionProvider();
        this.distributionProvider.initialize(params);

        this.partyRegistry = new PartyRegistry(csvLoader);
        this.voterPopulation = new VoterPopulation();
        this.voterBehavior = new VoterBehavior();

        this.scandalScheduler = new ScandalScheduler(distributionProvider);
        this.impactCalculator = new ScandalImpactCalculator(params.partyCount() + 10);
    }

    // ========================================
    // Getter Methods
    // ========================================

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

    // ========================================
    // Business Logic Methods
    // ========================================

    public void initializeSimulation() {
        state.reset();
        scandalScheduler.reset();
        impactCalculator.reset();

        partyRegistry.initializeParties(parameters, distributionProvider);

        voterPopulation.initialize(
                parameters.populationSize(),
                partyRegistry.getParties().size(),
                distributionProvider
        );

        recalculateCounts();
    }

    public List<VoterTransition> runSimulationStep() {
        state.incrementStep();

        state.getActiveScandals().removeIf(e -> state.getCurrentStep() - e.occurredAtStep() > SimulationConfig.SCANDAL_MAX_AGE_TICKS);

        if (scandalScheduler.shouldScandalOccur() && partyRegistry.getParties().size() > 1) {
            triggerNewScandal();
        }

        double[] acutePressures = impactCalculator.calculateAcutePressure(
                state.getActiveScandals(),
                partyRegistry.getParties(),
                state.getCurrentStep()
        );

        impactCalculator.processRecovery(partyRegistry.getParties(), parameters.populationSize());

        List<VoterTransition> transitions = voterBehavior.processVoterDecisions(
                voterPopulation,
                partyRegistry.getParties(),
                parameters,
                acutePressures,
                impactCalculator
        );
        recalculateCounts();

        return transitions;
    }

    public void updateParameters(SimulationParameters newParams) {
        boolean structuralChange = (newParams.partyCount() != parameters.partyCount()) ||
                (newParams.populationSize() != parameters.populationSize());

        this.parameters = newParams;
        distributionProvider.initialize(newParams);

        if (structuralChange) {
            initializeSimulation();
        }
    }

    public void resetState() {
        initializeSimulation();
    }

    // ========================================
    // Utility Methods
    // ========================================

    private void triggerNewScandal() {
        List<Party> realParties = partyRegistry.getParties().stream()
                .filter(p -> !p.getName().equals(SimulationConfig.UNDECIDED_NAME))
                .toList();

        if (!realParties.isEmpty()) {
            Party target = realParties.get(random.nextInt(realParties.size()));
            Scandal s = csvLoader.getRandomScandal();

            ScandalEvent event = new ScandalEvent(s, target, state.getCurrentStep());
            state.addScandal(event);
            target.incrementScandalCount();
        }
    }

    private void recalculateCounts() {
        int[] counts = new int[partyRegistry.getParties().size()];
        int maxIdx = counts.length - 1;

        for (int i = 0; i < voterPopulation.size(); i++) {
            int idx = voterPopulation.getPartyIndex(i);
            if (idx <= maxIdx) {
                counts[idx]++;
            }
        }

        partyRegistry.updateSupporterCounts(counts);
    }
}
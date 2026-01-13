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
import de.schulprojekt.duv.model.voter.VoterTransition;
import de.schulprojekt.duv.util.CSVLoader;
import de.schulprojekt.duv.util.SimulationConfig;

import java.util.List;
import java.util.Random;

/**
 * The core simulation orchestrator.
 */
public class SimulationEngine {

    // --- CONSTANTS ---
    private static final int SCANDAL_MAX_AGE_TICKS = 200;

    // --- FIELDS ---
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

    // --- CONSTRUCTOR ---
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
        this.impactCalculator = new ScandalImpactCalculator(params.partyCount() + 10); // REF
    }

    // --- INITIALIZATION ---

    public void initializeSimulation() {
        state.reset();
        scandalScheduler.reset();
        impactCalculator.reset();

        partyRegistry.initializeParties(parameters, distributionProvider);

        voterPopulation.initialize(
                parameters.populationSize(), // REF
                partyRegistry.getParties().size(),
                distributionProvider
        );

        recalculateCounts();
    }

    // --- MAIN LOGIC ---

    public List<VoterTransition> runSimulationStep() {
        state.incrementStep();

        // 1. Scandal Management
        state.getActiveScandals().removeIf(e -> state.getCurrentStep() - e.occurredAtStep() > SCANDAL_MAX_AGE_TICKS);

        if (scandalScheduler.shouldScandalOccur() && partyRegistry.getParties().size() > 1) {
            triggerNewScandal();
        }

        // 2. Calculate Scandal Impact
        double[] acutePressures = impactCalculator.calculateAcutePressure(
                state.getActiveScandals(),
                partyRegistry.getParties(),
                state.getCurrentStep()
        );

        impactCalculator.processRecovery(partyRegistry.getParties(), parameters.populationSize()); // REF

        // 3. Voter Decisions
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
        boolean structuralChange = (newParams.partyCount() != parameters.partyCount()) || // REF
                (newParams.populationSize() != parameters.populationSize());              // REF

        this.parameters = newParams;
        distributionProvider.initialize(newParams);

        if (structuralChange) {
            initializeSimulation();
        }
    }

    public void resetState() {
        initializeSimulation();
    }

    // --- HELPER METHODS ---

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

    // --- GETTERS ---

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
}
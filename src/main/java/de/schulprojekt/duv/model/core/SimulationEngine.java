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
import java.util.stream.Collectors;

/**
 * The core simulation orchestrator.
 * Connects all subsystems (Voters, Parties, Scandals) and manages the timeline (Ticks).
 * Does not contain calculation logic itself, but delegates to specialized modules.
 */
public class SimulationEngine {

    // --- CONSTANTS (Configuration) ---
    // Defines how long a scandal remains in the active list before being removed
    private static final int SCANDAL_MAX_AGE_TICKS = 200;

    // --- FIELDS (State) ---
    private final SimulationState state;
    private SimulationParameters parameters;

    // --- FIELDS (Infrastructure) ---
    private final CSVLoader csvLoader;
    private final Random random = new Random();

    // --- FIELDS (Subsystems) ---
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

        // 1. Initialize Random Distributions
        this.distributionProvider = new DistributionProvider();
        this.distributionProvider.initialize(params);

        // 2. Party Management
        this.partyRegistry = new PartyRegistry(csvLoader);

        // 3. Voter Data & Behavior
        this.voterPopulation = new VoterPopulation();
        this.voterBehavior = new VoterBehavior();

        // 4. Scandal Systems
        this.scandalScheduler = new ScandalScheduler(distributionProvider);
        // Buffer size for array (Number of parties + Reserve)
        this.impactCalculator = new ScandalImpactCalculator(params.getNumberOfParties() + 10);
    }

    // --- INITIALIZATION ---

    /**
     * Resets the simulation completely and re-initializes population and parties.
     */
    public void initializeSimulation() {
        state.reset();
        scandalScheduler.reset();
        impactCalculator.reset();

        // Load and setup parties
        partyRegistry.initializeParties(parameters, distributionProvider);

        // Generate voter population (parallelized)
        voterPopulation.initialize(
                parameters.getTotalVoterCount(),
                partyRegistry.getParties().size(),
                distributionProvider
        );

        // Perform initial vote count
        recalculateCounts();
    }

    // --- MAIN LOGIC (Public Methods) ---

    /**
     * Executes a single simulation step (Tick).
     * @return List of voter transitions for visualization.
     */
    public List<VoterTransition> runSimulationStep() {
        state.incrementStep();

        // 1. Scandal Management
        // Remove old scandals
        state.getActiveScandals().removeIf(e -> state.getCurrentStep() - e.occurredAtStep() > SCANDAL_MAX_AGE_TICKS);

        // Check if a new scandal occurs (Exponential Distribution)
        if (scandalScheduler.shouldScandalOccur() && partyRegistry.getParties().size() > 1) {
            triggerNewScandal();
        }

        // 2. Calculate Scandal Impact
        // IMPORTANT: We only fetch ACUTE pressure here.
        // Permanent damage is handled separately in VoterBehavior.
        double[] acutePressures = impactCalculator.calculateAcutePressure(
                state.getActiveScandals(),
                partyRegistry.getParties(),
                state.getCurrentStep()
        );

        // Process recovery from permanent damages
        impactCalculator.processRecovery(partyRegistry.getParties(), parameters.getTotalVoterCount());

        // 3. Voter Decisions (Parallel Processing)
        return voterBehavior.processVoterDecisions(
                voterPopulation,
                partyRegistry.getParties(),
                parameters,
                acutePressures,
                impactCalculator
        );
    }

    /**
     * Reacts to parameter changes (e.g., from GUI Sliders).
     */
    public void updateParameters(SimulationParameters newParams) {
        // Check for structural changes (requires to be reset)
        boolean structuralChange = (newParams.getNumberOfParties() != parameters.getNumberOfParties()) ||
                (newParams.getTotalVoterCount() != parameters.getTotalVoterCount());

        this.parameters = newParams;
        distributionProvider.initialize(newParams);

        if (structuralChange) {
            // Re-initialize if structure changes (e.g. array sizes changed)
            initializeSimulation();
        }
    }

    public void resetState() {
        initializeSimulation();
    }

    // --- HELPER METHODS (Private) ---

    /**
     * Triggers a new random scandal.
     */
    private void triggerNewScandal() {
        // Filter: "Undecided" (Index 0) cannot have a scandal
        List<Party> realParties = partyRegistry.getParties().stream()
                .filter(p -> !p.getName().equals(SimulationConfig.UNDECIDED_NAME))
                .collect(Collectors.toList());

        if (!realParties.isEmpty()) {
            // Select random target and scandal type
            Party target = realParties.get(random.nextInt(realParties.size()));
            Scandal s = csvLoader.getRandomScandal();

            // Create event and store in state
            ScandalEvent event = new ScandalEvent(s, target, state.getCurrentStep());
            state.addScandal(event);

            // Update party statistics
            target.incrementScandalCount();
        }
    }

    /**
     * Counts supporters for all parties once (Initial Setup).
     * During runtime, VoterBehavior uses efficient delta updates.
     */
    private void recalculateCounts() {
        int[] counts = new int[partyRegistry.getParties().size()];
        int maxIdx = counts.length - 1;

        // Iterate over all voters (fast array access)
        for (int i = 0; i < voterPopulation.size(); i++) {
            int idx = voterPopulation.getPartyIndex(i);
            if (idx <= maxIdx) {
                counts[idx]++;
            }
        }

        partyRegistry.updateSupporterCounts(counts);
    }

    // --- GETTERS & SETTERS ---

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
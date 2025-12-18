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

public class SimulationEngine {

    private final SimulationState state;
    private SimulationParameters parameters;
    private final CSVLoader csvLoader;
    private final Random random = new Random();

    // Subsystems
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

        this.distributionProvider = new DistributionProvider();
        this.distributionProvider.initialize(params);

        this.partyRegistry = new PartyRegistry(csvLoader);
        this.voterPopulation = new VoterPopulation();
        this.voterBehavior = new VoterBehavior();

        this.scandalScheduler = new ScandalScheduler(distributionProvider);
        this.impactCalculator = new ScandalImpactCalculator(params.getNumberOfParties() + 5);
    }

    public void initializeSimulation() {
        state.reset();
        scandalScheduler.reset();
        impactCalculator.reset();

        partyRegistry.initializeParties(parameters);
        voterPopulation.initialize(
                parameters.getTotalVoterCount(),
                partyRegistry.getParties().size(),
                distributionProvider
        );
        recalculateCounts();
    }

    public void updateParameters(SimulationParameters newParams) {
        boolean structuralChange = (newParams.getNumberOfParties() != parameters.getNumberOfParties()) ||
                (newParams.getTotalVoterCount() != parameters.getTotalVoterCount());
        this.parameters = newParams;
        distributionProvider.initialize(newParams);

        if (structuralChange) {
            initializeSimulation();
        }
    }

    public List<VoterTransition> runSimulationStep() {
        state.incrementStep();

        // 1. Skandale managen
        state.getActiveScandals().removeIf(e -> state.getCurrentStep() - e.getOccurredAtStep() > 200);

        if (scandalScheduler.shouldScandalOccur() && partyRegistry.getParties().size() > 1) {
            triggerNewScandal();
        }

        // 2. Auswirkungen berechnen
        double[] pressures = impactCalculator.calculateCurrentPressure(
                state.getActiveScandals(),
                partyRegistry.getParties(),
                state.getCurrentStep()
        );
        impactCalculator.processRecovery(partyRegistry.getParties(), parameters.getTotalVoterCount());

        // 3. Wählerwanderung
        List<VoterTransition> transitions = voterBehavior.processVoterDecisions(
                voterPopulation,
                partyRegistry.getParties(),
                parameters,
                pressures,
                impactCalculator
        );

        return transitions;
    }

    private void triggerNewScandal() {
        List<Party> realParties = partyRegistry.getParties().stream()
                .filter(p -> !p.getName().equals(SimulationConfig.UNDECIDED_NAME))
                .collect(Collectors.toList());

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
            if (idx <= maxIdx) counts[idx]++;
        }
        partyRegistry.updateSupporterCounts(counts);
    }

    // Getter Delegate
    public List<Party> getParties() { return partyRegistry.getParties(); }
    public ScandalEvent getLastScandal() { return state.consumeLastScandal(); }
    public int getCurrentStep() { return state.getCurrentStep(); }
    public SimulationParameters getParameters() { return parameters; }
    public void resetState() { initializeSimulation(); }
}
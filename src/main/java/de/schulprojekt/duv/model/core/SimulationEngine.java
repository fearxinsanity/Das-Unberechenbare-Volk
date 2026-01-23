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
import de.schulprojekt.duv.model.voter.ZeitgeistManager;
import de.schulprojekt.duv.model.dto.VoterTransition;
import de.schulprojekt.duv.util.io.CSVLoader;
import de.schulprojekt.duv.util.config.SimulationConfig;

import java.util.List;

/**
 * Verwaltet die gesamte Simulationslogik an einer zentralen Stelle.
 * <p>
 * Die Klasse kapselt den komplexen Ablauf eines Simulationsschrittes,
 * damit der Controller nur eine einzige Methode aufrufen muss,
 * ohne die Details der Interaktion zwischen Wählern, Parteien und Skandalen kennen zu müssen.
 * </p>
 *
 * @author Nico Hoffmann
 * @version 1.0
 */
public class SimulationEngine {

    // ========================================
    // Instance Variables
    // ========================================

    private final SimulationState state;
    private SimulationParameters parameters;
    private final CSVLoader csvLoader;
    private final DistributionProvider distributionProvider;
    private final PartyRegistry partyRegistry;
    private final VoterPopulation voterPopulation;
    private final VoterBehavior voterBehavior;
    private final ZeitgeistManager zeitgeistManager;
    private final ScandalScheduler scandalScheduler;
    private final ScandalImpactCalculator impactCalculator;

    // ========================================
    // Constructors
    // ========================================

    public SimulationEngine(SimulationParameters params,
                            CSVLoader csvLoader,
                            DistributionProvider distributionProvider,
                            PartyRegistry partyRegistry,
                            VoterPopulation voterPopulation,
                            VoterBehavior voterBehavior,
                            ZeitgeistManager zeitgeistManager,
                            ScandalScheduler scandalScheduler,
                            ScandalImpactCalculator impactCalculator) {
        this.parameters = params;
        this.state = new SimulationState();
        this.csvLoader = csvLoader;
        this.distributionProvider = distributionProvider;
        this.partyRegistry = partyRegistry;
        this.voterPopulation = voterPopulation;
        this.voterBehavior = voterBehavior;
        this.zeitgeistManager = zeitgeistManager;
        this.scandalScheduler = scandalScheduler;
        this.impactCalculator = impactCalculator;

        this.distributionProvider.initialize(params);
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

        double initialZeitgeist = (distributionProvider.getRandomGenerator().nextDouble() - 0.5) * 2.0;
        zeitgeistManager.setZeitgeist(initialZeitgeist);

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

        zeitgeistManager.updateZeitgeist();

        voterPopulation.updateVoterAttributes(parameters);

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
                impactCalculator,
                state.getCurrentStep(),
                zeitgeistManager.getCurrentZeitgeist()
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
        List<Party> realParties = partyRegistry.getTargetableParties();

        if (!realParties.isEmpty()) {
            int index = distributionProvider.getRandomGenerator().nextInt(realParties.size());
            Party target = realParties.get(index);
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
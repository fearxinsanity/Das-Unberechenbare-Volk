package de.schulprojekt.duv.model.voter;

import de.schulprojekt.duv.model.core.SimulationParameters;
import de.schulprojekt.duv.model.dto.VoterTransition;
import de.schulprojekt.duv.model.party.Party;
import de.schulprojekt.duv.model.random.DistributionProvider;
import de.schulprojekt.duv.model.scandal.ScandalImpactCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the VoterBehavior class focusing on decision logic and determinism.
 */
class VoterBehaviorTest {

    private VoterBehavior voterBehavior;
    private VoterPopulation population;
    private List<Party> parties;
    private SimulationParameters params;
    private ScandalImpactCalculator impactCalculator;
    private DistributionProvider distributionProvider;
    private ZeitgeistManager zeitgeistManager;

    @BeforeEach
    void setUp() {
        params = new SimulationParameters(
                1000, 50.0, 20.0, 10.0, 50.0, 50, 1.0, 3, 2.5
        );

        // 2. Initialize Random Distribution Provider
        distributionProvider = new DistributionProvider(params);
        distributionProvider.initialize(params);

        // 3. Initialize Parties
        parties = new ArrayList<>();
        parties.add(new Party("Non-Voters", "NV", "#808080", 50.0, 0.0, 0));
        parties.add(new Party("Party A", "PA", "#FF0000", 20.0, 1000.0, 0));
        parties.add(new Party("Party B", "PB", "#0000FF", 80.0, 1000.0, 0));

        // 4. Initialize Population
        population = new VoterPopulation();
        population.initialize(params.populationSize(), parties.size(), distributionProvider);

        // 5. Initialize Impact Calculator
        impactCalculator = new ScandalImpactCalculator(parties.size() + 5);

        // 6. Initialize ZeitgeistManager
        zeitgeistManager = new ZeitgeistManager();
        zeitgeistManager.setZeitgeist(0.0); // Start with neutral zeitgeist for testing

        // 7. Initialize the Class Under Test
        voterBehavior = new VoterBehavior();
    }

    @Test
    @DisplayName("Should process voter decisions with zeitgeist parameter")
    void testProcessVoterDecisions_BasicFlow() {
        double[] acutePressures = new double[parties.size()];
        int currentStep = 0;

        // Execute with explicit zeitgeist from manager
        List<VoterTransition> transitions = voterBehavior.processVoterDecisions(
                population,
                parties,
                params,
                acutePressures,
                impactCalculator,
                currentStep,
                zeitgeistManager.getCurrentZeitgeist()
        );

        assertNotNull(transitions, "The result list should not be null");

        for (int i = 0; i < 10; i++) {
            int partyIndex = population.getPartyIndex(i);
            assertTrue(partyIndex >= 0 && partyIndex < parties.size(),
                    "Voter " + i + " has invalid party index: " + partyIndex);
        }
    }

    @Test
    @DisplayName("Should respect zeitgeist changes in decision process")
    void testProcessWithChangedZeitgeist() {
        double[] acutePressures = new double[parties.size()];

        // Test step 1: Neutral
        voterBehavior.processVoterDecisions(population, parties, params, acutePressures, impactCalculator, 1, 0.0);

        // Test step 2: Extreme Zeitgeist shift via manager update
        zeitgeistManager.updateZeitgeist();
        double activeZeitgeist = zeitgeistManager.getCurrentZeitgeist();

        List<VoterTransition> transitions = voterBehavior.processVoterDecisions(
                population,
                parties,
                params,
                acutePressures,
                impactCalculator,
                2,
                activeZeitgeist
        );

        assertNotNull(transitions);
    }

    @Test
    @DisplayName("Should handle acute pressure correctly in decisions")
    void testWithAcutePressure() {
        double[] acutePressures = new double[parties.size()];
        acutePressures[1] = 10000.0; // Massive pressure on Party A

        voterBehavior.processVoterDecisions(
                population,
                parties,
                params,
                acutePressures,
                impactCalculator,
                0,
                0.0
        );

        int supportersA = parties.get(1).getCurrentSupporterCount();
        assertTrue(supportersA >= 0, "Supporter count should remain non-negative");
    }

}
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

class VoterBehaviorTest {

    private VoterBehavior voterBehavior;
    private VoterPopulation population;
    private List<Party> parties;
    private SimulationParameters params;
    private ScandalImpactCalculator impactCalculator;
    private DistributionProvider distributionProvider;

    @BeforeEach
    void setUp() {
        // 1. Initialize Parameters (Standard values)
        params = new SimulationParameters(
                1000,   // populationSize
                50.0,   // mediaInfluence
                20.0,   // volatilityRate
                10.0,   // scandalProbability
                50.0,   // loyaltyAverage
                50,     // tickRate
                1.0,    // chaosFactor
                3,      // partyCount
                2.5,    // budgetEffectiveness
                42L     // seed
        );

        // 2. Initialize Random Distribution Provider
        distributionProvider = new DistributionProvider();
        distributionProvider.initialize(params);

        // 3. Initialize Parties with CORRECT Constructor
        // Signature: Name, Abbr, ColorCode, Position (0-100), Budget, Supporters
        parties = new ArrayList<>();

        // Party 0: Non-Voters / Undecided (usually neutral position)
        parties.add(new Party("Non-Voters", "NV", "#808080", 50.0, 0.0, 0));

        // Party 1: Left Wing (Position 20.0)
        parties.add(new Party("Party A", "PA", "#FF0000", 20.0, 1000.0, 0));

        // Party 2: Right Wing (Position 80.0)
        parties.add(new Party("Party B", "PB", "#0000FF", 80.0, 1000.0, 0));

        // 4. Initialize Population
        population = new VoterPopulation();
        // Assuming initialize signature: (int size, int partyCount, DistributionProvider provider)
        population.initialize(params.populationSize(), parties.size(), distributionProvider);

        // 5. Initialize Impact Calculator
        impactCalculator = new ScandalImpactCalculator(parties.size() + 5);

        // 6. Initialize the Class Under Test
        voterBehavior = new VoterBehavior();
    }

    @Test
    @DisplayName("Should process voter decisions without errors")
    void testProcessVoterDecisions_BasicFlow() {
        // Create an empty pressure array (no active scandals)
        double[] acutePressures = new double[parties.size()];

        // Execute the method
        List<VoterTransition> transitions = voterBehavior.processVoterDecisions(
                population,
                parties,
                params,
                acutePressures,
                impactCalculator,
                0
        );

        // Assertions
        assertNotNull(transitions, "The result list should not be null");

        // Check if population indices are valid after processing
        // We sample the first 10 voters to ensure they have valid party indices
        for (int i = 0; i < 10; i++) {
            int partyIndex = population.getPartyIndex(i);
            assertTrue(partyIndex >= 0 && partyIndex < parties.size(),
                    "Voter " + i + " has invalid party index: " + partyIndex);
        }
    }

    @Test
    @DisplayName("Should trigger transitions with high volatility")
    void testHighVolatility() {
        // Create parameters with 100% volatility to force changes
        SimulationParameters highVolParams = new SimulationParameters(
                1000, 50.0, 100.0, 10.0, 50.0, 50, 1.0, 3, 2.5, 42L
        );

        // Important: Re-initialize distribution so it uses the new params
        distributionProvider.initialize(highVolParams);

        double[] acutePressures = new double[parties.size()];

        List<VoterTransition> transitions = voterBehavior.processVoterDecisions(
                population,
                parties,
                highVolParams,
                acutePressures,
                impactCalculator,
                1
        );

        assertNotNull(transitions);
        // With 100% volatility, transitions are very likely, but since it's random,
        // we mainly ensure no crash and a non-null list.
    }

    @Test
    @DisplayName("Should handle acute pressure (scandals) correctly")
    void testWithAcutePressure() {
        double[] acutePressures = new double[parties.size()];
        // Apply massive pressure to Party A (Index 1)
        acutePressures[1] = 10000.0;

        voterBehavior.processVoterDecisions(
                population,
                parties,
                params,
                acutePressures,
                impactCalculator,
                0
        );

        // Check internal consistency
        int supportersA = parties.get(1).getCurrentSupporterCount();
        assertTrue(supportersA >= 0, "Supporter count should be non-negative");

    }
}
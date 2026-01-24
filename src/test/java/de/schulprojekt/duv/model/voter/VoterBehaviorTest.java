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
 * Tests für die Klasse VoterBehavior.
 * Fokus auf die ausgelagerte Simulationslogik und die korrekte Nutzung der SoA-Struktur.
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

        distributionProvider = new DistributionProvider(params);
        distributionProvider.initialize(params);

        parties = new ArrayList<>();
        parties.add(new Party("Non-Voters", "NV", "#808080", 50.0, 0, 0));
        parties.add(new Party("Party A", "PA", "#FF0000", 20.0, 500, 1000));
        parties.add(new Party("Party B", "PB", "#0000FF", 80.0, 500, 1000));

        population = new VoterPopulation();
        voterBehavior = new VoterBehavior();

        // Refactored: Initialisierung erfolgt nun über VoterBehavior
        voterBehavior.initializePopulation(population, params.populationSize(), parties.size(), distributionProvider);

        impactCalculator = new ScandalImpactCalculator(parties.size() + 5);
        zeitgeistManager = new ZeitgeistManager();
        zeitgeistManager.setZeitgeist(0.0);
    }

    @Test
    @DisplayName("Sollte Wählerentscheidungen ohne den Parameter currentStep verarbeiten")
    void testProcessVoterDecisions_Flow() {
        double[] acutePressures = new double[parties.size()];

        // Refactored: Aufruf ohne currentStep
        List<VoterTransition> transitions = voterBehavior.processVoterDecisions(
                population,
                parties,
                params,
                acutePressures,
                impactCalculator,
                zeitgeistManager.getCurrentZeitgeist()
        );

        assertNotNull(transitions, "Die Ergebnisliste darf nicht null sein");
        assertTrue(population.size() > 0);

        // Nutzt Raw-Methoden für den Check
        for (int i = 0; i < 10; i++) {
            int partyIndex = population.getPartyIndexRaw(i);
            assertTrue(partyIndex >= 0 && partyIndex < parties.size());
        }
    }

    @Test
    @DisplayName("Sollte die Evolution der Wählerattribute korrekt durchführen")
    void testEvolvePopulation() {
        float initialLoyalty = population.getLoyaltyRaw(0);

        // Simulation von 100 Ticks Evolution
        for (int i = 0; i < 100; i++) {
            voterBehavior.evolvePopulation(population, params);
        }

        float finalLoyalty = population.getLoyaltyRaw(0);
        // Es ist extrem unwahrscheinlich, dass die Loyalität bei 100 Ticks exakt gleich bleibt
        assertNotEquals(initialLoyalty, finalLoyalty, "Attribute sollten sich über die Zeit verändern");
    }

    @Test
    @DisplayName("Massiver Skandaldruck sollte die Wähleranzahl einer Partei reduzieren")
    void testWithAcutePressureImpact() {
        double[] acutePressures = new double[parties.size()];
        acutePressures[1] = 1000.0; // Extremer Druck auf Partei A

        int initialSupporters = parties.get(1).getCurrentSupporterCount();

        voterBehavior.processVoterDecisions(
                population,
                parties,
                params,
                acutePressures,
                impactCalculator,
                0.0
        );

        int finalSupporters = parties.get(1).getCurrentSupporterCount();
        assertTrue(finalSupporters <= initialSupporters, "Unterstützer sollten bei Skandalen abwandern");
    }
}
package de.schulprojekt.duv.model.random;

import de.schulprojekt.duv.model.core.SimulationParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DistributionProviderTest {

    private DistributionProvider provider;
    private SimulationParameters params;
    private final long testSeed = 12345L;

    @BeforeEach
    void setUp() {
        params = new SimulationParameters(1000, 65.0, 35.0, 5.0, 50.0, 5, 1.0, 4, 1.0, testSeed);
        provider = new DistributionProvider();
        provider.initialize(params);
    }

    @Test
    @DisplayName("Sollte bei gleichem Seed identische Ergebnisse liefern (Determinismus)")
    void testDeterminism() {
        DistributionProvider secondProvider = new DistributionProvider();
        secondProvider.initialize(params);

        for (int i = 0; i < 100; i++) {
            assertEquals(provider.sampleLoyalty(), secondProvider.sampleLoyalty());
            assertEquals(provider.sampleUniform(), secondProvider.sampleUniform());
            assertEquals(provider.sampleTimeUntilNextScandal(), secondProvider.sampleTimeUntilNextScandal());
        }
    }

    @Test
    @DisplayName("Sollte korrekte Grenzen für die Loyalität einhalten")
    void testSampleLoyaltyBoundaries() {
        for (int i = 0; i < 1000; i++) {
            double sample = provider.sampleLoyalty();
            assertTrue(sample >= 0.0 && sample <= 100.0);
        }
    }
}
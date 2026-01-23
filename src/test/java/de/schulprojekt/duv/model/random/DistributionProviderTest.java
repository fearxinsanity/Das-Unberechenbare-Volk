package de.schulprojekt.duv.model.random;

import de.schulprojekt.duv.model.core.SimulationParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DistributionProviderTest {
    private DistributionProvider provider;
    private SimulationParameters params;

    @BeforeEach
    void setUp() {
        params = new SimulationParameters(1000, 65.0, 35.0, 5.0, 50.0, 5, 1.0, 4, 1.0);
        provider = new DistributionProvider(params);
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
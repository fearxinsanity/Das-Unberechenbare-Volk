package de.schulprojekt.duv.model.random;

import de.schulprojekt.duv.model.core.SimulationParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DistributionProviderTest {

    private DistributionProvider provider;
    private SimulationParameters params;

    @BeforeEach
    void setUp() {
        provider = new DistributionProvider();

        // Initialize with real parameters
        // We set chaosFactor to 5.0 explicitly to test the uniform range later
        params = new SimulationParameters(
                1000,   // populationSize
                50.0,   // mediaInfluence
                20.0,   // volatilityRate
                10.0,   // scandalProbability
                50.0,   // loyaltyAverage
                50,     // tickRate
                5.0,    // chaosFactor (Important for testUniformRange)
                5,      // partyCount
                2.5     // budgetEffectiveness
        );

        provider.initialize(params);
    }

    @Test
    @DisplayName("Should initialize distributions without errors")
    void testInitialization() {
        // Just ensuring no NPE is thrown when sampling immediately after init
        assertDoesNotThrow(() -> provider.sampleLoyalty());
        assertDoesNotThrow(() -> provider.sampleUniform());
        assertDoesNotThrow(() -> provider.sampleTimeUntilNextScandal());
    }

    @RepeatedTest(50)
    @DisplayName("Loyalty samples should always be within [0, 100]")
    void testLoyaltyRange() {
        double value = provider.sampleLoyalty();
        assertTrue(value >= 0.0 && value <= 100.0,
                "Loyalty value " + value + " is out of bounds [0, 100]");
    }

    @Test
    @DisplayName("Uniform samples should be within [0, chaosFactor]")
    void testUniformRange() {
        double maxChaos = params.chaosFactor(); // 5.0

        for (int i = 0; i < 100; i++) {
            double value = provider.sampleUniform();
            assertTrue(value >= 0.0 && value <= maxChaos,
                    "Uniform value " + value + " is out of bounds [0, " + maxChaos + "]");
        }
    }

    @Test
    @DisplayName("Time until next scandal should be a positive number")
    void testScandalSampling() {
        // Exponential distribution always yields non-negative values
        for (int i = 0; i < 20; i++) {
            double time = provider.sampleTimeUntilNextScandal();
            assertTrue(time >= 0.0, "Time until scandal must be non-negative");
        }
    }

    @Test
    @DisplayName("Should handle extremely low scandal probability correctly")
    void testLowScandalProbability() {
        // Setup params with 0.0 scandal probability
        SimulationParameters safeParams = new SimulationParameters(
                1000, 50.0, 20.0, 0.0, 50.0, 50, 1.0, 5, 2.5
        );

        provider.initialize(safeParams);

        // Even with 0 probability, the provider clamps lambda to MIN_SCANDAL_LAMBDA
        // to avoid division by zero. It should return a very large number (long time until scandal).
        double time = provider.sampleTimeUntilNextScandal();
        assertTrue(time > 0, "Time should still be positive/valid even with 0 probability");
    }
}
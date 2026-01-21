package de.schulprojekt.duv.util;

import de.schulprojekt.duv.model.core.SimulationParameters;
import de.schulprojekt.duv.util.validation.ParameterValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests für den ParameterValidator.
 * Stellt sicher, dass alle Simulationsparameter innerhalb ihrer definierten Grenzen liegen.
 */
class ParameterValidatorTest {

    /**
     * Hilfsmethode, um schnell valide Parameter zu erzeugen.
     * Nutzt den neuen 10-Argument-Konstruktor inklusive Seed.
     */
    private SimulationParameters createValidParams() {
        return new SimulationParameters(
                1000,  // populationSize
                50.0,  // mediaInfluence
                20.0,  // volatilityRate
                10.0,  // scandalProbability
                50.0,  // loyaltyAverage
                50,    // tickRate
                1.0,   // chaosFactor
                5,     // partyCount
                2.5,   // budgetEffectiveness
                42L    // seed
        );
    }

    @Test
    @DisplayName("Sollte true zurückgeben, wenn alle Parameter valide sind")
    void testValidParameters() {
        SimulationParameters params = createValidParams();
        assertTrue(ParameterValidator.isValid(params), "Parameter sollten standardmäßig gültig sein");
        assertDoesNotThrow(() -> ParameterValidator.validate(params));
    }

    @Test
    @DisplayName("Sollte Fehler werfen, wenn Population außerhalb der Grenzen liegt")
    void testPopulationBoundary() {
        // Test: Zu klein (Min ist 1000)
        SimulationParameters invalidPop = new SimulationParameters(
                999, 50.0, 20.0, 10.0, 50.0, 50, 1.0, 5, 2.5, 42L
        );
        assertTrue(ParameterValidator.isInvalid(invalidPop), "Sollte invalid sein bei Population < 1000");

        // Test: Zu groß (Max ist 2.000.000)
        SimulationParameters tooBigPop = new SimulationParameters(
                2_000_001, 50.0, 20.0, 10.0, 50.0, 50, 1.0, 5, 2.5, 42L
        );
        // KORREKTUR: Wir erwarten nun TRUE für isInvalid
        assertTrue(ParameterValidator.isInvalid(tooBigPop), "Sollte invalid sein bei Population > 2.000.000");
    }

    @Test
    @DisplayName("Sollte Prozentwerte korrekt validieren")
    void testPercentageBoundary() {
        SimulationParameters lowMedia = new SimulationParameters(
                1000, -0.1, 20.0, 10.0, 50.0, 50, 1.0, 5, 2.5, 42L
        );
        String errorMsg = ParameterValidator.getValidationError(lowMedia);
        assertTrue(errorMsg.contains("Media influence"), "Fehlermeldung sollte 'Media influence' enthalten.");

        SimulationParameters highMedia = new SimulationParameters(
                1000, 100.1, 20.0, 10.0, 50.0, 50, 1.0, 5, 2.5, 42L
        );
        assertTrue(ParameterValidator.isInvalid(highMedia));
    }

    @Test
    @DisplayName("Sollte Utility-Methoden für Clamping korrekt ausführen")
    void testClampingMethods() {
        assertEquals(0.0, ParameterValidator.clampPercentage(-50.0));
        assertEquals(100.0, ParameterValidator.clampPercentage(150.0));
        assertEquals(10, ParameterValidator.clampInt(5, 10, 20));
    }
}
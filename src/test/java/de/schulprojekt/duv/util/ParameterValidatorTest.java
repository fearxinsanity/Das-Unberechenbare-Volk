package de.schulprojekt.duv.util;

import de.schulprojekt.duv.model.core.SimulationParameters;
import de.schulprojekt.duv.util.validation.ParameterValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParameterValidatorTest {

    /**
     * Hilfsmethode, um schnell valide Parameter zu erzeugen.
     * Nutzt den normalen Konstruktor statt Mocks.
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
                2.5    // budgetEffectiveness
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
        // Test: Zu klein (Min ist 100). Wir testen 99.
        // Hinweis: Der Record-Konstruktor erlaubt >= 0, also können wir 99 erstellen,
        // aber der Validator sollte es ablehnen.
        SimulationParameters invalidPop = new SimulationParameters(
                99, 50.0, 20.0, 10.0, 50.0, 50, 1.0, 5, 2.5
        );

        assertFalse(ParameterValidator.isValid(invalidPop), "Sollte invalid sein bei Population < 100");
        assertThrows(IllegalArgumentException.class, () -> ParameterValidator.validate(invalidPop));

        // Test: Zu groß (Max ist 2.000.000). Wir testen 2.000.001
        SimulationParameters tooBigPop = new SimulationParameters(
                2_000_001, 50.0, 20.0, 10.0, 50.0, 50, 1.0, 5, 2.5
        );
        assertFalse(ParameterValidator.isValid(tooBigPop));
    }

    @Test
    @DisplayName("Sollte Prozentwerte korrekt validieren (MediaInfluence)")
    void testPercentageBoundary() {
        // Zu niedrig (-0.1)
        SimulationParameters lowMedia = new SimulationParameters(
                1000, -0.1, 20.0, 10.0, 50.0, 50, 1.0, 5, 2.5
        );
        String errorMsg = ParameterValidator.getValidationError(lowMedia);
        assertTrue(errorMsg.contains("Medieneinfluss"), "Sollte Fehler für Medieneinfluss zurückgeben");

        // Zu hoch (100.1)
        SimulationParameters highMedia = new SimulationParameters(
                1000, 100.1, 20.0, 10.0, 50.0, 50, 1.0, 5, 2.5
        );
        assertTrue(ParameterValidator.isInvalid(highMedia));
    }

    @Test
    @DisplayName("Sollte Utility-Methoden für Clamping korrekt ausführen")
    void testClampingMethods() {
        // Test clampPercentage
        assertEquals(0.0, ParameterValidator.clampPercentage(-50.0));
        assertEquals(100.0, ParameterValidator.clampPercentage(150.0));
        assertEquals(55.5, ParameterValidator.clampPercentage(55.5));

        // Test clampInt
        assertEquals(10, ParameterValidator.clampInt(5, 10, 20)); // unteres Limit
        assertEquals(20, ParameterValidator.clampInt(25, 10, 20)); // oberes Limit
        assertEquals(15, ParameterValidator.clampInt(15, 10, 20)); // im Bereich
    }
}
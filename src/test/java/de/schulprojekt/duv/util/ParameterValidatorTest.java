package de.schulprojekt.duv.util;

import de.schulprojekt.duv.model.core.SimulationParameters;
import de.schulprojekt.duv.util.validation.ParameterValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParameterValidatorTest {

    private SimulationParameters createValidParams() {
        return new SimulationParameters(1000, 50.0, 20.0, 10.0, 50.0, 50, 1.0, 5, 2.5);
    }

    @Test
    @DisplayName("Sollte valide Parameter akzeptieren")
    void testValidParameters() {
        SimulationParameters params = createValidParams();
        assertTrue(ParameterValidator.isValid(params));
        assertDoesNotThrow(() -> ParameterValidator.validate(params));
    }

    @Test
    @DisplayName("Sollte ungültige Populationsgrößen ablehnen")
    void testPopulationBoundary() {
        // Zu klein (Min 1000)
        SimulationParameters tooSmall = new SimulationParameters(999, 50.0, 20.0, 10.0, 50.0, 50, 1.0, 5, 2.5);
        assertTrue(ParameterValidator.isInvalid(tooSmall));

        // Zu groß (Max 2.000.000)
        SimulationParameters tooBig = new SimulationParameters(2000001, 50.0, 20.0, 10.0, 50.0, 50, 1.0, 5, 2.5);
        assertTrue(ParameterValidator.isInvalid(tooBig));
    }

    @Test
    @DisplayName("Sollte Prozentwerte (0-100) korrekt validieren")
    void testPercentageValues() {
        SimulationParameters invalidMedia = new SimulationParameters(1000, 100.1, 20.0, 10.0, 50.0, 50, 1.0, 5, 2.5);
        assertTrue(ParameterValidator.isInvalid(invalidMedia));

        SimulationParameters negativeMedia = new SimulationParameters(1000, -0.1, 20.0, 10.0, 50.0, 50, 1.0, 5, 2.5);
        assertTrue(ParameterValidator.isInvalid(negativeMedia));
    }
}
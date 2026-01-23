package de.schulprojekt.duv.util.validation;

/**
 * Enum aller Validierungs- und Protokollierungsnachrichten für Simulationsparameter.
 *
 * @author Nico Hoffmann
 * @version 1.0
 */
public enum ValidationMessage {

    // ========================================
    // Validation Error Messages
    // ========================================

    POPULATION_OUT_OF_RANGE(
            "Population size must be between %,d and %,d"
    ),

    MEDIA_INFLUENCE_OUT_OF_RANGE(
            "Media influence must be between %.1f%% and %.1f%%"
    ),

    VOLATILITY_OUT_OF_RANGE(
            "Volatility rate must be between %.1f%% and %.1f%%"
    ),

    SCANDAL_PROBABILITY_OUT_OF_RANGE(
            "Scandal probability must be between %.1f%% and %.1f%%"
    ),

    LOYALTY_OUT_OF_RANGE(
            "Loyalty average must be between %.1f%% and %.1f%%"
    ),

    TICK_RATE_OUT_OF_RANGE(
            "Tick rate must be between %d and %d"
    ),

    CHAOS_FACTOR_OUT_OF_RANGE(
            "Chaos factor must be between %.1f and %.1f"
    ),

    PARTY_COUNT_OUT_OF_RANGE(
            "Party count must be between %d and %d"
    ),

    BUDGET_EFFECTIVENESS_OUT_OF_RANGE(
            "Budget effectiveness must be between %.1f and %.1f"
    ),

    // ========================================
    // Controller Log Messages
    // ========================================

    INVALID_PARAMETERS_REJECTED(
            "Invalid parameters rejected: %s"
    ),

    INVALID_PARAMETER_INPUT(
            "Invalid parameter input, using defaults"
    ),

    BUILT_PARAMETERS_INVALID(
            "Built parameters are invalid: %s"
    ),

    SPEED_FACTOR_OUT_OF_RANGE(
            "Speed factor must be between 1 and 100, got: %d"
    ),

    // ========================================
    // Utility Class Error Messages
    // ========================================

    UTILITY_CLASS_INSTANTIATION(
            "Utility class cannot be instantiated"
    );

    // ========================================
    // Instance Variables
    // ========================================

    private final String messageTemplate;

    // ========================================
    // Constructors
    // ========================================

    /**
     * Erstellt eine Validierungsnachricht mit der angegebenen Vorlage.
     *
     * @param messageTemplate die Nachrichtenvorlage mit Format-Platzhaltern
     */
    ValidationMessage(String messageTemplate) {
        this.messageTemplate = messageTemplate;
    }

    // ========================================
    // Business Logic Methods
    // ========================================

    /**
     * Formatiert die Nachrichtenvorlage mit den übergebenen Argumenten.
     *
     * @param args die Argumente, die in die Nachricht formatiert werden sollen
     * @return die formatierte Nachricht
     */
    public String format(Object... args) {
        return String.format(messageTemplate, args);
    }

    /**
     * Ruft die Nachricht ohne Formatierung ab.
     *
     * @return der Nachrichtentext als String
     */
    @Override
    public String toString() {
        return messageTemplate;
    }
}

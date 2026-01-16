package de.schulprojekt.duv.util.validation;

/**
 * Enumeration of validation error messages for simulation parameters.
 * Provides centralized, type-safe error message management.
 *
 * @author Nico Hoffmann
 * @version 1.0
 */
public enum ValidationMessage {

    // ========================================
    // Enum Constants
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
    );

    // ========================================
    // Instance Variables
    // ========================================

    private final String messageTemplate;

    // ========================================
    // Constructors
    // ========================================

    /**
     * Constructs a validation message with the given template.
     *
     * @param messageTemplate the message template with format placeholders
     */
    ValidationMessage(String messageTemplate) {
        this.messageTemplate = messageTemplate;
    }

    // ========================================
    // Business Logic Methods
    // ========================================

    /**
     * Formats the message template with the provided arguments.
     *
     * @param args the arguments to format into the message
     * @return the formatted error message
     */
    public String format(Object... args) {
        return String.format(messageTemplate, args);
    }

    /**
     * Gets the raw message template.
     *
     * @return the unformatted message template
     */
    public String getTemplate() {
        return messageTemplate;
    }
}

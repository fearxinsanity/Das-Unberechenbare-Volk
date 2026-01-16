package de.schulprojekt.duv.util.validation;

import de.schulprojekt.duv.model.core.SimulationParameters;

/**
 * Validates simulation parameters to ensure they are within acceptable ranges.
 * @author Nico Hoffmann
 * @version 1.0
 */
public class ParameterValidator {

    // ========================================
    // Static Variables
    // ========================================

    private static final int MIN_POPULATION = 100;
    private static final int MAX_POPULATION = 2_000_000;

    private static final double MIN_PERCENTAGE = 0.0;
    private static final double MAX_PERCENTAGE = 100.0;

    private static final double MIN_SCANDAL_PROB = 0.0;
    private static final double MAX_SCANDAL_PROB = 60.0;

    private static final int MIN_TICK_RATE = 1;
    private static final int MAX_TICK_RATE = 100;

    private static final double MIN_CHAOS = 0.0;
    private static final double MAX_CHAOS = 10.0;

    private static final int MIN_PARTIES = 2;
    private static final int MAX_PARTIES = 20;

    private static final double MIN_BUDGET_EFFECTIVENESS = 0.0;
    private static final double MAX_BUDGET_EFFECTIVENESS = 5.0;

    // ========================================
    // Constructors
    // ========================================

    private ParameterValidator() {
        throw new UnsupportedOperationException(ValidationMessage.UTILITY_CLASS_INSTANTIATION.toString());
    }

    // ========================================
    // Getter Methods for Constraints
    // ========================================

    public static int getMinPopulation() {
        return MIN_POPULATION;
    }

    public static int getMaxPopulation() {
        return MAX_POPULATION;
    }

    public static double getMinPercentage() {
        return MIN_PERCENTAGE;
    }

    public static double getMaxPercentage() {
        return MAX_PERCENTAGE;
    }

    public static double getMinScandalProb() {
        return MIN_SCANDAL_PROB;
    }

    public static double getMaxScandalProb() {
        return MAX_SCANDAL_PROB;
    }

    public static int getMinTickRate() {
        return MIN_TICK_RATE;
    }

    public static int getMaxTickRate() {
        return MAX_TICK_RATE;
    }

    public static double getMinChaos() {
        return MIN_CHAOS;
    }

    public static double getMaxChaos() {
        return MAX_CHAOS;
    }

    public static int getMinParties() {
        return MIN_PARTIES;
    }

    public static int getMaxParties() {
        return MAX_PARTIES;
    }

    public static double getMinBudgetEffectiveness() {
        return MIN_BUDGET_EFFECTIVENESS;
    }

    public static double getMaxBudgetEffectiveness() {
        return MAX_BUDGET_EFFECTIVENESS;
    }

    // ========================================
    // Validation Methods
    // ========================================

    /**
     * Validates all simulation parameters.
     * @param params the parameters to validate
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public static void validate(SimulationParameters params) {
        String error = getValidationError(params);
        if (!error.isEmpty()) {
            throw new IllegalArgumentException(error);
        }
    }

    /**
     * Checks if parameters are valid.
     * @param params the parameters to check
     * @return true if valid, false otherwise
     */
    public static boolean isValid(SimulationParameters params) {
        return getValidationError(params).isEmpty();
    }

    /**
     * Checks if parameters are invalid.
     * Convenience method to avoid negation in calling code.
     * @param params the parameters to check
     * @return true if invalid, false otherwise
     */
    public static boolean isInvalid(SimulationParameters params) {
        return !isValid(params);
    }

    /**
     * Gets validation error message using centralized ValidationMessage enum.
     * @param params the parameters to validate
     * @return error message, or empty string if valid
     */
    public static String getValidationError(SimulationParameters params) {
        if (params.populationSize() < MIN_POPULATION || params.populationSize() > MAX_POPULATION) {
            return ValidationMessage.POPULATION_OUT_OF_RANGE.format(MIN_POPULATION, MAX_POPULATION);
        }

        if (params.mediaInfluence() < MIN_PERCENTAGE || params.mediaInfluence() > MAX_PERCENTAGE) {
            return ValidationMessage.MEDIA_INFLUENCE_OUT_OF_RANGE.format(MIN_PERCENTAGE, MAX_PERCENTAGE);
        }

        if (params.volatilityRate() < MIN_PERCENTAGE || params.volatilityRate() > MAX_PERCENTAGE) {
            return ValidationMessage.VOLATILITY_OUT_OF_RANGE.format(MIN_PERCENTAGE, MAX_PERCENTAGE);
        }

        if (params.scandalProbability() < MIN_SCANDAL_PROB || params.scandalProbability() > MAX_SCANDAL_PROB) {
            return ValidationMessage.SCANDAL_PROBABILITY_OUT_OF_RANGE.format(MIN_SCANDAL_PROB, MAX_SCANDAL_PROB);
        }

        if (params.loyaltyAverage() < MIN_PERCENTAGE || params.loyaltyAverage() > MAX_PERCENTAGE) {
            return ValidationMessage.LOYALTY_OUT_OF_RANGE.format(MIN_PERCENTAGE, MAX_PERCENTAGE);
        }

        if (params.tickRate() < MIN_TICK_RATE || params.tickRate() > MAX_TICK_RATE) {
            return ValidationMessage.TICK_RATE_OUT_OF_RANGE.format(MIN_TICK_RATE, MAX_TICK_RATE);
        }

        if (params.chaosFactor() < MIN_CHAOS || params.chaosFactor() > MAX_CHAOS) {
            return ValidationMessage.CHAOS_FACTOR_OUT_OF_RANGE.format(MIN_CHAOS, MAX_CHAOS);
        }

        if (params.partyCount() < MIN_PARTIES || params.partyCount() > MAX_PARTIES) {
            return ValidationMessage.PARTY_COUNT_OUT_OF_RANGE.format(MIN_PARTIES, MAX_PARTIES);
        }

        if (params.budgetEffectiveness() < MIN_BUDGET_EFFECTIVENESS || params.budgetEffectiveness() > MAX_BUDGET_EFFECTIVENESS) {
            return ValidationMessage.BUDGET_EFFECTIVENESS_OUT_OF_RANGE.format(MIN_BUDGET_EFFECTIVENESS, MAX_BUDGET_EFFECTIVENESS);
        }

        return "";
    }

    // ========================================
    // Utility Methods
    // ========================================

    /**
     * Clamps a value to a percentage range (0-100).
     * @param value the value to clamp
     * @return clamped value
     */
    public static double clampPercentage(double value) {
        return Math.max(MIN_PERCENTAGE, Math.min(MAX_PERCENTAGE, value));
    }

    /**
     * Clamps an integer value to a specified range.
     * @param value the value to clamp
     * @param min minimum value
     * @param max maximum value
     * @return clamped value
     */
    public static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Clamps a double value to a specified range.
     * @param value the value to clamp
     * @param min minimum value
     * @param max maximum value
     * @return clamped value
     */
    public static double clampDouble(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}

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
    private static final int MAX_PARTIES = 8;

    private static final double MIN_BUDGET_EFFECTIVENESS = 0.0;
    private static final double MAX_BUDGET_EFFECTIVENESS = 5.0;

    // ========================================
    // Constructors
    // ========================================

    private ParameterValidator() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
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
     * Gets validation error message.
     * @param params the parameters to validate
     * @return error message, or empty string if valid
     */
    public static String getValidationError(SimulationParameters params) {
        if (params.populationSize() < MIN_POPULATION || params.populationSize() > MAX_POPULATION) {
            return String.format("Population size must be between %d and %,d", MIN_POPULATION, MAX_POPULATION);
        }

        if (params.mediaInfluence() < MIN_PERCENTAGE || params.mediaInfluence() > MAX_PERCENTAGE) {
            return String.format("Media influence must be between %.1f%% and %.1f%%", MIN_PERCENTAGE, MAX_PERCENTAGE);
        }

        if (params.volatilityRate() < MIN_PERCENTAGE || params.volatilityRate() > MAX_PERCENTAGE) {
            return String.format("Volatility rate must be between %.1f%% and %.1f%%", MIN_PERCENTAGE, MAX_PERCENTAGE);
        }

        if (params.scandalProbability() < MIN_SCANDAL_PROB || params.scandalProbability() > MAX_SCANDAL_PROB) {
            return String.format("Scandal probability must be between %.1f%% and %.1f%%", MIN_SCANDAL_PROB, MAX_SCANDAL_PROB);
        }

        if (params.loyaltyAverage() < MIN_PERCENTAGE || params.loyaltyAverage() > MAX_PERCENTAGE) {
            return String.format("Loyalty average must be between %.1f%% and %.1f%%", MIN_PERCENTAGE, MAX_PERCENTAGE);
        }

        if (params.tickRate() < MIN_TICK_RATE || params.tickRate() > MAX_TICK_RATE) {
            return String.format("Tick rate must be between %d and %d", MIN_TICK_RATE, MAX_TICK_RATE);
        }

        if (params.chaosFactor() < MIN_CHAOS || params.chaosFactor() > MAX_CHAOS) {
            return String.format("Chaos factor must be between %.1f and %.1f", MIN_CHAOS, MAX_CHAOS);
        }

        if (params.partyCount() < MIN_PARTIES || params.partyCount() > MAX_PARTIES) {
            return String.format("Party count must be between %d and %d", MIN_PARTIES, MAX_PARTIES);
        }

        if (params.budgetEffectiveness() < MIN_BUDGET_EFFECTIVENESS || params.budgetEffectiveness() > MAX_BUDGET_EFFECTIVENESS) {
            return String.format("Budget effectiveness must be between %.1f and %.1f", MIN_BUDGET_EFFECTIVENESS, MAX_BUDGET_EFFECTIVENESS);
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

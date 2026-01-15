package de.schulprojekt.duv.model.core;

/**
 * Immutable Data Transfer Object for simulation settings.
 * * @param populationSize total number of simulated voters
 * @param mediaInfluence influence of media on voters (0.0 - 100.0)
 * @param volatilityRate willingness of voters to change parties (0.0 - 100.0)
 * @param scandalProbability probability of a scandal per tick (0.0 - 60.0)
 * @param loyaltyAverage average initial party loyalty (0.0 - 100.0)
 * @param tickRate simulation speed in Ticks Per Second
 * @param chaosFactor random deviation factor for variance
 * @param partyCount number of active political parties
 * @param budgetEffectiveness multiplier for campaign budget impact
 * * @author Nico Hoffmann
 * @version 1.1
 * @since Java 16
 */
public record SimulationParameters(
        int populationSize,
        double mediaInfluence,
        double volatilityRate,
        double scandalProbability,
        double loyaltyAverage,
        int tickRate,
        double chaosFactor,
        int partyCount,
        double budgetEffectiveness
) {

    // ========================================
    // Compact Constructor (Validation)
    // ========================================

    /**
     * Validates that numeric constraints are met.
     */
    public SimulationParameters {
        if (populationSize < 0) throw new IllegalArgumentException("Population cannot be negative");
        if (tickRate < 1) throw new IllegalArgumentException("Tick rate must be at least 1");
    }

    // ========================================
    // Transformation Methods
    // ========================================

    /**
     * Creates a copy of the parameters with an updated tick rate.
     * * @param newTickRate the new simulation speed
     * @return a new SimulationParameters instance
     */
    public SimulationParameters withTickRate(int newTickRate) {
        return new SimulationParameters(
                populationSize,
                mediaInfluence,
                volatilityRate,
                scandalProbability,
                loyaltyAverage,
                newTickRate,
                chaosFactor,
                partyCount,
                budgetEffectiveness
        );
    }
}
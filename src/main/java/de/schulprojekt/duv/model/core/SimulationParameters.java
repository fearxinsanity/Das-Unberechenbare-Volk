package de.schulprojekt.duv.model.core;

/**
 * Immutable Data Transfer Object for simulation settings.
 * @param populationSize total number of simulated voters
 * @param mediaInfluence influence of media on voters (0.0 - 100.0)
 * @param volatilityRate willingness of voters to change parties (0.0 - 100.0)
 * @param scandalProbability probability of a scandal per tick (0.0 - 60.0)
 * @param loyaltyAverage average initial party loyalty (0.0 - 100.0)
 * @param tickRate simulation speed in Ticks Per Second
 * @param chaosFactor random deviation factor for variance
 * @param partyCount number of active political parties
 * @param budgetEffectiveness multiplier for campaign budget impact
 * @param seed the random seed for determinism
 * @author Nico Hoffmann
 * @version 1.2
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
        double budgetEffectiveness,
        long seed // Das 10. Feld
) {
    public SimulationParameters {
        if (populationSize < 0) throw new IllegalArgumentException("Population cannot be negative");
        if (tickRate < 1) throw new IllegalArgumentException("Tick rate must be at least 1");
    }

    public SimulationParameters withTickRate(int newTickRate) {
        return new SimulationParameters(
                populationSize, mediaInfluence, volatilityRate, scandalProbability,
                loyaltyAverage, newTickRate, chaosFactor, partyCount, budgetEffectiveness, seed
        );
    }
}
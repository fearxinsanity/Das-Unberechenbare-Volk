package de.schulprojekt.duv.model.core;

/**
 * Holds all configurable parameters of the simulation.
 * Serves as the immutable "Single Source of Truth" for settings modified via the GUI.
 *
 * @param populationSize      General: Total number of voters (Simulated Agents).
 * @param mediaInfluence      Dynamic: Influence of media (0.0 - 100.0).
 * @param volatilityRate      Dynamic: Willingness to change parties (0.0 - 100.0).
 * @param scandalProbability  Dynamic: Probability of a scandal per tick (0.0 - 60.0).
 * @param loyaltyAverage      Stats: Average party loyalty at start (0.0 - 100.0).
 * @param tickRate            System: Simulation speed in Ticks Per Second (TPS).
 * @param chaosFactor         Stats: Random deviation/variance factor.
 * @param partyCount          General: Number of active parties.
 * @param budgetEffectiveness Stats: Multiplier for the impact of campaign budgets.
 *
 * @author Nico Hoffmann
 * @version 1.0
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

    // --- Helper Methods (Copying) ---

    /**
     * Creates a COPY of these parameters with a new tick rate.
     * Used for live speed adjustments during simulation.
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
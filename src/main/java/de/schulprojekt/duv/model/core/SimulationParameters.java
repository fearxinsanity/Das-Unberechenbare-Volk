package de.schulprojekt.duv.model.core;

/**
 * Holds all configurable parameters of the simulation.
 * Serves as the "Single Source of Truth" for settings that can be modified via the GUI.
 */
public class SimulationParameters {

    // --- CONSTANTS (Defaults) ---
    private static final int DEFAULT_VOTER_COUNT = 250000;
    private static final double DEFAULT_MEDIA_INFLUENCE = 65.0;
    private static final double DEFAULT_MOBILITY_RATE = 35.0;
    private static final double DEFAULT_SCANDAL_CHANCE = 5.0;
    private static final double DEFAULT_INITIAL_LOYALTY = 50.0;
    private static final int DEFAULT_TICKS_PER_SECOND = 60;
    private static final double DEFAULT_RANDOM_RANGE = 1.0;
    private static final int DEFAULT_PARTY_COUNT = 6;
    private static final double DEFAULT_BUDGET_FACTOR = 1.0;

    // --- FIELDS ---

    // General Settings
    private final int populationSize;
    private final int partyCount;

    // Dynamic Settings
    private final double mediaInfluence;   // 0-100
    private final double volatilityRate;       // 0-100 (Willingness to change)
    private final double scandalProbability;          // Probability (0-60)

    // Statistical Initialization
    private final double loyaltyAverage;     // 0-100
    private final double chaosFactor;     // Random factor (Variance)
    private final double budgetEffectiveness;   // Multiplier for budget influence

    // System Settings
    private final int tickRate;  // Speed (TPS)

    // --- CONSTRUCTORS ---

    /**
     * Full constructor for all parameters.
     * The order corresponds exactly to the usage in DashboardController.
     *
     * @param populationSize          Number of voters (Simulated Agents)
     * @param mediaInfluence     Influence of media (0-100)
     * @param volatilityRate         Base probability for party switching (0-100)
     * @param scandalProbability            Probability for scandals
     * @param loyaltyAverage       Average party loyalty at start
     * @param tickRate Speed of simulation (Ticks per second)
     * @param chaosFactor       Random deviation in decisions
     * @param partyCount          Number of parties (excl. Undecided)
     * @param budgetEffectiveness     Weighting of the budget
     */
    public SimulationParameters(int populationSize,
                                double mediaInfluence,
                                double volatilityRate,
                                double scandalProbability,
                                double loyaltyAverage,
                                int tickRate,
                                double chaosFactor,
                                int partyCount,
                                double budgetEffectiveness) {
        this.populationSize = populationSize;
        this.mediaInfluence = mediaInfluence;
        this.volatilityRate = volatilityRate;
        this.scandalProbability = scandalProbability;
        this.loyaltyAverage = loyaltyAverage;
        this.tickRate = tickRate;
        this.chaosFactor = chaosFactor;
        this.partyCount = partyCount;
        this.budgetEffectiveness = budgetEffectiveness;
    }
    // --- HELPER METHODS ---

    /**
     * Creates a COPY of these parameters with a new speed (TPS).
     * Necessary because fields are final (Immutable pattern).
     */
    public SimulationParameters withSimulationTicksPerSecond(int newTps) {
        return new SimulationParameters(
                this.populationSize,
                this.mediaInfluence,
                this.volatilityRate,
                this.scandalProbability,
                this.loyaltyAverage,
                newTps,
                this.chaosFactor,
                this.partyCount,
                this.budgetEffectiveness
        );
    }

    // --- GETTERS ---

    public int getPopulationSize() {
        return populationSize;
    }

    public int getPartyCount() {
        return partyCount;
    }

    public double getMediaInfluence() {
        return mediaInfluence;
    }

    public double getVolatilityRate() {
        return volatilityRate;
    }

    public double getScandalProbability() {
        return scandalProbability;
    }

    public double getLoyaltyAverage() {
        return loyaltyAverage;
    }

    public double getChaosFactor() {
        return chaosFactor;
    }

    public double getBudgetEffectiveness() {
        return budgetEffectiveness;
    }

    public int getTickRate() {
        return tickRate;
    }
}
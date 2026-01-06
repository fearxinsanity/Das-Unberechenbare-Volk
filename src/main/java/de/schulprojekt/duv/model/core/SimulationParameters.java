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
    private final int totalVoterCount;
    private final int numberOfParties;

    // Dynamic Settings
    private final double globalMediaInfluence;   // 0-100
    private final double baseMobilityRate;       // 0-100 (Willingness to change)
    private final double scandalChance;          // Probability (0-60)

    // Statistical Initialization
    private final double initialLoyaltyMean;     // 0-100
    private final double uniformRandomRange;     // Random factor (Variance)
    private final double campaignBudgetFactor;   // Multiplier for budget influence

    // System Settings
    private final int simulationTicksPerSecond;  // Speed (TPS)

    // --- CONSTRUCTORS ---

    /**
     * Default constructor with reasonable default values.
     */
    public SimulationParameters() {
        this(
                DEFAULT_VOTER_COUNT,
                DEFAULT_MEDIA_INFLUENCE,
                DEFAULT_MOBILITY_RATE,
                DEFAULT_SCANDAL_CHANCE,
                DEFAULT_INITIAL_LOYALTY,
                DEFAULT_TICKS_PER_SECOND,
                DEFAULT_RANDOM_RANGE,
                DEFAULT_PARTY_COUNT,
                DEFAULT_BUDGET_FACTOR
        );
    }

    /**
     * Full constructor for all parameters.
     * The order corresponds exactly to the usage in DashboardController.
     *
     * @param totalVoterCount          Number of voters (Simulated Agents)
     * @param globalMediaInfluence     Influence of media (0-100)
     * @param baseMobilityRate         Base probability for party switching (0-100)
     * @param scandalChance            Probability for scandals
     * @param initialLoyaltyMean       Average party loyalty at start
     * @param simulationTicksPerSecond Speed of simulation (Ticks per second)
     * @param uniformRandomRange       Random deviation in decisions
     * @param numberOfParties          Number of parties (excl. Undecided)
     * @param campaignBudgetFactor     Weighting of the budget
     */
    public SimulationParameters(int totalVoterCount,
                                double globalMediaInfluence,
                                double baseMobilityRate,
                                double scandalChance,
                                double initialLoyaltyMean,
                                int simulationTicksPerSecond,
                                double uniformRandomRange,
                                int numberOfParties,
                                double campaignBudgetFactor) {
        this.totalVoterCount = totalVoterCount;
        this.globalMediaInfluence = globalMediaInfluence;
        this.baseMobilityRate = baseMobilityRate;
        this.scandalChance = scandalChance;
        this.initialLoyaltyMean = initialLoyaltyMean;
        this.simulationTicksPerSecond = simulationTicksPerSecond;
        this.uniformRandomRange = uniformRandomRange;
        this.numberOfParties = numberOfParties;
        this.campaignBudgetFactor = campaignBudgetFactor;
    }
    // --- HELPER METHODS ---

    /**
     * Creates a COPY of these parameters with a new speed (TPS).
     * Necessary because fields are final (Immutable pattern).
     */
    public SimulationParameters withSimulationTicksPerSecond(int newTps) {
        return new SimulationParameters(
                this.totalVoterCount,
                this.globalMediaInfluence,
                this.baseMobilityRate,
                this.scandalChance,
                this.initialLoyaltyMean,
                newTps,
                this.uniformRandomRange,
                this.numberOfParties,
                this.campaignBudgetFactor
        );
    }

    // --- GETTERS ---

    public int getTotalVoterCount() {
        return totalVoterCount;
    }

    public int getNumberOfParties() {
        return numberOfParties;
    }

    public double getGlobalMediaInfluence() {
        return globalMediaInfluence;
    }

    public double getBaseMobilityRate() {
        return baseMobilityRate;
    }

    public double getScandalChance() {
        return scandalChance;
    }

    public double getInitialLoyaltyMean() {
        return initialLoyaltyMean;
    }

    public double getUniformRandomRange() {
        return uniformRandomRange;
    }

    public double getCampaignBudgetFactor() {
        return campaignBudgetFactor;
    }

    public int getSimulationTicksPerSecond() {
        return simulationTicksPerSecond;
    }
}
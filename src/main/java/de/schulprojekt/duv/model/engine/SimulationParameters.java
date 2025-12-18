package de.schulprojekt.duv.model.engine;

/**
 * Parameter für die Simulation.
 * Enthält alle konfigurierbaren Werte.
 */
public class SimulationParameters {

    private int totalVoterCount;
    private double globalMediaInfluence;
    private double baseMobilityRate;
    private double scandalChance;
    private double initialLoyaltyMean;
    private int simulationTicksPerSecond;
    private double uniformRandomRange;
    private int numberOfParties;

    // NEU: Faktor für das Kampagnenbudget (Erfüllt Anforderung: Budget-Einstellung)
    private double campaignBudgetFactor;

    // Konstante für den "Unsicher"-Anteil
    public static final double UNDECIDED_TRANSITION_RATE = 0.6;

    public SimulationParameters(int totalVoterCount, double globalMediaInfluence,
                                double baseMobilityRate, double scandalChance, double initialLoyaltyMean,
                                int simulationTicksPerSecond, double uniformRandomRange, int numberOfParties,
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

    // --- Getter und Setter ---

    public int getTotalVoterCount() { return totalVoterCount; }
    public void setTotalVoterCount(int totalVoterCount) { this.totalVoterCount = totalVoterCount; }

    public double getGlobalMediaInfluence() { return globalMediaInfluence; }
    public void setGlobalMediaInfluence(double globalMediaInfluence) { this.globalMediaInfluence = globalMediaInfluence; }

    public double getBaseMobilityRate() { return baseMobilityRate; }
    public void setBaseMobilityRate(double baseMobilityRate) { this.baseMobilityRate = baseMobilityRate; }

    public double getScandalChance() { return scandalChance; }
    public void setScandalChance(double scandalChance) { this.scandalChance = scandalChance; }

    public double getInitialLoyaltyMean() { return initialLoyaltyMean; }
    public void setInitialLoyaltyMean(double initialLoyaltyMean) { this.initialLoyaltyMean = initialLoyaltyMean; }

    public int getSimulationTicksPerSecond() { return simulationTicksPerSecond; }
    public void setSimulationTicksPerSecond(int simulationTicksPerSecond) { this.simulationTicksPerSecond = simulationTicksPerSecond; }

    public double getUniformRandomRange() { return uniformRandomRange; }
    public void setUniformRandomRange(double uniformRandomRange) { this.uniformRandomRange = uniformRandomRange; }

    public int getNumberOfParties() { return numberOfParties; }
    public void setNumberOfParties(int numberOfParties) { this.numberOfParties = numberOfParties; }

    public double getCampaignBudgetFactor() { return campaignBudgetFactor; }
    public void setCampaignBudgetFactor(double campaignBudgetFactor) { this.campaignBudgetFactor = campaignBudgetFactor; }

    public int getTotalSimulationTicks() {
        return Integer.MAX_VALUE;
    }
}
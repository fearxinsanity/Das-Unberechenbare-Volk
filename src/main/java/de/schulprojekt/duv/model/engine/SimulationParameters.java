package de.schulprojekt.duv.model.engine;

public class SimulationParameters {

    private int totalVoterCount;
    private double globalMediaInfluence;
    private double baseMobilityRate;
    private double scandalChance;
    private double initialLoyaltyMean;
    private int simulationTicksPerSecond;
    private double uniformRandomRange;
    private int numberOfParties;

    public SimulationParameters(int totalVoterCount, double globalMediaInfluence, double baseMobilityRate, double scandalChance, double initialLoyaltyMean, int simulationTicksPerSecond, double uniformRandomRange, int numberOfParties) {
        this.totalVoterCount = totalVoterCount;
        this.globalMediaInfluence = globalMediaInfluence;
        this.baseMobilityRate = baseMobilityRate;
        this.scandalChance = scandalChance;
        this.initialLoyaltyMean = initialLoyaltyMean;
        this.simulationTicksPerSecond = simulationTicksPerSecond;
        this.uniformRandomRange = uniformRandomRange;
        this.numberOfParties = numberOfParties;
    }

    public int getTotalVoterCount() {
        return totalVoterCount;
    }

    public void setTotalVoterCount(int totalVoterCount) {
        this.totalVoterCount = totalVoterCount;
    }

    public double getGlobalMediaInfluence() {
        return globalMediaInfluence;
    }

    public void setGlobalMediaInfluence(double globalMediaInfluence) {
        this.globalMediaInfluence = globalMediaInfluence;
    }

    public double getBaseMobilityRate() {
        return baseMobilityRate;
    }

    public void setBaseMobilityRate(double baseMobilityRate) {
        this.baseMobilityRate = baseMobilityRate;
    }

    public double getScandalChance() {
        return scandalChance;
    }

    public void setScandalChance(double scandalChance) {
        this.scandalChance = scandalChance;
    }

    public double getInitialLoyaltyMean() {
        return initialLoyaltyMean;
    }

    public void setInitialLoyaltyMean(double initialLoyaltyMean) {
        this.initialLoyaltyMean = initialLoyaltyMean;
    }

    public int getSimulationTicksPerSecond() {
        return simulationTicksPerSecond;
    }

    public void setSimulationTicksPerSecond(int simulationTicksPerSecond) {
        this.simulationTicksPerSecond = simulationTicksPerSecond;
    }

    public double getUniformRandomRange() {
        return uniformRandomRange;
    }

    public void setUniformRandomRange(double uniformRandomRange) {
        this.uniformRandomRange = uniformRandomRange;
    }

    public int getNumberOfParties() {
        return numberOfParties;
    }

    public void setNumberOfParties(int numberOfParties) {
        this.numberOfParties = numberOfParties;
    }
}

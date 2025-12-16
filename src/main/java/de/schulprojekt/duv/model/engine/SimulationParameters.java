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
    // Removed: private int simulationDurationSeconds;

    // Konstante für den "Unsicher"-Anteil
    public static final double UNDECIDED_TRANSITION_RATE = 0.6; // 60% der Wechsel gehen erst zu "Unsicher"

    public SimulationParameters(int totalVoterCount, double globalMediaInfluence,
                                double baseMobilityRate, double scandalChance, double initialLoyaltyMean,
                                int simulationTicksPerSecond, double uniformRandomRange, int numberOfParties) {
        // Der 9-Argumente-Konstruktor und die Dauer wurden entfernt.
        this.totalVoterCount = totalVoterCount;
        this.globalMediaInfluence = globalMediaInfluence;
        this.baseMobilityRate = baseMobilityRate;
        this.scandalChance = scandalChance;
        this.initialLoyaltyMean = initialLoyaltyMean;
        this.simulationTicksPerSecond = simulationTicksPerSecond;
        this.uniformRandomRange = uniformRandomRange;
        this.numberOfParties = numberOfParties;
    }

    // --- Getter und Setter ---

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

    // Removed: getSimulationDurationSeconds() and setSimulationDurationSeconds()

    /**
     * Berechnet die Gesamtzahl der Ticks. Gibt Integer.MAX_VALUE zurück, da die
     * Simulation nun unbegrenzt läuft.
     */
    public int getTotalSimulationTicks() {
        return Integer.MAX_VALUE;
    }
}
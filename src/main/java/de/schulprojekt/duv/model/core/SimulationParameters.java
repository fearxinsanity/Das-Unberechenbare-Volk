package de.schulprojekt.duv.model.core;

/**
 * Hält alle konfigurierbaren Parameter der Simulation.
 * Diese Klasse dient als "Single Source of Truth" für Einstellungen,
 * die vom Nutzer über die GUI geändert werden können.
 */
public class SimulationParameters {

    // --- Grundeinstellungen ---
    private int totalVoterCount;
    private int numberOfParties;

    // --- Dynamik-Einstellungen ---
    private double globalMediaInfluence;   // 0-100
    private double baseMobilityRate;       // 0-100 (Wechselwille)
    private double scandalChance;          // Wahrscheinlichkeit (0-60)

    // --- Statistik-Initialisierung ---
    private double initialLoyaltyMean;     // 0-100
    private double uniformRandomRange;     // Zufallsfaktor (Varianz)
    private double campaignBudgetFactor;   // Multiplikator für Budget-Einfluss

    // --- System-Einstellungen ---
    private int simulationTicksPerSecond;  // Geschwindigkeit (TPS)

    /**
     * Standard-Konstruktor mit sinnvollen Default-Werten.
     */
    public SimulationParameters() {
        this(250000, 65.0, 35.0, 5.0, 50.0, 60, 1.0, 6, 1.0);
    }

    /**
     * Vollständiger Konstruktor für alle Parameter.
     * Die Reihenfolge entspricht exakt der Nutzung im DashboardController.
     *
     * @param totalVoterCount          Anzahl der Wähler (Simulierte Agenten)
     * @param globalMediaInfluence     Einfluss der Medien (0-100)
     * @param baseMobilityRate         Grundwahrscheinlichkeit für Parteiwechsel (0-100)
     * @param scandalChance            Wahrscheinlichkeit für Skandale
     * @param initialLoyaltyMean       Durchschnittliche Parteitreue zu Beginn
     * @param simulationTicksPerSecond Geschwindigkeit der Simulation (Ticks pro Sekunde)
     * @param uniformRandomRange       Zufallsstreuung bei Entscheidungen
     * @param numberOfParties          Anzahl der Parteien (exkl. Unsicher)
     * @param campaignBudgetFactor     Gewichtung des Budgets
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

    // --- Getter und Setter ---

    public int getTotalVoterCount() {
        return totalVoterCount;
    }

    public void setTotalVoterCount(int totalVoterCount) {
        this.totalVoterCount = totalVoterCount;
    }

    public int getNumberOfParties() {
        return numberOfParties;
    }

    public void setNumberOfParties(int numberOfParties) {
        this.numberOfParties = numberOfParties;
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

    public double getUniformRandomRange() {
        return uniformRandomRange;
    }

    public void setUniformRandomRange(double uniformRandomRange) {
        this.uniformRandomRange = uniformRandomRange;
    }

    public double getCampaignBudgetFactor() {
        return campaignBudgetFactor;
    }

    public void setCampaignBudgetFactor(double campaignBudgetFactor) {
        this.campaignBudgetFactor = campaignBudgetFactor;
    }

    public int getSimulationTicksPerSecond() {
        return simulationTicksPerSecond;
    }

    public void setSimulationTicksPerSecond(int simulationTicksPerSecond) {
        this.simulationTicksPerSecond = simulationTicksPerSecond;
    }
}
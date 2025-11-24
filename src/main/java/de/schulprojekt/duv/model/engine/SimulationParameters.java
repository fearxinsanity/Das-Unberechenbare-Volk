package de.schulprojekt.duv.model.engine;

import java.util.ArrayList;
import java.util.List;

public class SimulationParameters {

    // --- GUI-controlled parameters ---
    private int voterCount = 2000000;
    private double mediaInfluence = 0.65;
    private double voterMobility = 0.35;
    private int partyCount = 4;
    private double initialBudget = 10000.0;
    private double scandalLambda = 0.05;
    private List<Double> partyBudgets = new ArrayList<>();
    private static final double DEFAULT_BUDGET = 50000.0;

    /**
     * Percentage of budget spent per party, per tick.
     */
    private double budgetSpendRatePerTick = 0.01; // 1%

    /**
     * Minimum effectiveness for a campaign (Uniform Distribution).
     */
    private double campaignEffectivenessMin = 0.05; // 5%

    /**
     * Maximum effectiveness for a campaign (Uniform Distribution).
     */
    private double campaignEffectivenessMax = 0.20; // 20%

    /**
     * Scales the raw (budget * effectiveness) value to fit the
     * 0.0-1.0 political orientation scale.
     */
    private double campaignInfluenceScalingFactor = 0.00001;

    public double getBudgetForParty(int partyIndex) {
        if (partyIndex >= 0 && partyIndex < partyBudgets.size()) {
            return partyBudgets.get(partyIndex);
        }
        // Fallback, if no budget (or too few) were provided
        return DEFAULT_BUDGET;
    }

    public void setPartyBudgets(List<Double> partyBudgets) {
        this.partyBudgets = partyBudgets;
    }

    public int getVoterCount() {
        return voterCount;
    }

    public void setVoterCount(int voterCount) {
        this.voterCount = voterCount;
    }

    public double getMediaInfluence() {
        return mediaInfluence;
    }

    public void setMediaInfluence(double mediaInfluence) {
        this.mediaInfluence = mediaInfluence;
    }

    public double getVoterMobility() {
        return voterMobility;
    }

    public void setVoterMobility(double voterMobility) {
        this.voterMobility = voterMobility;
    }

    public int getPartyCount() {
        return partyCount;
    }

    public void setPartyCount(int partyCount) {
        this.partyCount = partyCount;
    }

    public double getInitialBudget() {
        return initialBudget;
    }

    public void setInitialBudget(double initialBudget) {
        this.initialBudget = initialBudget;
    }

    public double getScandalLambda() {
        return scandalLambda;
    }

    public void setScandalLambda(double scandalLambda) {
        this.scandalLambda = scandalLambda;
    }

    public double getBudgetSpendRatePerTick() {
        return budgetSpendRatePerTick;
    }
    public double getCampaignEffectivenessMin() {
        return campaignEffectivenessMin;
    }
    public double getCampaignEffectivenessMax() {
        return campaignEffectivenessMax;
    }
    public double getCampaignInfluenceScalingFactor() {
        return campaignInfluenceScalingFactor;
    }

}

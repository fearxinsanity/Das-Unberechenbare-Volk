package de.schulprojekt.duv.model.engine;

public class SimulationParameters {

    private int voterCount = 2500;
    private double mediaInfluence = 0.65;
    private double voterMobility = 0.35;
    private int partyCount = 4;
    private double initialBudget = 10000.0;
    private double scandalLambda = 0.05;

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
}

package de.schulprojekt.duv.model.entities;

public class Party {
    private final String name;
    private final double corePosition;
    private double campaignBudget;
    private long supporterCount;

    public Party(String name, double corePosition, double initialBudget) {
        this.name = name;
        this.corePosition = corePosition;
        this.campaignBudget = initialBudget;
        this.supporterCount = 0;
    }

    public String getName() {
        return name;
    }

    public double getCorePosition() {
        return corePosition;
    }

    public double getCampaignBudget() {
        return campaignBudget;
    }

    public void setCampaignBudget(double campaignBudget) {
        this.campaignBudget = campaignBudget;
    }

    public long getSupporterCount() {
        return supporterCount;
    }

    public void setSupporterCount(long supporterCount) {
        this.supporterCount = supporterCount;
    }
}

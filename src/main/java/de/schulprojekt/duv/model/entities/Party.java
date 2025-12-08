package de.schulprojekt.duv.model.entities;

public class Party {

    private String name;
    private String colorCode;
    private double politicalPosition;
    private double campaignBudget;
    private int currentSupporterCount;

    public Party(String name, String colorCode, double politicalPosition, double campaignBudget, int currentSupporterCount) {
        this.name = name;
        this.colorCode = colorCode;
        this.politicalPosition = politicalPosition;
        this.campaignBudget = campaignBudget;
        this.currentSupporterCount = currentSupporterCount;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColorCode() {
        return colorCode;
    }

    public void setColorCode(String colorCode) {
        this.colorCode = colorCode;
    }

    public double getPoliticalPosition() {
        return politicalPosition;
    }

    public void setPoliticalPosition(double politicalPosition) {
        this.politicalPosition = politicalPosition;
    }

    public double getCampaignBudget() {
        return campaignBudget;
    }

    public void setCampaignBudget(double campaignBudget) {
        this.campaignBudget = campaignBudget;
    }

    public int getCurrentSupporterCount() {
        return currentSupporterCount;
    }

    public void setCurrentSupporterCount(int currentSupporterCount) {
        this.currentSupporterCount = currentSupporterCount;
    }
}

package de.schulprojekt.duv.model.entities;

public class Party {

    private String fullName;
    private String abbreviation;
    private String colorCode;
    private double politicalPosition; // 0 = Links, 100 = Rechts
    private double campaignBudget;
    private int currentSupporterCount;
    private int scandalCount;

    public Party(String fullName, String abbreviation, String colorCode, double politicalPosition, double campaignBudget, int currentSupporterCount) {
        this.fullName = fullName;
        this.abbreviation = abbreviation;
        this.colorCode = colorCode;
        this.politicalPosition = politicalPosition;
        this.campaignBudget = campaignBudget;
        this.currentSupporterCount = currentSupporterCount;
        this.scandalCount = 0;
    }

    public String getName() { return fullName; }
    public void setName(String name) { this.fullName = name; }
    public String getAbbreviation() { return abbreviation; }
    public String getColorCode() { return colorCode; }

    public double getPoliticalPosition() { return politicalPosition; }

    // --- NEUE METHODE HIER ---
    public String getPoliticalOrientationName() {
        if (politicalPosition < 20) {
            return "Linksextrem";
        } else if (politicalPosition < 40) {
            return "Links";
        } else if (politicalPosition < 60) {
            return "Mitte";
        } else if (politicalPosition < 80) {
            return "Rechts";
        } else {
            return "Rechtsextrem";
        }
    }
    // -------------------------

    public void setPoliticalPosition(double politicalPosition) { this.politicalPosition = politicalPosition; }
    public double getCampaignBudget() { return campaignBudget; }
    public int getCurrentSupporterCount() { return currentSupporterCount; }
    public void setCurrentSupporterCount(int currentSupporterCount) { this.currentSupporterCount = currentSupporterCount; }
    public int getScandalCount() { return scandalCount; }
    public void incrementScandalCount() { this.scandalCount++; }
}
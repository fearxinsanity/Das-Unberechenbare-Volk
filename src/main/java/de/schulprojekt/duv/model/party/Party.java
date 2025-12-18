package de.schulprojekt.duv.model.party;

public class Party {
    private String name;
    private String abbreviation;
    private String colorCode; // Hex-Code als String (z.B. "FF0000")
    private double politicalPosition; // 0 (Links) bis 100 (Rechts)
    private double campaignBudget;
    private int currentSupporterCount;
    private int scandalCount;

    public Party(String name, String abbreviation, String colorCode, double politicalPosition, double campaignBudget, int currentSupporterCount) {
        this.name = name;
        this.abbreviation = abbreviation;
        this.colorCode = colorCode;
        this.politicalPosition = politicalPosition;
        this.campaignBudget = campaignBudget;
        this.currentSupporterCount = currentSupporterCount;
        this.scandalCount = 0;
    }

    // --- Getter & Setter ---

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAbbreviation() { return abbreviation; }
    public void setAbbreviation(String abbreviation) { this.abbreviation = abbreviation; }

    public String getColorCode() { return colorCode; }
    public void setColorCode(String colorCode) { this.colorCode = colorCode; }

    public double getPoliticalPosition() { return politicalPosition; }
    public void setPoliticalPosition(double politicalPosition) { this.politicalPosition = politicalPosition; }

    public double getCampaignBudget() { return campaignBudget; }
    public void setCampaignBudget(double campaignBudget) { this.campaignBudget = campaignBudget; }

    public int getCurrentSupporterCount() { return currentSupporterCount; }
    public void setCurrentSupporterCount(int currentSupporterCount) { this.currentSupporterCount = currentSupporterCount; }

    public int getScandalCount() { return scandalCount; }
    public void incrementScandalCount() { this.scandalCount++; }

    // --- Logik-Methoden für UI ---

    /**
     * Gibt einen lesbaren Text für die politische Ausrichtung zurück.
     * Wichtig für den Tooltip im Dashboard.
     */
    public String getPoliticalOrientationName() {
        if (politicalPosition < 20) return "Linksextrem";
        if (politicalPosition < 40) return "Links";
        if (politicalPosition < 60) return "Zentristisch";
        if (politicalPosition < 80) return "Rechts";
        return "Rechtsextrem";
    }
}
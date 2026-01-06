package de.schulprojekt.duv.model.party;

/**
 * Represents a political party in the simulation.
 * Holds state regarding its position, budget, supporters, and scandal history.
 */
public class Party {

    // --- CONSTANTS (UI Logic) ---

    private static final double LIMIT_FAR_LEFT = 20.0;
    private static final double LIMIT_LEFT = 40.0;
    private static final double LIMIT_CENTER = 60.0;
    private static final double LIMIT_RIGHT = 80.0;

    // --- FIELDS ---

    // Immutable Properties (Identity & Config)
    private final String name;
    private final String abbreviation;
    private final String colorCode;        // Hex-Code as String (e.g. "FF0000")
    private final double politicalPosition; // 0 (Left) to 100 (Right)
    private final double campaignBudget;

    // Mutable State (Simulation Runtime)
    private int currentSupporterCount;
    private int scandalCount;

    // --- CONSTRUCTOR ---

    public Party(String name, String abbreviation, String colorCode, double politicalPosition, double campaignBudget, int currentSupporterCount) {
        this.name = name;
        this.abbreviation = abbreviation;
        this.colorCode = colorCode;
        this.politicalPosition = politicalPosition;
        this.campaignBudget = campaignBudget;
        this.currentSupporterCount = currentSupporterCount;
        this.scandalCount = 0;
    }

    // --- MAIN LOGIC (UI Helpers & Business Logic) ---

    /**
     * Returns a readable string for the political orientation based on position.
     * Used for Tooltips in the Dashboard.
     */
    public String getPoliticalOrientationName() {
        if (politicalPosition < LIMIT_FAR_LEFT) return "Linksextrem";
        if (politicalPosition < LIMIT_LEFT) return "Links";
        if (politicalPosition < LIMIT_CENTER) return "Zentristisch";
        if (politicalPosition < LIMIT_RIGHT) return "Rechts";
        return "Rechtsextrem";
    }

    public void incrementScandalCount() {
        this.scandalCount++;
    }

    // --- GETTERS & SETTERS ---

    public String getName() { return name; }
    public String getAbbreviation() { return abbreviation; }
    public String getColorCode() { return colorCode; }
    public double getPoliticalPosition() { return politicalPosition; }
    public double getCampaignBudget() { return campaignBudget; }
    public int getCurrentSupporterCount() { return currentSupporterCount; }
    public void setCurrentSupporterCount(int currentSupporterCount) { this.currentSupporterCount = currentSupporterCount; }
    public int getScandalCount() { return scandalCount; }
}
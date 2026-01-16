package de.schulprojekt.duv.model.party;

/**
 * Represents a political party in the simulation.
 * @author Nico Hoffmann
 * @version 1.0
 */
public class Party {

    // ========================================
    // Static Variables
    // ========================================

    private static final double LIMIT_FAR_LEFT = 20.0;
    private static final double LIMIT_LEFT = 40.0;
    private static final double LIMIT_CENTER = 60.0;
    private static final double LIMIT_RIGHT = 80.0;

    // ========================================
    // Instance Variables
    // ========================================

    private final String name;
    private final String abbreviation;
    private final String colorCode;
    private final double politicalPosition;
    private final double campaignBudget;
    private int currentSupporterCount;
    private int scandalCount;

    // ========================================
    // Constructors
    // ========================================

    public Party(String name, String abbreviation, String colorCode, double politicalPosition, double campaignBudget, int currentSupporterCount) {
        if (politicalPosition < 0 || politicalPosition > 100) {
            throw new IllegalArgumentException("Political position must be between 0 and 100, got: " + politicalPosition);
        }
        if (campaignBudget < 0) {
            throw new IllegalArgumentException("Campaign budget cannot be negative, got: " + campaignBudget);
        }
        if (currentSupporterCount < 0) {
            throw new IllegalArgumentException("Supporter count cannot be negative, got: " + currentSupporterCount);
        }
        this.name = name;
        this.abbreviation = abbreviation;
        this.colorCode = colorCode;
        this.politicalPosition = politicalPosition;
        this.campaignBudget = campaignBudget;
        this.currentSupporterCount = currentSupporterCount;
        this.scandalCount = 0;
    }

    // ========================================
    // Getter Methods
    // ========================================

    public String getName() {
        return name;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public String getColorCode() {
        return colorCode;
    }

    public double getPoliticalPosition() {
        return politicalPosition;
    }

    public double getCampaignBudget() {
        return campaignBudget;
    }

    public int getCurrentSupporterCount() {
        return currentSupporterCount;
    }

    public int getScandalCount() {
        return scandalCount;
    }

    // ========================================
    // Setter Methods
    // ========================================

    public void setCurrentSupporterCount(int currentSupporterCount) {
        this.currentSupporterCount = currentSupporterCount;
    }

    // ========================================
    // Business Logic Methods
    // ========================================

    /**
     * Returns the political orientation name based on position.
     * @return "Linksextrem" (< 20), "Links" (20-40), "Zentristisch" (40-60),
     * "Rechts" (60-80), or "Rechtsextrem" (80-100)
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
}
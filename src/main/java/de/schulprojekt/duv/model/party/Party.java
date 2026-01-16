package de.schulprojekt.duv.model.party;

import de.schulprojekt.duv.util.config.PartyConfig;

/**
 * Represents a political party in the simulation.
 * @author Nico Hoffmann
 * @version 1.1
 */
public class Party {

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
        if (!PartyConfig.isValidPosition(politicalPosition)) {
            throw new IllegalArgumentException(String.format(
                    "Political position must be between %.0f and %.0f, got: %.1f",
                    PartyConfig.MIN_POSITION,
                    PartyConfig.MAX_POSITION,
                    politicalPosition
            ));
        }
        if (campaignBudget < PartyConfig.MIN_BUDGET) {
            throw new IllegalArgumentException("Campaign budget cannot be negative, got: " + campaignBudget);
        }
        if (currentSupporterCount < PartyConfig.MIN_SUPPORTERS) {
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
        if (currentSupporterCount < PartyConfig.MIN_SUPPORTERS) {
            throw new IllegalArgumentException("Supporter count cannot be negative, got: " + currentSupporterCount);
        }
        this.currentSupporterCount = currentSupporterCount;
    }

    // ========================================
    // Business Logic Methods
    // ========================================

    /**
     * Returns the political orientation name based on position.
     * @return political orientation classification
     */
    public String getPoliticalOrientationName() {
        return PartyConfig.getOrientationName(politicalPosition);
    }

    public void incrementScandalCount() {
        this.scandalCount++;
    }
}

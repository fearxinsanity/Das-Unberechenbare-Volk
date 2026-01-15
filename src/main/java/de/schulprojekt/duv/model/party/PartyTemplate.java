package de.schulprojekt.duv.model.party;

/**
 * A blueprint for a party loaded from a CSV file.
 * * @param name the full name of the party
 * @param abbreviation the short identifier
 * @param colorCode hex color for UI representation
 * * @author Nico Hoffmann
 * @version 1.1
 * @since Java 16
 */
public record PartyTemplate(
        String name,
        String abbreviation,
        String colorCode
) {

    // ========================================
    // Transformation Methods
    // ========================================

    /**
     * Creates a fully-fledged Party instance from this template.
     * * @param politicalPosition assigned position (0-100)
     * @param campaignBudget initial budget
     * @return a new Party instance
     */
    public Party toParty(double politicalPosition, double campaignBudget) {
        return new Party(name, abbreviation, colorCode, politicalPosition, campaignBudget, 0);
    }

    // ========================================
    // Utility Methods
    // ========================================

    @Override
    public String toString() {
        return abbreviation + " - " + name;
    }
}
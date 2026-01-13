package de.schulprojekt.duv.model.party;

/**
 * A blueprint for a party loaded from a CSV file.
 * Implemented as a Java Record because it is a pure, immutable data carrier (DTO).
 * Used to generate random party instances during the initialization phase.
 */
public record PartyTemplate(String name, String abbreviation, String colorCode) {

    // --- MAIN LOGIC (Conversion) ---

    /**
     * Creates a fully-fledged Party instance from this template.
     * Combines the static template data (Name, ID, Color) with dynamic simulation parameters.
     *
     * @param politicalPosition The assigned political position (0-100).
     * @param campaignBudget    The initial campaign budget.
     * @return A new, ready-to-use Party instance.
     */
    public Party toParty(double politicalPosition, double campaignBudget) {
        // Pass record components to Party constructor
        // Initial supporter count is 0 (will be calculated by SimulationEngine)
        return new Party(name, abbreviation, colorCode, politicalPosition, campaignBudget, 0);
    }

    // --- OVERRIDES ---

    @Override
    public String toString() {
        return abbreviation + " - " + name;
    }
}
package de.schulprojekt.duv.model.party;

import de.schulprojekt.duv.util.config.PartyConfig;

/**
 * Eine Vorlage für eine Partei, die aus einer CSV-Datei geladen wird.
 * Dient als Datenspeicher für die statischen Informationen einer Partei.
 *
 * @param name der vollständige Name der Partei
 * @param abbreviation das Kürzel der Partei
 * @param colorCode der standardmäßige Hex-Farbcode für die UI-Darstellung
 * @author Nico Hoffmann
 * @version 1.0
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
     * Baut aus dieser Vorlage eine richtige Partei zusammen.
     * Hier werden die festen Informationen aus der CSV mit den Werten kombiniert.
     *
     * @param politicalPosition die zugewiesene politische Position
     * @param campaignBudget das verfügbare Budget für den Wahlkampf
     * @param color die zu verwendende Farbe
     * @return eine neue Party-Instanz mit den angegebenen Werten und initialen Unterstützern
     */
    public Party toParty(double politicalPosition, double campaignBudget, String color) {
        return new Party(
                name,
                abbreviation,
                color,
                politicalPosition,
                campaignBudget,
                PartyConfig.MIN_SUPPORTERS
        );
    }

    // ========================================
    // Utility Methods
    // ========================================

    @Override
    public String toString() {
        return abbreviation + " - " + name;
    }
}
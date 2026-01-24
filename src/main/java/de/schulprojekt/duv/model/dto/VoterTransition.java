package de.schulprojekt.duv.model.dto;

import de.schulprojekt.duv.model.party.Party;

/**
 * Datentransferobjekt, beschreibt die Parteienwechsel eines Wählers
 * @param from der ursprünglichen Partei
 * @param to die Zielpartei
 * @author Nico Hoffmann
 * @version 1.0
 */
public record VoterTransition(
        Party from,
        Party to
) {

    // ========================================
    // Custom Accessor Methods
    // ========================================

    /**
     * Gibt eine formatierte Bezeichnung für den Wechsel zurück.
     * @return Übergangs-Pfeil-Zeichenfolge
     */
    public String getTransitionLabel() {
        return String.format("%s ➔ %s", from.getName(), to.getName());
    }

    // ========================================
    // Utility Methods
    // ========================================

    @Override
    public String toString() {
        return getTransitionLabel();
    }
}
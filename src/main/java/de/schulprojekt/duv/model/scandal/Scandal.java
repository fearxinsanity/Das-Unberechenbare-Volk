package de.schulprojekt.duv.model.scandal;

/**
 * Repräsentiert einen Skandal-Datensatz.
 * @param id Eindeutige Kennung des Skandals
 * @param type Kategorie des Skandals
 * @param title Titel für die Anzeige
 * @param description Kurze Beschreibung
 * @param strength Intensitätsfaktor
 * @author Nico Hoffmann
 * @version 1.0
 */
public record Scandal(
        int id,
        String type,
        String title,
        String description,
        double strength
) {

    // ========================================
    // Utility Methods
    // ========================================

    @Override
    public String toString() {
        return title + " (" + (int) (strength * 100) + "%)";
    }
}
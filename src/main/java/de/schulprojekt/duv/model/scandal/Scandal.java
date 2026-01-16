package de.schulprojekt.duv.model.scandal;

/**
 * Represents a scandal data record.
 * @param id unique identifier of the scandal
 * @param type category of the scandal
 * @param title display title
 * @param description short text description
 * @param strength intensity factor (0.0 to 1.0)
 * @author Nico Hoffmann
 * @version 1.1
 * @since Java 16
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
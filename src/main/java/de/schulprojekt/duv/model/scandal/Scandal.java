package de.schulprojekt.duv.model.scandal;

/**
 * Repräsentiert einen Skandal-Datensatz.
 * Aktualisiert für 5 Parameter (inkl. ID).
 *
 * @param id          Die eindeutige ID des Datensatzes.
 * @param type        Der Typ des Skandals (z.B. "CORRUPTION").
 * @param title       Der Titel (z.B. "Spendenaffäre").
 * @param description Eine kurze Beschreibung (z.B. "Illegale Gelder …").
 * @param strength    Die Stärke des Skandals (0.0 bis 1.0).
 */
public record Scandal(
        int id,
        String type,
        String title,
        String description,
        double strength
)

{
    @Override
    public String toString() {
        return title + " (" + (int) (strength * 100) + "%)";
    }
}
package de.schulprojekt.duv.model.scandal;

/**
 * Repräsentiert einen Skandal-Datensatz.
 * Aktualisiert für 5 Parameter (inkl. ID).
 */
public class Scandal {
    private final int id;
    private final String type;        // z.B. "CORRUPTION"
    private final String title;       // z.B. "Spendenaffäre"
    private final String description; // z.B. "Illegale Gelder..."
    private final double strength;    // 0.0 bis 1.0

    // Konstruktor passend zum CSVLoader (5 Argumente)
    public Scandal(int id, String type, String title, String description, double strength) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.description = description;
        this.strength = strength;
    }

    // --- Getter ---

    public int getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public double getStrength() {
        return strength;
    }

    @Override
    public String toString() {
        return title + " (" + (int)(strength * 100) + "%)";
    }
}
package de.schulprojekt.duv.model.entities;

/**
 * Repräsentiert einen Skandal-Datensatz aus der CSV.
 * Die Stärke (strength) beeinflusst die Auswirkung auf Wählerstimmen.
 */
public class Scandal {

    private final int id;
    private final String type;
    private final String title;
    private final String description;
    private final double strength; // 0.0 - 1.0

    public Scandal(int id, String type, String title, String description, double strength) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.description = description;
        this.strength = Math.max(0.0, Math.min(1.0, strength));
    }

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
        return String.format("[%s] %s (Stärke: %.0f%%)", type, title, strength * 100);
    }
}
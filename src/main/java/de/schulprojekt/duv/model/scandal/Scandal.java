package de.schulprojekt.duv.model.scandal;

/**
 * Repräsentiert eine Vorlage für einen Skandal (Typ und Grundstärke).
 * Instanzen dieser Klasse werden beim Start aus der CSV geladen und dienen als Blueprint
 * für konkrete {@link ScandalEvent}s.
 */
public class Scandal {

    private final String name;
    private final double strength; // Basis-Schadenswirkung (0.0 bis 1.0)
    private final String description;

    /**
     * Erstellt einen neuen Skandal-Typ.
     * * @param name Bezeichnung (z.B. "Spendenaffäre")
     * @param strength Stärke der Auswirkung (wird mit Zufall und Parametern verrechnet)
     * @param description Text für die UI-Benachrichtigung
     */
    public Scandal(String name, double strength, String description) {
        this.name = name;
        this.strength = strength;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public double getStrength() {
        return strength;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return String.format("%s (Stärke: %.2f)", name, strength);
    }
}
package de.schulprojekt.duv.model.party;

import javafx.scene.paint.Color;

/**
 * Repräsentiert eine politische Partei oder Gruppe (z.B. Nichtwähler) in der Simulation.
 * Diese Klasse hält die Stammdaten (Name, Farbe, Position) sowie den aktuellen Status
 * (Wählerzahlen, Budget, Skandal-Statistik).
 */
public class Party {

    // --- Stammdaten (Immutable) ---
    private final String name;
    private final String abbreviation; // Kürzel für Diagramme
    private final Color color;
    private final double politicalPosition; // Wert von 0.0 (Links) bis 100.0 (Rechts)

    // --- Dynamische Daten (Mutable) ---
    private int currentSupporterCount;
    private double campaignBudget;

    // Statistik
    private int scandalCount = 0;

    /**
     * Konstruktor für eine neue Partei.
     *
     * @param name Name der Partei
     * @param abbreviation Kürzel (z.B. "SPD", "CDU")
     * @param color Farbe für die UI
     * @param politicalPosition Politische Ausrichtung (0-100)
     * @param initialBudget Startbudget
     * @param initialSupporters Startanzahl der Wähler
     */
    public Party(String name, String abbreviation, Color color, double politicalPosition, double initialBudget, int initialSupporters) {
        this.name = name;
        this.abbreviation = abbreviation;
        this.color = color;
        this.politicalPosition = politicalPosition;
        this.campaignBudget = initialBudget;
        this.currentSupporterCount = initialSupporters;
    }

    // --- Logik-Methoden ---

    public void incrementScandalCount() {
        this.scandalCount++;
    }

    /**
     * Zieht Budget für Wahlkampfmaßnahmen ab.
     * @param amount Betrag
     * @return true, wenn genug Budget vorhanden war
     */
    public boolean spendBudget(double amount) {
        if (campaignBudget >= amount) {
            campaignBudget -= amount;
            return true;
        }
        return false;
    }

    // --- Getter & Setter ---

    public String getName() {
        return name;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public Color getColor() {
        return color;
    }

    public double getPoliticalPosition() {
        return politicalPosition;
    }

    public int getCurrentSupporterCount() {
        return currentSupporterCount;
    }

    public void setCurrentSupporterCount(int currentSupporterCount) {
        this.currentSupporterCount = currentSupporterCount;
    }

    public double getCampaignBudget() {
        return campaignBudget;
    }

    public void setCampaignBudget(double campaignBudget) {
        this.campaignBudget = campaignBudget;
    }

    public int getScandalCount() {
        return scandalCount;
    }

    @Override
    public String toString() {
        return String.format("%s (%d Wähler, Pos: %.1f)", name, currentSupporterCount, politicalPosition);
    }
}
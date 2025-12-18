package de.schulprojekt.duv.model.entities;

/**
 * Template für eine Partei aus der CSV-Datei.
 * Wird verwendet um zufällig Parteien zu generieren.
 */
public class PartyTemplate {

    private final String name;
    private final String abbreviation;
    private final String colorCode;

    public PartyTemplate(String name, String abbreviation, String colorCode) {
        this.name = name;
        this.abbreviation = abbreviation;
        this.colorCode = colorCode;
    }

    public String getName() {
        return name;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public String getColorCode() {
        return colorCode;
    }

    /**
     * Erstellt eine vollwertige Party-Instanz aus diesem Template.
     */
    public Party toParty(double politicalPosition, double campaignBudget) {
        // Hier übergeben wir jetzt Name UND Abkürzung
        return new Party(name, abbreviation, colorCode, politicalPosition, campaignBudget, 0);
    }

    @Override
    public String toString() {
        return abbreviation + " - " + name;
    }
}
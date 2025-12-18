package de.schulprojekt.duv.model.party;

/**
 * Template für eine Partei aus der CSV-Datei.
 * Wird verwendet um zufällig Parteien zu generieren.
 */
public class PartyTemplate {

    private final String name;
    private final String abbreviation;
    private final String colorCode; // Korrigiert von colorHex zu colorCode

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
     * @param politicalPosition Die politische Position (0-100).
     * @param campaignBudget Das Startbudget.
     * @return Neue Party-Instanz.
     */
    public Party toParty(double politicalPosition, double campaignBudget) {
        // Übergibt die Werte an den Party-Konstruktor
        // colorCode wird durchgereicht
        return new Party(name, abbreviation, colorCode, politicalPosition, campaignBudget, 0);
    }

    @Override
    public String toString() {
        return abbreviation + " - " + name;
    }
}
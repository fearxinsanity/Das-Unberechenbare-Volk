package de.schulprojekt.duv.model.party;

import javafx.scene.paint.Color;

/**
 * Ein Template für eine Partei, wie es aus der CSV-Datei gelesen wird.
 * Diese Klasse dient als Vorlage (Blueprint). Erst durch den Aufruf von {@link #toParty}
 * wird daraus ein lebendiges Objekt für die Simulation, angereichert mit dynamischen Werten
 * wie Position und Budget.
 */
public class PartyTemplate {

    private final String name;
    private final String abbreviation;
    private final String colorHex;

    /**
     * Erstellt eine neue Parteivorlage.
     *
     * @param name Der volle Name der Partei.
     * @param abbreviation Das Kürzel (z.B. "SPD").
     * @param colorHex Der Farbcode als Hex-String (z.B. "#FF0000").
     */
    public PartyTemplate(String name, String abbreviation, String colorHex) {
        this.name = name;
        this.abbreviation = abbreviation;
        this.colorHex = colorHex;
    }

    /**
     * Erzeugt eine Instanz einer echten {@link Party} basierend auf diesem Template.
     *
     * @param politicalPosition Die zugewiesene politische Position (0-100).
     * @param initialBudget Das zugewiesene Startbudget.
     * @return Ein neues Party-Objekt.
     */
    public Party toParty(double politicalPosition, double initialBudget) {
        Color color;
        try {
            // Versuche, den Hex-Code (z.B. "#FF0000") in ein JavaFX Color-Objekt zu wandeln
            color = Color.web(this.colorHex);
        } catch (IllegalArgumentException | NullPointerException e) {
            // Fallback, falls die CSV fehlerhaft ist
            color = Color.GREY;
            System.err.println("Warnung: Ungültiger Farbcode für " + name + ": " + colorHex);
        }

        // Initiale Wählerzahl ist immer 0, da diese erst bei der Simulations-Initialisierung berechnet wird
        return new Party(
                this.name,
                this.abbreviation,
                color,
                politicalPosition,
                initialBudget,
                0
        );
    }

    public String getName() {
        return name;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public String getColorHex() {
        return colorHex;
    }
}
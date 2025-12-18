package de.schulprojekt.duv.model.scandal;

import de.schulprojekt.duv.model.party.Party;

/**
 * Repräsentiert ein konkretes Skandal-Ereignis, das zu einem bestimmten Zeitpunkt
 * in der Simulation aufgetreten ist.
 * Verknüpft den Skandal-Typ (z.B. "Korruption") mit der betroffenen Partei und dem Zeitpunkt.
 */
public class ScandalEvent {

    private final Scandal scandal;
    private final Party affectedParty;
    private final int occurredAtStep;

    /**
     * Erstellt ein neues Skandal-Ereignis.
     *
     * @param scandal Der Typ des Skandals (Vorlage).
     * @param affectedParty Die betroffene Partei.
     * @param occurredAtStep Der Zeitschritt (Tick), in dem der Skandal begann.
     */
    public ScandalEvent(Scandal scandal, Party affectedParty, int occurredAtStep) {
        this.scandal = scandal;
        this.affectedParty = affectedParty;
        this.occurredAtStep = occurredAtStep;
    }

    public Scandal getScandal() {
        return scandal;
    }

    public Party getAffectedParty() {
        return affectedParty;
    }

    public int getOccurredAtStep() {
        return occurredAtStep;
    }

    /**
     * Erzeugt eine lesbare Nachricht für die UI (News-Feed/Ticker).
     */
    public String getEventMessage() {
        // Beispiel: "⚠ SKANDAL: Spendenaffäre erschüttert die CDU! (Illegale Gelder angenommen)"
        return String.format("⚠ SKANDAL: %s erschüttert die %s! (%s)",
                scandal.getName(),
                affectedParty.getName(),
                scandal.getDescription());
    }

    @Override
    public String toString() {
        return getEventMessage();
    }
}
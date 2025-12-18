package de.schulprojekt.duv.model.scandal;

import de.schulprojekt.duv.model.party.Party;

/**
 * Repräsentiert ein aktives Skandal-Ereignis in der Simulation.
 * Enthält den Skandal, die betroffene Partei und den Zeitstempel.
 */
public class ScandalEvent {

    private final Scandal scandal;
    private final Party affectedParty;
    private final int occurredAtStep;

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
     * Formatierte Nachricht für den Event-Feed.
     */
    public String getEventMessage() {
        // FEHLERBEHEBUNG: scandal.getName() -> scandal.getTitle()
        return String.format("⚠ %s: %s betrifft %s",
                scandal.getType(),
                scandal.getTitle(),
                affectedParty.getName());
    }

    @Override
    public String toString() {
        return getEventMessage();
    }
}
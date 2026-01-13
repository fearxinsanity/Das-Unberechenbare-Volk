package de.schulprojekt.duv.model.scandal;

import de.schulprojekt.duv.model.party.Party;

/**
 * Repräsentiert ein aktives Skandal-Ereignis in der Simulation.
 * Enthält den Skandal, die betroffene Partei und den Zeitstempel.
 */
public record ScandalEvent(Scandal scandal, Party affectedParty, int occurredAtStep) {

    /**
     * Formatierte Nachricht für den Event-Feed.
     */
    public String getEventMessage() {
        // FEHLERBEHEBUNG: scandal.getName() -> scandal.getTitle()
        return String.format("⚠ %s: %s betrifft %s",
                scandal.type(),
                scandal.title(),
                affectedParty.getName());
    }

    @Override
    public String toString() {
        return getEventMessage();
    }
}
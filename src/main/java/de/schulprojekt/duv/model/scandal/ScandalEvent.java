package de.schulprojekt.duv.model.scandal;

import de.schulprojekt.duv.model.party.Party;

/**
 * Represents an active scandal event in the simulation.
 * Contains the scandal, the affected party, and the timestamp when it occurred.
 *
 * @param scandal the scandal that occurred
 * @param affectedParty the party affected by the scandal
 * @param occurredAtStep the simulation step when the scandal occurred
 *
 * @author Nico Hoffmann
 * @version 1.0
 * @since Java 16
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
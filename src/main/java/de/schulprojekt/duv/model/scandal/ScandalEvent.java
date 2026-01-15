package de.schulprojekt.duv.model.scandal;

import de.schulprojekt.duv.model.party.Party;

/**
 * Represents an active scandal event in the simulation.
 * * @param scandal the scandal data
 * @param affectedParty the party affected by the scandal
 * @param occurredAtStep the simulation step when it occurred
 * * @author Nico Hoffmann
 * @version 1.1
 * @since Java 16
 */
public record ScandalEvent(
        Scandal scandal,
        Party affectedParty,
        int occurredAtStep
) {

    // ========================================
    // Custom Accessor Methods
    // ========================================

    /**
     * Returns a formatted message for the event feed.
     * * @return formatted event message
     */
    public String getEventMessage() {
        return String.format("⚠ %s: %s betrifft %s",
                scandal.type(),
                scandal.title(),
                affectedParty.getName());
    }

    // ========================================
    // Utility Methods
    // ========================================

    @Override
    public String toString() {
        return getEventMessage();
    }
}
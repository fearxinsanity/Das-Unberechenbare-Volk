package de.schulprojekt.duv.model.scandal;

import de.schulprojekt.duv.model.party.Party;

/**
 * Repräsentiert ein aktives Skandal-Ereignis in der Simulation.
 * @param scandal Die Skandaldaten
 * @param affectedParty Die vom Skandal betroffene Partei
 * @param occurredAtStep Der Simulationsschritt, in dem der Skandal auftrat
 * @author Nico Hoffmann
 * @version 1.0
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
     * Gibt eine formatierte Nachricht für den Ereignis-Feed zurück
     * @return Formatierte Ereignismeldung
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
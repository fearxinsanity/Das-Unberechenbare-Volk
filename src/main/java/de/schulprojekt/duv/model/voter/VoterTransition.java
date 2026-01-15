package de.schulprojekt.duv.model.voter;

import de.schulprojekt.duv.model.party.Party;

/**
 * An immutable record representing a voter transition between parties.
 * Used primarily for visualization (e.g., flying particles in UI).
 *
 * @param from the party the voter is leaving
 * @param to the party the voter is joining
 *
 * @author Nico Hoffmann
 * @version 1.0
 * @since Java 16
 */
public record VoterTransition(Party from, Party to) {

    // --- Business Logic / Formatting ---

    /**
     * Erstellt ein formatiertes Label für UI-Anzeigen oder Logs.
     */
    public String getTransitionLabel() {
        return String.format("%s ➔ %s", from.getName(), to.getName());
    }

    // --- Technical Overrides ---

    @Override
    public String toString() {
        return getTransitionLabel();
    }
}
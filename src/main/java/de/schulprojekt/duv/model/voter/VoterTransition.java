package de.schulprojekt.duv.model.voter;

import de.schulprojekt.duv.model.party.Party;

/**
 * Ein unveränderlicher Datensatz, der einen Wählerwechsel beschreibt.
 * Dient hauptsächlich der Visualisierung (z.B. fliegende Punkte im UI).
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
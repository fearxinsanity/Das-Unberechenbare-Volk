package de.schulprojekt.duv.model.voter;

import de.schulprojekt.duv.model.party.Party;

/**
 * Ein einfaches Datenobjekt (DTO), das einen einzelnen Wählerwechsel beschreibt.
 * Wird verwendet, um Visualisierungsdaten (Punkte, die sich bewegen) von der Engine zur UI zu senden.
 *
 * Instanzen dieser Klasse sind unveränderlich (Immutable).
 */
public class VoterTransition {

    private final Party oldParty;
    private final Party newParty;

    /**
     * Erstellt ein neues Übergangs-Event.
     *
     * @param oldParty Die Partei, die der Wähler verlassen hat.
     * @param newParty Die Partei, zu der der Wähler gewechselt ist.
     */
    public VoterTransition(Party oldParty, Party newParty) {
        this.oldParty = oldParty;
        this.newParty = newParty;
    }

    public Party getOldParty() {
        return oldParty;
    }

    public Party getNewParty() {
        return newParty;
    }
}
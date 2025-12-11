package de.schulprojekt.duv.model.engine;

import de.schulprojekt.duv.model.entities.Party;

/**
 * A Data Transfer Object (DTO) that represents a single voter transition event
 * Used by SimulationEngine to pass animation data to the SimulationController.
 */
public class VoterTransition {
    private final Party oldParty;
    private final Party newParty;

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

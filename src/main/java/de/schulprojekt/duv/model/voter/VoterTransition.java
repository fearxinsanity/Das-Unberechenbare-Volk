package de.schulprojekt.duv.model.voter;

import de.schulprojekt.duv.model.party.Party;

/**
 * Immutable Data Transfer Object describing a voter's party change.
 * * @param from the original party
 * @param to the target party
 * * @author Nico Hoffmann
 * @version 1.1
 * @since Java 16
 */
public record VoterTransition(
        Party from,
        Party to
) {

    // ========================================
    // Custom Accessor Methods
    // ========================================

    /**
     * Returns a formatted label for the transition.
     * * @return transition arrow string
     */
    public String getTransitionLabel() {
        return String.format("%s ➔ %s", from.getName(), to.getName());
    }

    // ========================================
    // Utility Methods
    // ========================================

    @Override
    public String toString() {
        return getTransitionLabel();
    }
}
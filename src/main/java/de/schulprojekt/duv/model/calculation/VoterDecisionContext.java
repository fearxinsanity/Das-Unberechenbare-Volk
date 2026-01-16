package de.schulprojekt.duv.model.calculation;

import de.schulprojekt.duv.model.voter.VoterType;

/**
 * Immutable record encapsulating all relevant context for a voter's party selection decision.
 * @author Nico Hoffmann
 * @version 1.0
 * @param position voter's political position (0-100 scale)
 * @param loyalty voter's party loyalty value
 * @param mediaInfluence voter's susceptibility to media influence
 * @param voterType scientific voter archetype (pragmatic, ideological, etc.)
 * @param currentPartyIndex index of voter's current party (0 = undecided)
 * @param currentPenalty total scandal pressure on current party
 */
public record VoterDecisionContext(
        float position,
        float loyalty,
        float mediaInfluence,
        VoterType voterType,
        int currentPartyIndex,
        double currentPenalty
) {
}

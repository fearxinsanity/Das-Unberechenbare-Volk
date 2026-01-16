package de.schulprojekt.duv.model.calculation;

/**
 * Immutable record containing the evaluation breakdown of a party's attractiveness to a voter.
 * @author Nico Hoffmann
 * @version 1.0
 * @param partyIndex index of evaluated party
 * @param distanceScore score based on political distance (higher = closer match)
 * @param budgetScore score from campaign budget and media effectiveness
 * @param penalty total penalty from scandals (acute + permanent)
 * @param finalScore total score after applying noise (used for party selection)
 */
public record PartyEvaluationResult(
        int partyIndex,
        double distanceScore,
        double budgetScore,
        double penalty,
        double finalScore
) {
}

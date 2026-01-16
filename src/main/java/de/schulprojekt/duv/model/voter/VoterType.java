package de.schulprojekt.duv.model.voter;

/**
 * Defines scientifically-based voter archetypes with distinct behavioral patterns.
 * Based on electoral behavior research distinguishing pragmatic, ideological,
 * rational-choice, affective, heuristic, and politically disengaged voters.
 * @author Nico Hoffmann
 * @version 1.0
 */
public enum VoterType {

    /**
     * Pragmatic voters prioritize personal benefits and direct utility.
     * Low party loyalty, high switching probability, moderate media influence.
     */
    PRAGMATIC(0.3, 1.2, 0.8),

    /**
     * Ideological voters have strong worldview-based convictions.
     * High party loyalty, low switching probability, resistant to media influence.
     */
    IDEOLOGICAL(0.85, 0.5, 1.5),

    /**
     * Rational-choice voters systematically evaluate policy positions and self-interest.
     * Medium loyalty, high position-sensitivity, moderate media influence.
     */
    RATIONAL_CHOICE(0.5, 0.9, 1.2),

    /**
     * Affective voters decide based on emotions and gut feelings.
     * Low loyalty, high charisma-sensitivity, strong media influence.
     */
    AFFECTIVE(0.4, 1.4, 0.6),

    /**
     * Heuristic voters use mental shortcuts (party identification, media cues).
     * Medium loyalty, very high media influence, low position-sensitivity.
     */
    HEURISTIC(0.55, 1.6, 0.7),

    /**
     * Politically disengaged voters have inconsistent preferences and low knowledge.
     * Very low loyalty, highest switching probability, moderate media influence.
     */
    POLITIKFERN(0.2, 1.1, 0.5);

    // ========================================
    // Instance Variables
    // ========================================

    private final double loyaltyModifier;
    private final double mediaModifier;
    private final double distanceSensitivity;

    // ========================================
    // Constructors
    // ========================================

    VoterType(double loyaltyModifier, double mediaModifier, double distanceSensitivity) {
        this.loyaltyModifier = loyaltyModifier;
        this.mediaModifier = mediaModifier;
        this.distanceSensitivity = distanceSensitivity;
    }

    // ========================================
    // Getter Methods
    // ========================================

    public double getLoyaltyModifier() {
        return loyaltyModifier;
    }

    public double getMediaModifier() {
        return mediaModifier;
    }

    public double getDistanceSensitivity() {
        return distanceSensitivity;
    }
}

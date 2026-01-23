package de.schulprojekt.duv.model.voter;

/**
 * Definiert Archetypen mit unterschiedlichen Verhaltensmustern.
 * Basierend auf der Wahlverhaltensforschung, die zwischen pragmatischen, ideologischen,
 * Rational-Choice-, affektiven, heuristischen und politikfernen Wählern unterscheidet.
 * @author Nico Hoffmann
 * @version 1.0
 */
public enum VoterType {

    /**
     * Pragmatische Wähler priorisieren persönlichen Nutzen und direkten Vorteil.
     * Geringe Parteiloyalität, hohe Wechselwahrscheinlichkeit, moderater Medieneinfluss.
     */
    PRAGMATIC(0.3, 1.2, 0.8),

    /**
     * Ideologische Wähler haben starke, weltanschaulich geprägte Überzeugungen.
     * Hohe Parteiloyalität, geringe Wechselwahrscheinlichkeit, resistent gegen Medieneinfluss.
     */
    IDEOLOGICAL(0.85, 0.5, 1.5),

    /**
     * Rational-Choice-Wähler bewerten systematisch politische Positionen und Eigeninteresse.
     * Mittlere Loyalität, hohe Positionssensitivität, moderater Medieneinfluss.
     */
    RATIONAL_CHOICE(0.5, 0.9, 1.2),

    /**
     * Affektive Wähler entscheiden basierend auf Emotionen und Bauchgefühl.
     * Geringe Loyalität, hohe Charisma-Sensitivität, starker Medieneinfluss.
     */
    AFFECTIVE(0.4, 1.4, 0.6),

    /**
     * Heuristische Wähler nutzen mentale Abkürzungen (Parteidentifikation, Medienhinweise).
     * Mittlere Loyalität, sehr hoher Medieneinfluss, geringe Positionssensitivität.
     */
    HEURISTIC(0.55, 1.6, 0.7),

    /**
     * Politikferne Wähler haben inkonsistente Präferenzen und geringes Wissen.
     * Sehr geringe Loyalität, höchste Wechselwahrscheinlichkeit, moderater Medieneinfluss.
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
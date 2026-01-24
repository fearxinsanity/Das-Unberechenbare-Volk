package de.schulprojekt.duv.util.config;

/**
 * Konstanten Konfiguration für VoterBehavior Algorithmen.
 *
 * @author Nico Hoffmann
 * @version 1.0
 */
public final class VoterBehaviorConfig {

    // ========================================
    // Opinion Dynamics Constants
    // ========================================

    /**
     * Maximaler Drift der Wählermeinung pro Tick auf dem politischen Spektrum.
     */
    public static final double OPINION_DRIFT_FACTOR = 0.25;

    /**
     * Maximaler Absolutwert für den globalen politischen Trend.
     */
    public static final double ZEITGEIST_MAX_AMPLITUDE = 8.0;

    /**
     * Maximale Änderung des Zeitgeists pro Tick.
     */
    public static final double ZEITGEIST_DRIFT_STRENGTH = 0.15;

    /**
     * Gewichtungsfaktor für den Einfluss des globalen Trends auf den individuellen Meinungstrend.
     */
    public static final double GLOBAL_TREND_WEIGHT = 0.1;

    // ========================================
    // Population Initialization (Externalisiert aus VoterPopulation)
    // ========================================

    /** Anteil der Wähler, die zu Beginn unentschlossen sind. */
    public static final double UNDECIDED_RATIO = 0.20;

    /** Mittelwert für die initiale politische Positionierung. */
    public static final double POS_MEAN = 50.0;

    /** Standardabweichung für die initiale politische Positionierung. */
    public static final double POS_STD_DEV = 25.0;

    /** Exponent zur Berechnung des medialen Einflusses. */
    public static final double MEDIA_INFLUENCE_EXPONENT = 0.7;

    // ========================================
    // Dynamics Constants (Externalisiert aus VoterPopulation)
    // ========================================

    /** Wahrscheinlichkeit für einen Wechsel des Wählertyps pro Tick. */
    public static final double TYPE_CHANGE_PROBABILITY = 0.0001;

    /** Maximale Fluktuation der Loyalität pro Tick. */
    public static final float LOYALTY_FLUCTUATION = 2.0f;

    /** Maximale Drift des Medieneinflusses pro Tick. */
    public static final float MEDIA_INFLUENCE_DRIFT = 0.05f;

    // ========================================
    // Voter Type Probabilities (Externalisiert aus VoterPopulation)
    // ========================================

    public static final double PROB_PRAGMATIC = 0.25;
    public static final double PROB_IDEOLOGICAL = 0.15;
    public static final double PROB_RATIONAL_CHOICE = 0.20;
    public static final double PROB_AFFECTIVE = 0.15;
    public static final double PROB_HEURISTIC = 0.15;

    // ========================================
    // Party Switching Probability Constants
    // ========================================

    /** Divisor zur Dämpfung des Loyalitätseffekts auf die Wechselwahrscheinlichkeit. */
    public static final double LOYALTY_DAMPING_FACTOR = 180.0;

    /** Maximale Wahrscheinlichkeit, dass ein Wähler einen Wechsel überhaupt in Betracht zieht. */
    public static final double MAX_SWITCH_PROBABILITY = 0.65;

    /** Multiplikator für die Mobilität unentschlossener Wähler. */
    public static final double UNDECIDED_MOBILITY_BONUS = 1.3;

    /** Wahrscheinlichkeit, dass ein unzufriedener Wähler unentschlossen wird, statt eine andere Partei zu wählen. */
    public static final double RESIGNATION_PROBABILITY = 0.15;

    // ========================================
    // Scandal Impact Constants
    // ========================================

    public static final double ACUTE_SCANDAL_MULTIPLIER = 0.018;
    public static final double MAX_ACUTE_SCANDAL_BOOST = 0.35;
    public static final double ACUTE_SCANDAL_PENALTY_WEIGHT = 2.5;
    public static final double PERMANENT_SCANDAL_PENALTY_WEIGHT = 0.8;
    public static final double DISASTER_FLIGHT_THRESHOLD = 25.0;

    // ========================================
    // Party Evaluation Constants
    // ========================================

    public static final double DISTANCE_SCORE_BASE = 40.0;
    public static final double DISTANCE_SENSITIVITY = 0.04;
    public static final double DECISION_NOISE_FACTOR = 12.0;

    // ========================================
    // Campaign Budget Constants
    // ========================================

    public static final double BUDGET_SCORE_MULTIPLIER = 12.0;

    // ========================================
    // Daily Momentum Constants
    // ========================================

    public static final double MOMENTUM_BASE = 0.8;
    public static final double MOMENTUM_VARIANCE = 0.4;

    /**
     * Privater Konstruktor, um Instanziierung zu verhindern.
     */
    private VoterBehaviorConfig() {
        throw new UnsupportedOperationException("Configuration class cannot be instantiated");
    }
}
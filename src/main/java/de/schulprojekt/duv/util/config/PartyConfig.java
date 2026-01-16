package de.schulprojekt.duv.util.config;

/**
 * Configuration constants for party behavior and political orientation classification.
 * Defines thresholds for the political spectrum and party-related calculations.
 *
 * @author Nico Hoffmann
 * @version 1.0
 */
public final class PartyConfig {

    // ========================================
// Political Orientation Thresholds
// ========================================

    /**
     * Upper bound for far-left political classification.
     *
     * <p><b>Rationale:</b> Positions 0-20 represent far-left/extreme left parties
     * (e.g., communist, radical socialist). This matches typical left-right scale
     * divisions used in political science where extremes occupy ~20% of the spectrum.</p>
     */
    public static final double LIMIT_FAR_LEFT = 20.0;

    /**
     * Upper bound for center-left political classification.
     *
     * <p><b>Rationale:</b> Positions 20-40 represent moderate left parties
     * (e.g., social democrats, progressive liberals). This creates a balanced
     * 5-tier system with 20-point intervals.</p>
     */
    public static final double LIMIT_LEFT = 40.0;

    /**
     * Upper bound for centrist political classification.
     *
     * <p><b>Rationale:</b> Positions 40-60 represent centrist parties
     * (e.g., moderate liberals, centrists). The middle 20 points ensure
     * a realistic center that's not too narrow or too broad.</p>
     */
    public static final double LIMIT_CENTER = 60.0;

    /**
     * Upper bound for center-right political classification.
     *
     * <p><b>Rationale:</b> Positions 60-80 represent moderate right parties
     * (e.g., conservatives, Christian democrats). Symmetric with center-left.</p>
     */
    public static final double LIMIT_RIGHT = 80.0;

    // ========================================
    // Validation Constants
    // ========================================

    /**
     * Minimum valid political position on the 0-100 scale.
     */
    public static final double MIN_POSITION = 0.0;

    /**
     * Maximum valid political position on the 0-100 scale.
     */
    public static final double MAX_POSITION = 100.0;

    /**
     * Minimum valid campaign budget.
     */
    public static final double MIN_BUDGET = 0.0;

    /**
     * Minimum valid supporter count.
     */
    public static final int MIN_SUPPORTERS = 0;

    // ========================================
    // Constructors
    // ========================================

    /**
     * Private constructor to prevent instantiation.
     * This is a utility class with only static constants.
     */
    private PartyConfig() {
        throw new UnsupportedOperationException("Configuration class cannot be instantiated");
    }

    // ========================================
    // Utility Methods
    // ========================================

    /**
     * Gets the political orientation name for a given position.
     *
     * @param position the political position (0-100)
     * @return the orientation name
     */
    public static String getOrientationName(double position) {
        if (position < LIMIT_FAR_LEFT) return "Linksextrem";
        if (position < LIMIT_LEFT) return "Links";
        if (position < LIMIT_CENTER) return "Zentristisch";
        if (position < LIMIT_RIGHT) return "Rechts";
        return "Rechtsextrem";
    }

    /**
     * Validates if a political position is within valid bounds.
     *
     * @param position the position to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidPosition(double position) {
        return position >= MIN_POSITION && position <= MAX_POSITION;
    }
}

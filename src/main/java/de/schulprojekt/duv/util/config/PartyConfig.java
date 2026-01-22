package de.schulprojekt.duv.util.config;

import de.schulprojekt.duv.view.Main;

import java.util.ResourceBundle;

/**
 * Configuration constants for party behavior and political orientation classification.
 * Defines thresholds for the political spectrum and party-related calculations.
 * Supports localization.
 *
 * @author Nico Hoffmann
 * @version 1.2
 */
public final class PartyConfig {

    // ========================================
    // Political Orientation Thresholds
    // ========================================

    /**
     * Upper bound for far-left political classification.
     */
    public static final double LIMIT_FAR_LEFT = 20.0;

    /**
     * Upper bound for center-left political classification.
     */
    public static final double LIMIT_LEFT = 40.0;

    /**
     * Upper bound for centrist political classification.
     */
    public static final double LIMIT_CENTER = 60.0;

    /**
     * Upper bound for center-right political classification.
     */
    public static final double LIMIT_RIGHT = 80.0;

    // ========================================
    // Validation Constants
    // ========================================

    public static final double MIN_POSITION = 0.0;
    public static final double MAX_POSITION = 100.0;
    public static final double MIN_BUDGET = 0.0;
    public static final int MIN_SUPPORTERS = 0;

    // ========================================
    // Constructors
    // ========================================

    private PartyConfig() {
        throw new UnsupportedOperationException("Configuration class cannot be instantiated");
    }

    // ========================================
    // Utility Methods
    // ========================================

    /**
     * Gets the localized political orientation name for a given position.
     * Uses the current locale from Main class.
     *
     * @param position the political position (0-100)
     * @return the localized orientation name
     */
    public static String getOrientationName(double position) {
        // Load the bundle based on the current active locale
        ResourceBundle bundle = ResourceBundle.getBundle("de.schulprojekt.duv.messages", Main.getLocale());

        if (position < LIMIT_FAR_LEFT) return bundle.getString("orientation.far_left");
        if (position < LIMIT_LEFT) return bundle.getString("orientation.left");
        if (position < LIMIT_CENTER) return bundle.getString("orientation.center");
        if (position < LIMIT_RIGHT) return bundle.getString("orientation.right");
        return bundle.getString("orientation.far_right");
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
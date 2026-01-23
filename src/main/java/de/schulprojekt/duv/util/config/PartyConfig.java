package de.schulprojekt.duv.util.config;

import de.schulprojekt.duv.view.Main;

import java.util.ResourceBundle;

/**
 * Konfigurationskonstanten für das Parteiverhalten und die Klassifizierung der politischen Orientierung.
 * Definiert Schwellenwerte für das politische Spektrum und parteibezogene Berechnungen.
 * Unterstützt Lokalisierung.
 *
 * @author Nico Hoffmann
 * @version 1.0
 */
public final class PartyConfig {

    // ========================================
    // Political Orientation Thresholds
    // ========================================

    /**
     * Obergrenze für die politisch linksextreme Einstufung.
     */
    public static final double LIMIT_FAR_LEFT = 20.0;

    /**
     * Obergrenze für die politisch linke Einstufung.
     */
    public static final double LIMIT_LEFT = 40.0;

    /**
     * Obergrenze für die politisch zentristische Einstufung.
     */
    public static final double LIMIT_CENTER = 60.0;

    /**
     * Obergrenze für die politisch rechte Einstufung.
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
     * Ruft den lokalisierten Namen der politischen Orientierung für eine gegebene Position ab.
     * Verwendet das aktuelle Locale aus der Main-Klasse.
     *
     * @param position die politische Position
     * @return der lokalisierte Name der Orientierung
     */
    public static String getOrientationName(double position) {
        ResourceBundle bundle = ResourceBundle.getBundle("de.schulprojekt.duv.messages", Main.getLocale());

        if (position < LIMIT_FAR_LEFT) return bundle.getString("orientation.far_left");
        if (position < LIMIT_LEFT) return bundle.getString("orientation.left");
        if (position < LIMIT_CENTER) return bundle.getString("orientation.center");
        if (position < LIMIT_RIGHT) return bundle.getString("orientation.right");
        return bundle.getString("orientation.far_right");
    }

    /**
     * Überprüft, ob eine politische Position innerhalb der gültigen Grenzen liegt.
     *
     * @param position die zu validierende Position
     * @return true, wenn gültig, andernfalls false
     */
    public static boolean isValidPosition(double position) {
        return position >= MIN_POSITION && position <= MAX_POSITION;
    }
}
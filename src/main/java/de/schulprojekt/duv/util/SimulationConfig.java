package de.schulprojekt.duv.util;

import javafx.scene.paint.Color;

/**
 * Static configuration class for system-wide constants.
 * Contains technical settings and design values not changeable by the user at runtime.
 */
public class SimulationConfig {

    // --- Performance & Visualization ---

    /**
     * Percentage of voter transitions to visualize.
     * 0.0005 = 0.05% (Approx. 125 particles per tick at 250k voters).
     * Too high values will cause rendering lag.
     */
    public static final double VISUALIZATION_SAMPLE_RATE = 0.0005;

    /**
     * Number of history steps stored for line charts.
     */
    public static final int HISTORY_LENGTH = 500;

    // --- Simulation Logic ---

    /**
     * Standard deviation for the normal distribution of voter loyalty.
     */
    public static final double DEFAULT_LOYALTY_STD_DEV = 15.0;

    /**
     * Conversion factor: How much budget equals one influence point?
     */
    public static final double CAMPAIGN_BUDGET_FACTOR = 100000.0;

    // --- UI & Design ---

    /**
     * Display name for the "Undecided" / Non-voter faction.
     * Kept in German as it is a UI-facing string.
     */
    public static final String UNDECIDED_NAME = "Unsicher";

    /**
     * Color for the "Undecided" faction (Grey).
     */
    public static final Color UNDECIDED_COLOR = Color.web("#6c757d");

    // --- Constructor ---

    // Private constructor prevents instantiation of this utility class
    private SimulationConfig() {}
}
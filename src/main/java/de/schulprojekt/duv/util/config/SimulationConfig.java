package de.schulprojekt.duv.util.config;

import javafx.scene.paint.Color;

/**
 * Global configuration constants for the simulation.
 * @author Nico Hoffmann
 * @version 1.1
 */
public class SimulationConfig {

    // ========================================
    // Static Variables
    // ========================================

    public static final double VISUALIZATION_SAMPLE_RATE = 0.0005;
    public static final int HISTORY_LENGTH = 500;
    public static final double DEFAULT_LOYALTY_STD_DEV = 15.0;
    public static final double CAMPAIGN_BUDGET_FACTOR = 100000.0;
    public static final String UNDECIDED_NAME = "Unsicher";
    public static final Color UNDECIDED_COLOR = Color.web("#6c757d");
    // Number of ticks before old scandals are removed from active list
    public static final int SCANDAL_MAX_AGE_TICKS = 200;

    // ========================================
    // Constructors
    // ========================================

    private SimulationConfig() {
        // Prevent instantiation
    }
}
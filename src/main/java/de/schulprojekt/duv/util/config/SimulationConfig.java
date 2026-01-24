package de.schulprojekt.duv.util.config;

import javafx.scene.paint.Color;

/**
 * Globale KOnfiguration für Simulationskonstanten.
 * @author Nico Hoffmann
 * @version 1.0
 */
public class SimulationConfig {

    // ========================================
    // Simulation Defaults
    // ========================================

    public static final int DEFAULT_POPULATION = 250_000;
    public static final double DEFAULT_MEDIA_INFLUENCE = 65.0;
    public static final double DEFAULT_VOLATILITY = 35.0;
    public static final double DEFAULT_SCANDAL_PROB = 5.0;
    public static final double DEFAULT_LOYALTY = 50.0;
    public static final int DEFAULT_TICK_RATE = 5;
    public static final double DEFAULT_CHAOS = 1.0;
    public static final int DEFAULT_PARTIES = 4;
    public static final double DEFAULT_BUDGET_WEIGHT = 1.0;

    // ========================================
    // Static Variables
    // ========================================

    public static final double VISUALIZATION_SAMPLE_RATE = 0.0005;
    public static final int HISTORY_LENGTH = 500;
    public static final double DEFAULT_LOYALTY_STD_DEV = 15.0;
    public static final double CAMPAIGN_BUDGET_FACTOR = 100000.0;
    public static final String UNDECIDED_NAME = "Unsicher";
    public static final Color UNDECIDED_COLOR = Color.web("#6c757d");
    public static final int SCANDAL_MAX_AGE_TICKS = 200;

    // ========================================
    // Constructors
    // ========================================

    private SimulationConfig() {
        // Prevent instantiation
    }
}
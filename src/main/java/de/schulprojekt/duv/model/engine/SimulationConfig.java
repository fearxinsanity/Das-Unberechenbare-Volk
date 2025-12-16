package de.schulprojekt.duv.model.engine;

public class SimulationConfig {
    // Performance Einstellungen
    public static final int THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();
    public static final double VISUALIZATION_SAMPLE_RATE = 0.05; // Nur 5% der Wechsel visualisieren (Performance!)

    // Simulation Konstanten
    public static final double DEFAULT_LOYALTY_STD_DEV = 15.0;
    public static final double UNDECIDED_POSITION = 50.0;
    public static final double SCANDAL_IMPACT_FACTOR = 50.0;
    public static final double CAMPAIGN_BUDGET_FACTOR = 100000.0;

    // ANPASSUNG: Historie verlängert für flüssigere Darstellung (500 statt 100)
    public static final int HISTORY_LENGTH = 500;

    // Visualisierung
    public static final int MAX_PARTICLES = 1500; // Maximale Anzahl gleichzeitiger Punkte
    public static final String UNDECIDED_COLOR = "6c757d";
    public static final String UNDECIDED_NAME = "Unsicher";
}
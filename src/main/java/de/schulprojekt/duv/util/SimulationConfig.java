package de.schulprojekt.duv.util;

import javafx.scene.paint.Color;

/**
 * Statische Konfigurationsklasse für systemweite Konstanten.
 * Enthält Werte, die nicht zur Laufzeit vom User geändert werden (im Gegensatz zu SimulationParameters),
 * sondern das technische Verhalten oder Design steuern.
 */
public class SimulationConfig {

    // --- Performance & Visualisierung ---

    // Anzahl der Threads für parallele Berechnungen (z.B. Wähler-Logik)
    public static final int THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();

    // Wie viel Prozent der Wählerwechsel sollen visualisiert werden?
    // Zu hoch = Performance-Einbruch beim Zeichnen. 0.05% bei 250k Wählern sind ca. 125 Punkte pro Tick.
    public static final double VISUALIZATION_SAMPLE_RATE = 0.0005;

    // Maximale Anzahl gleichzeitiger Partikel auf dem Canvas (Sicherheitspuffer)
    public static final int MAX_PARTICLES = 1500;

    // Wie viele Schritte werden im Liniendiagramm gespeichert?
    public static final int HISTORY_LENGTH = 500;


    // --- Logik-Konstanten ---

    // Standardabweichung der Normalverteilung für die Parteitreue
    public static final double DEFAULT_LOYALTY_STD_DEV = 15.0;

    // Umrechnungsfaktor: Wie viel Budget entspricht einem Einfluss-Punkt?
    public static final double CAMPAIGN_BUDGET_FACTOR = 100000.0;

    // Basis-Position für Nichtwähler (Mitte)
    public static final double UNDECIDED_POSITION = 50.0;


    // --- UI & Design ---

    public static final String UNDECIDED_NAME = "Unsicher";

    // Farbe für die "Unsicher"-Fraktion (als JavaFX Color Objekt für direkte Nutzung)
    public static final Color UNDECIDED_COLOR = Color.web("#6c757d"); // Grau

    // Privater Konstruktor verhindert Instanziierung
    private SimulationConfig() {}
}
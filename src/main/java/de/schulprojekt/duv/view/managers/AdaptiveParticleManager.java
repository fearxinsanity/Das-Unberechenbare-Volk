package de.schulprojekt.duv.view.managers;

/**
 * Verwaltet die adaptive Partikelanzahl basierend auf der Echtzeit-Performance.
 *
 * @author Nico Hoffmann
 * @version 1.0
 */
public class AdaptiveParticleManager {

    // ========================================
    // Statische Variablen (Konstanten)
    // ========================================

    private static final int TARGET_FPS = 50;
    private static final int MAX_PARTICLES = 600;
    private static final int MIN_PARTICLES = 100;
    private static final double SMOOTHING_FACTOR = 0.9;
    private static final int ADAPTATION_INTERVAL = 30;

    // Schwellenwerte und Anpassungsschritte
    private static final int FPS_THRESHOLD_SEVERE = 10;
    private static final int FPS_THRESHOLD_MODERATE = 5;
    private static final int FPS_THRESHOLD_RECOVERY = 10;

    private static final int REDUCTION_STEP_SEVERE = 50;
    private static final int REDUCTION_STEP_MODERATE = 20;
    private static final int INCREASE_STEP = 20;

    // ========================================
    // Instanzvariablen
    // ========================================

    private int currentMaxParticles;
    private long lastFrameTime;
    private double smoothedFPS;
    private int framesSinceLastAdaptation;

    // ========================================
    // Konstruktoren
    // ========================================

    /**
     * Erstellt einen neuen adaptiven Partikel-Manager.
     */
    public AdaptiveParticleManager() {
        this.currentMaxParticles = MAX_PARTICLES;
        this.lastFrameTime = System.nanoTime();
        this.smoothedFPS = TARGET_FPS;
        this.framesSinceLastAdaptation = 0;
    }

    // ========================================
    // Getter-Methoden
    // ========================================

    public int getMaxParticles() {
        return currentMaxParticles;
    }

    // ========================================
    // Business-Logik-Methoden
    // ========================================

    /**
     * Aktualisiert die FPS-Messung und passt das Partikel-Limit bei Bedarf an.
     */
    public void updateFrame() {
        long now = System.nanoTime();
        double frameDuration = (now - lastFrameTime) / 1_000_000_000.0;
        lastFrameTime = now;

        double instantFPS = frameDuration > 0 ? 1.0 / frameDuration : 60.0;

        smoothedFPS = SMOOTHING_FACTOR * smoothedFPS + (1.0 - SMOOTHING_FACTOR) * instantFPS;

        framesSinceLastAdaptation++;
        if (framesSinceLastAdaptation >= ADAPTATION_INTERVAL) {
            adaptParticleLimit();
            framesSinceLastAdaptation = 0;
        }
    }

    public void reset() {
        this.lastFrameTime = System.nanoTime();
        this.smoothedFPS = TARGET_FPS;
        this.framesSinceLastAdaptation = 0;
    }

    // ========================================
    // Hilfsmethoden
    // ========================================

    /**
     * Passt das Partikel-Limit basierend auf den aktuellen FPS an.
     */
    private void adaptParticleLimit() {
        if (smoothedFPS < TARGET_FPS - FPS_THRESHOLD_SEVERE) {
            currentMaxParticles = Math.max(MIN_PARTICLES, currentMaxParticles - REDUCTION_STEP_SEVERE);
        } else if (smoothedFPS < TARGET_FPS - FPS_THRESHOLD_MODERATE) {
            currentMaxParticles = Math.max(MIN_PARTICLES, currentMaxParticles - REDUCTION_STEP_MODERATE);
        } else if (smoothedFPS > TARGET_FPS + FPS_THRESHOLD_RECOVERY && currentMaxParticles < MAX_PARTICLES) {
            currentMaxParticles = Math.min(MAX_PARTICLES, currentMaxParticles + INCREASE_STEP);
        }
    }
}
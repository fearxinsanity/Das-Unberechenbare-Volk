package de.schulprojekt.duv.view.managers;

/**
 * Manages adaptive particle count based on real-time performance.
 * Automatically adjusts particle limits to maintain target FPS.
 *
 * @author Nico Hoffmann
 * @version 1.0
 */
public class AdaptiveParticleManager {

    // ========================================
    // Static Variables
    // ========================================

    private static final int TARGET_FPS = 50;
    private static final int MAX_PARTICLES = 600;
    private static final int MIN_PARTICLES = 100;
    private static final double SMOOTHING_FACTOR = 0.9;
    private static final int ADAPTATION_INTERVAL = 30;

    // ========================================
    // Instance Variables
    // ========================================

    private int currentMaxParticles;
    private long lastFrameTime;
    private double smoothedFPS;
    private int framesSinceLastAdaptation;

    // ========================================
    // Constructors
    // ========================================

    /**
     * Creates a new adaptive particle manager.
     * Starts with maximum particle count and adjusts based on performance.
     */
    public AdaptiveParticleManager() {
        this.currentMaxParticles = MAX_PARTICLES;
        this.lastFrameTime = System.nanoTime();
        this.smoothedFPS = TARGET_FPS;
        this.framesSinceLastAdaptation = 0;
    }

    // ========================================
    // Getter Methods
    // ========================================

    /**
     * Gets the current maximum allowed particles.
     *
     * @return current particle limit
     */
    public int getMaxParticles() {
        return currentMaxParticles;
    }

    // ========================================
    // Business Logic Methods
    // ========================================

    /**
     * Updates FPS measurement and adjusts particle limit if needed.
     * Call this method once per render frame.
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

    /**
     * Resets FPS tracking (e.g., after pause/resume).
     */
    public void reset() {
        this.lastFrameTime = System.nanoTime();
        this.smoothedFPS = TARGET_FPS;
        this.framesSinceLastAdaptation = 0;
    }

    // ========================================
    // Utility Methods
    // ========================================

    /**
     * Adjusts particle limit based on current FPS.
     */
    private void adaptParticleLimit() {
        if (smoothedFPS < TARGET_FPS - 10) {
            currentMaxParticles = Math.max(MIN_PARTICLES, currentMaxParticles - 50);
        } else if (smoothedFPS < TARGET_FPS - 5) {
            currentMaxParticles = Math.max(MIN_PARTICLES, currentMaxParticles - 20);
        } else if (smoothedFPS > TARGET_FPS + 10 && currentMaxParticles < MAX_PARTICLES) {
            currentMaxParticles = Math.min(MAX_PARTICLES, currentMaxParticles + 20);
        }
    }
}

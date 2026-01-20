package de.schulprojekt.duv.util.config;

/**
 * Configuration constants for scandal mechanics and impact calculations.
 * Controls how scandals affect parties over time and their recovery dynamics.
 *
 * @author Nico Hoffmann
 * @version 1.0
 */
public final class ScandalConfig {

    // ========================================
    // Scandal Duration Constants
    // ========================================

    /**
     * Total duration a scandal remains active (in ticks).
     *
     * <p><b>Rationale:</b> 300 ticks represents a multi-month scandal lifecycle.
     * If 1 tick = 1 day, this is ~10 months. Real political scandals typically
     * dominate news cycles for 6-12 months before fading from public consciousness.</p>
     */
    public static final int SCANDAL_DURATION = 300;

    /**
     * Number of ticks for scandal impact to reach maximum intensity.
     *
     * <p><b>Rationale:</b> 90 ticks = ~3 months fade-in period. Scandals don't
     * hit instantly at full force - they build as media coverage intensifies,
     * investigations unfold, and public awareness grows. This models the
     * "scandal snowball effect".</p>
     */
    public static final int FADE_IN_TICKS = 90;

    // ========================================
    // Impact Multipliers
    // ========================================

    /**
     * Multiplier for acute (immediate) scandal pressure on voter decisions.
     *
     * <p><b>Rationale:</b> 4.0x means acute scandals have strong immediate impact.
     * If scandal strength is 10, acute pressure becomes 40. This creates noticeable
     * but not overwhelming short-term effects that dominate voter thinking during
     * the scandal's peak.</p>
     */
    public static final double ACUTE_PRESSURE_FACTOR = 6.0;

    /**
     * Multiplier for permanent (long-term) reputation damage accumulation.
     *
     * <p><b>Rationale:</b> 2.0x means permanent damage accumulates at half the rate
     * of acute pressure, but persists after the scandal fades. This models reputation
     * damage that outlasts news cycles (e.g., "the party that had the corruption scandal").</p>
     */
    public static final double PERMANENT_DAMAGE_FACTOR = 1.5;

    // ========================================
    // Recovery Mechanics
    // ========================================

    /**
     * Base rate at which permanent damage recovers per tick (percentage).
     *
     * <p><b>Rationale:</b> 0.5% per tick = 0.005. A party with 10 permanent damage
     * recovers 0.05 per tick, taking 200 ticks to fully recover. This models gradual
     * public forgiveness as scandals fade from collective memory.</p>
     */
    public static final double BASE_RECOVERY_RATE = 0.003;

    /**
     * Additional recovery rate bonus based on voter support share.
     *
     * <p><b>Rationale:</b> 4% bonus = 0.04. If a party has 30% voter share,
     * they get +1.2% recovery rate bonus. Popular parties recover faster because:
     * <ul>
     *   <li>More loyal supporters willing to forgive</li>
     *   <li>Better resources for image rehabilitation</li>
     *   <li>Media attention shifts to other topics faster</li>
     * </ul>
     * This creates realistic dynamics where strong parties bounce back while
     * weak ones struggle.</p>
     */
    public static final double VOTER_SHARE_RECOVERY_FACTOR = 0.04;

    // ========================================
    // Constructors
    // ========================================

    /**
     * Private constructor to prevent instantiation.
     * This is a utility class with only static constants.
     */
    private ScandalConfig() {
        throw new UnsupportedOperationException("Configuration class cannot be instantiated");
    }

    // ========================================
    // Utility Methods
    // ========================================

    /**
     * Calculates the time-based impact factor for a scandal at a given age.
     *
     * <p>Impact follows a quadratic fade-in curve (0 to FADE_IN_TICKS),
     * then linear decay until SCANDAL_DURATION.</p>
     *
     * @param age the scandal age in ticks
     * @return impact factor (0.0 to 1.0)
     */
    public static double calculateTimeFactor(int age) {
        if (age < 0) return 0.0;
        if (age >= SCANDAL_DURATION) return 0.0;

        if (age < FADE_IN_TICKS) {
            // Quadratic fade-in: impact grows gradually, then accelerates
            double progress = (double) age / FADE_IN_TICKS;
            return progress * progress;
        }

        // Linear decay after peak
        return 1.0 - ((double) (age - FADE_IN_TICKS) / (SCANDAL_DURATION - FADE_IN_TICKS));
    }

    /**
     * Calculates per-tick permanent damage accumulation.
     *
     * @param scandalStrength the strength of the scandal
     * @return damage accumulation per tick
     */
    public static double calculatePermanentDamagePerTick(double scandalStrength) {
        return (scandalStrength * PERMANENT_DAMAGE_FACTOR) / (double) SCANDAL_DURATION;
    }

    /**
     * Calculates recovery rate for a party based on their voter share.
     *
     * @param voterSharePercent the party's current voter share (0.0 to 1.0)
     * @return total recovery rate per tick
     */
    public static double calculateRecoveryRate(double voterSharePercent) {
        return BASE_RECOVERY_RATE + (voterSharePercent * VOTER_SHARE_RECOVERY_FACTOR);
    }
}

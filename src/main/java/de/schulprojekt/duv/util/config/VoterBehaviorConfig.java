package de.schulprojekt.duv.util.config;

/**
 * Configuration constants for voter behavior algorithms.
 * Contains all tuning parameters for the voter decision model with scientific justification.
 *
 * <p>These values are calibrated based on electoral behavior research and simulation testing.
 * Modifying these values will significantly affect simulation dynamics.</p>
 *
 * @author Nico Hoffmann
 * @version 1.0
 */
public final class VoterBehaviorConfig {

    // ========================================
    // Opinion Dynamics Constants
    // ========================================

    /**
     * Maximum voter opinion drift per tick on the 0-100 political spectrum.
     *
     * <p><b>Rationale:</b> In real elections, voter positions change gradually.
     * A value of 0.25 per tick allows for realistic opinion shifts without
     * causing extreme volatility. Over 100 ticks, a voter can drift ~25 points
     * (one quarter of the spectrum), which matches observed political realignment timescales.</p>
     *
     * <p><b>Impact:</b> Higher values create more volatile, less stable party support.</p>
     */
    public static final double OPINION_DRIFT_FACTOR = 0.25;

    /**
     * Maximum absolute value for global political trend (Zeitgeist).
     *
     * <p><b>Rationale:</b> Represents society-wide ideological shifts (e.g., leftward
     * or rightward movements). Limited to ±8 to prevent runaway trends that would
     * push all voters to extremes. Historical shifts like Reagan-era conservatism
     * or 1960s progressivism correspond to ~5-10 point movements.</p>
     */
    public static final double ZEITGEIST_MAX_AMPLITUDE = 8.0;

    /**
     * Maximum change in Zeitgeist per tick.
     *
     * <p><b>Rationale:</b> 0.15 per tick creates gradual societal shifts.
     * Over 50 ticks, Zeitgeist can shift ~7.5 points (within MAX_AMPLITUDE),
     * simulating multi-year political realignments.</p>
     */
    public static final double ZEITGEIST_DRIFT_STRENGTH = 0.15;

    /**
     * Weight factor for global trend influence on individual voter opinion drift.
     *
     * <p><b>Rationale:</b> 0.1 means Zeitgeist contributes 10% to opinion drift,
     * while 90% is individual noise. This reflects that most voters are somewhat
     * influenced by cultural zeitgeist but retain individual variability.</p>
     */
    public static final double GLOBAL_TREND_WEIGHT = 0.1;

    // ========================================
    // Party Switching Probability Constants
    // ========================================

    /**
     * Divisor to dampen loyalty effect on switching probability.
     *
     * <p><b>Rationale:</b> Loyalty ranges 0-100. Dividing by 180 means:
     * <ul>
     *   <li>Loyalty 0: No reduction (1.0 - 0/180 = 1.0)</li>
     *   <li>Loyalty 50: 28% reduction (1.0 - 50/180 = 0.72)</li>
     *   <li>Loyalty 100: 56% reduction (1.0 - 100/180 = 0.44)</li>
     * </ul>
     * This creates a gradual, non-linear loyalty effect matching the Michigan Model
     * of party identification.</p>
     */
    public static final double LOYALTY_DAMPING_FACTOR = 180.0;

    /**
     * Maximum probability a voter will consider switching parties.
     *
     * <p><b>Rationale:</b> Even in highly volatile elections (e.g., 2016 US),
     * most voters (~70-80%) stick with their party. A 65% cap ensures some
     * stability even under extreme conditions (scandals + low loyalty + high media).</p>
     */
    public static final double MAX_SWITCH_PROBABILITY = 0.65;

    /**
     * Multiplier for undecided voters' mobility (they switch more easily).
     *
     * <p><b>Rationale:</b> Electoral research shows undecided voters are 30-50%
     * more volatile than decided voters. A 1.3x multiplier falls within this range.</p>
     */
    public static final double UNDECIDED_MOBILITY_BONUS = 1.3;

    /**
     * Probability a dissatisfied voter becomes undecided instead of choosing another party.
     *
     * <p><b>Rationale:</b> In real elections, ~10-20% of dissatisfied voters don't
     * switch parties but become disillusioned non-voters or undecided. 15% matches
     * observed "withdrawal" behavior in turnout studies.</p>
     */
    public static final double RESIGNATION_PROBABILITY = 0.15;

    // ========================================
    // Scandal Impact Constants
    // ========================================

    /**
     * Divisor converting scandal penalty to switch probability increase.
     *
     * <p><b>Rationale:</b> Scandal penalties range 0-100+. Dividing by 1800 means:
     * <ul>
     *   <li>Penalty 20: +1.1% switch probability</li>
     *   <li>Penalty 50: +2.8% switch probability</li>
     *   <li>Penalty 100: +5.6% switch probability</li>
     * </ul>
     * This creates measurable but not overwhelming scandal effects unless penalties
     * accumulate to disaster levels.</p>
     */
    public static final double PENALTY_PRESSURE_DIVISOR = 1800.0;

    /**
     * Scandal pressure threshold triggering panic-mode party switching.
     *
     * <p><b>Rationale:</b> When total scandal damage exceeds 25 (combination of
     * acute + permanent), voters panic and switch immediately without considering
     * alternatives carefully. This models major scandals like Watergate or
     * corruption revelations that cause mass defections.</p>
     */
    public static final double DISASTER_FLIGHT_THRESHOLD = 25.0;

    /**
     * Weight multiplier for permanent scandal damage in decision calculation.
     *
     * <p><b>Rationale:</b> Permanent damage (reputation loss) weighs 1.5x more than
     * acute scandals because voters remember long-term character issues more than
     * temporary controversies. Matches framing theory from political psychology.</p>
     */
    public static final double PERMANENT_DAMAGE_WEIGHT = 1.5;

    // ========================================
    // Party Evaluation Constants
    // ========================================

    /**
     * Base attractiveness score before distance penalty (0-100 scale).
     *
     * <p><b>Rationale:</b> Starting value of 40 ensures that even ideologically
     * distant parties have some baseline appeal. This prevents extreme polarization
     * where voters only consider parties within 10 points. Real voters sometimes
     * vote against ideology due to other factors (charisma, single issues).</p>
     */
    public static final double DISTANCE_SCORE_BASE = 40.0;

    /**
     * Sensitivity factor for political distance penalty (higher = distance matters more).
     *
     * <p><b>Rationale:</b> 0.04 creates a smooth decay curve:
     * <ul>
     *   <li>Distance 0: Score = 40.0</li>
     *   <li>Distance 25: Score = 20.0 (halved)</li>
     *   <li>Distance 50: Score = 13.3</li>
     *   <li>Distance 100: Score = 8.0</li>
     * </ul>
     * This matches spatial voting theory (Downs) where proximity strongly matters
     * but doesn't completely eliminate distant parties.</p>
     */
    public static final double DISTANCE_SENSITIVITY = 0.04;

    /**
     * Random noise amplitude added to party selection scores.
     *
     * <p><b>Rationale:</b> 12.0 on a ~40-60 point scale adds ±6 point randomness,
     * representing information uncertainty, emotional factors, and bounded rationality.
     * This is ~15% noise, matching observed voter inconsistency in surveys.</p>
     */
    public static final double DECISION_NOISE_FACTOR = 12.0;

    // ========================================
    // Campaign Budget Constants
    // ========================================

    /**
     * Campaign budget score multiplier for attractiveness calculation.
     *
     * <p><b>Rationale:</b> After normalization (budget / CAMPAIGN_BUDGET_FACTOR),
     * multiplying by 12 ensures campaign spending has measurable but not overwhelming
     * impact. A well-funded campaign can add ~10-15 points to attractiveness,
     * matching empirical studies showing 5-10% vote share effects from spending.</p>
     */
    public static final double BUDGET_SCORE_MULTIPLIER = 12.0;

    // ========================================
    // Daily Momentum Constants
    // ========================================

    /**
     * Base momentum value for party daily performance variance.
     *
     * <p><b>Rationale:</b> Starting at 0.8 means parties typically operate at
     * 80-120% effectiveness (when combined with MOMENTUM_VARIANCE). This simulates
     * good/bad campaign days without extreme swings.</p>
     */
    public static final double MOMENTUM_BASE = 0.8;

    /**
     * Random variance range for party daily momentum.
     *
     * <p><b>Rationale:</b> 0.4 range creates momentum between 0.8 and 1.2,
     * representing ±20% daily fluctuation. This captures good/bad news cycles,
     * debate performances, and campaign messaging effectiveness.</p>
     */
    public static final double MOMENTUM_VARIANCE = 0.4;

    // ========================================
    // Constructors
    // ========================================

    /**
     * Private constructor to prevent instantiation.
     * This is a utility class with only static constants.
     */
    private VoterBehaviorConfig() {
        throw new UnsupportedOperationException("Configuration class cannot be instantiated");
    }
}

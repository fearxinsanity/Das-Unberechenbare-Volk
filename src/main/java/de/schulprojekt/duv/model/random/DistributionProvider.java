package de.schulprojekt.duv.model.random;

import de.schulprojekt.duv.model.core.SimulationParameters;
import de.schulprojekt.duv.util.SimulationConfig;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;

/**
 * Manages the statistical distributions used in the simulation.
 * Wraps the Apache Commons Math libraries to provide specific probability functions
 * for loyalty, random noise, and scandal occurrence.
 */
public class DistributionProvider {

    // --- CONSTANTS (Math Tuning) ---
    private static final double MIN_SCANDAL_LAMBDA = 0.00001;
    private static final double SCANDAL_PROBABILITY_DIVISOR = 3000.0;
    private static final double UNIFORM_MIN = 0.0;
    private static final double LOYALTY_MIN = 0.0;
    private static final double LOYALTY_MAX = 100.0;

    // --- FIELDS ---
    private NormalDistribution loyaltyDistribution;
    private UniformRealDistribution uniformDistribution;
    private ExponentialDistribution scandalDistribution;

    // --- CONSTRUCTOR ---
    public DistributionProvider() {
        // Distributions are set up in initialize() based on parameters
    }

    // --- INITIALIZATION ---

    /**
     * Sets up the mathematical distributions based on the provided simulation parameters.
     * Re-called whenever parameters change.
     */
    public void initialize(SimulationParameters params) {
        // 1. Normal Distribution for Voter Loyalty (Bell Curve)
        this.loyaltyDistribution = new NormalDistribution(
                params.getInitialLoyaltyMean(),
                SimulationConfig.DEFAULT_LOYALTY_STD_DEV
        );

        // 2. Uniform Distribution for Random Noise (0.0 to Range)
        this.uniformDistribution = new UniformRealDistribution(
                UNIFORM_MIN,
                params.getUniformRandomRange()
        );

        // 3. Exponential Distribution for Scandals (Time until next event)
        double scandalProb = params.getScandalChance();
        // Calculation of Lambda for Poisson process / Exponential distribution
        double scandalLambda = Math.max(MIN_SCANDAL_LAMBDA, scandalProb / SCANDAL_PROBABILITY_DIVISOR);
        this.scandalDistribution = new ExponentialDistribution(1.0 / scandalLambda);
    }

    // --- MAIN LOGIC (Sampling) ---

    public double sampleLoyalty() {
        return Math.max(LOYALTY_MIN, Math.min(LOYALTY_MAX, loyaltyDistribution.sample()));
    }

    public double sampleUniform() {
        return uniformDistribution.sample();
    }

    public double sampleTimeUntilNextScandal() {
        return scandalDistribution.sample();
    }
}
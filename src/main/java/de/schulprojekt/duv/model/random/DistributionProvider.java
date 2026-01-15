package de.schulprojekt.duv.model.random;

import de.schulprojekt.duv.model.core.SimulationParameters;
import de.schulprojekt.duv.util.SimulationConfig;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;

/**
 * Manages the statistical distributions used in the simulation.
 * * @author Nico Hoffmann
 * @version 1.1
 */
public class DistributionProvider {

    // ========================================
    // Static Variables
    // ========================================

    private static final double MIN_SCANDAL_LAMBDA = 0.00001;
    private static final double SCANDAL_PROBABILITY_DIVISOR = 3000.0;
    private static final double UNIFORM_MIN = 0.0;
    private static final double LOYALTY_MIN = 0.0;
    private static final double LOYALTY_MAX = 100.0;

    // ========================================
    // Instance Variables
    // ========================================

    private NormalDistribution loyaltyDistribution;
    private UniformRealDistribution uniformDistribution;
    private ExponentialDistribution scandalDistribution;

    // ========================================
    // Constructors
    // ========================================

    public DistributionProvider() {
        // Initialized via initialize method
    }

    // ========================================
    // Business Logic Methods
    // ========================================

    /**
     * Sets up mathematical distributions based on simulation parameters.
     * * @param params the simulation settings
     */
    public void initialize(SimulationParameters params) {
        this.loyaltyDistribution = new NormalDistribution(
                params.loyaltyAverage(),
                SimulationConfig.DEFAULT_LOYALTY_STD_DEV
        );

        this.uniformDistribution = new UniformRealDistribution(
                UNIFORM_MIN,
                params.chaosFactor()
        );

        double scandalProb = params.scandalProbability();
        double scandalLambda = Math.max(MIN_SCANDAL_LAMBDA, scandalProb / SCANDAL_PROBABILITY_DIVISOR);
        this.scandalDistribution = new ExponentialDistribution(1.0 / scandalLambda);
    }

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
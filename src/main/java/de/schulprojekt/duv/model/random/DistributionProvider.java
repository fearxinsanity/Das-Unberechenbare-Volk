package de.schulprojekt.duv.model.random;

import de.schulprojekt.duv.model.core.SimulationParameters;
import de.schulprojekt.duv.util.SimulationConfig;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;

public class DistributionProvider {

    private NormalDistribution loyaltyDistribution;
    private UniformRealDistribution uniformDistribution;
    private ExponentialDistribution scandalDistribution;

    public void initialize(SimulationParameters params) {
        this.loyaltyDistribution = new NormalDistribution(params.getInitialLoyaltyMean(), SimulationConfig.DEFAULT_LOYALTY_STD_DEV);
        this.uniformDistribution = new UniformRealDistribution(0.0, params.getUniformRandomRange());

        double scandalProb = params.getScandalChance();
        double scandalLambda = Math.max(0.00001, scandalProb / 3000.0);
        this.scandalDistribution = new ExponentialDistribution(1.0 / scandalLambda);
    }

    public double sampleLoyalty() {
        return Math.max(0, Math.min(100, loyaltyDistribution.sample()));
    }

    public double sampleUniform() {
        return uniformDistribution.sample();
    }

    public double sampleTimeUntilNextScandal() {
        return scandalDistribution.sample();
    }
}
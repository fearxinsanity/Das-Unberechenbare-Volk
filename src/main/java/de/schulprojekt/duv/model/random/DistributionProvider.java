package de.schulprojekt.duv.model.random;

import de.schulprojekt.duv.model.core.SimulationParameters;
import de.schulprojekt.duv.model.voter.VoterType;
import de.schulprojekt.duv.util.config.SimulationConfig;
import de.schulprojekt.duv.util.config.VoterBehaviorConfig;
import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.util.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * Verwaltet die in der Simulation verwendeten statistischen Verteilungen.
 * @author Nico Hoffmann
 * @version 1.0
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
    private RandomGenerator randomGenerator;
    private EnumeratedDistribution<VoterType> typeDistribution;

    // ========================================
    // Constructors
    // ========================================

    public DistributionProvider(SimulationParameters params) {
        initialize(params);
    }

    // ========================================
    // Business Logic Methods
    // ========================================

    /**
     * Konfiguriert die mathematischen Verteilungen basierend auf den Simulationsparametern.
     * @param params Die Parameter-Objekte, die die Eingabewerte für die Verteilungen liefern.
     * @see SimulationParameters#scandalProbability()
     */
    public void initialize(SimulationParameters params) {
        this.randomGenerator = new JDKRandomGenerator();

        this.loyaltyDistribution = new NormalDistribution(
                randomGenerator,
                params.loyaltyAverage(),
                SimulationConfig.DEFAULT_LOYALTY_STD_DEV
        );

        this.uniformDistribution = new UniformRealDistribution(
                randomGenerator,
                UNIFORM_MIN,
                params.chaosFactor()
        );

        double scandalProb = params.scandalProbability();
        double scandalLambda = Math.max(MIN_SCANDAL_LAMBDA, scandalProb / SCANDAL_PROBABILITY_DIVISOR);
        this.scandalDistribution = new ExponentialDistribution(randomGenerator, 1.0 / scandalLambda);

        List<Pair<VoterType, Double>> typeWeights = new ArrayList<>();
        typeWeights.add(new Pair<>(VoterType.PRAGMATIC, VoterBehaviorConfig.PROB_PRAGMATIC));
        typeWeights.add(new Pair<>(VoterType.IDEOLOGICAL, VoterBehaviorConfig.PROB_IDEOLOGICAL));
        typeWeights.add(new Pair<>(VoterType.RATIONAL_CHOICE, VoterBehaviorConfig.PROB_RATIONAL_CHOICE));
        typeWeights.add(new Pair<>(VoterType.AFFECTIVE, VoterBehaviorConfig.PROB_AFFECTIVE));
        typeWeights.add(new Pair<>(VoterType.HEURISTIC, VoterBehaviorConfig.PROB_HEURISTIC));

        double remainder = 1.0 - typeWeights.stream().mapToDouble(Pair::getSecond).sum();
        typeWeights.add(new Pair<>(VoterType.POLITIKFERN, Math.max(0, remainder)));

        this.typeDistribution = new EnumeratedDistribution<>(randomGenerator, typeWeights);
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

    public RandomGenerator getRandomGenerator() {
        return randomGenerator;
    }

    public VoterType sampleVoterType() {
        return typeDistribution.sample();
    }
}
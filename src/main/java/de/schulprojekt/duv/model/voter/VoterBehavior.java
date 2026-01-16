package de.schulprojekt.duv.model.voter;

import de.schulprojekt.duv.model.core.SimulationParameters;
import de.schulprojekt.duv.model.party.Party;
import de.schulprojekt.duv.model.scandal.ScandalImpactCalculator;
import de.schulprojekt.duv.util.SimulationConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * Controls the behavior and decision-making of the voter population.
 * @author Nico Hoffmann
 * @version 1.1
 */
public class VoterBehavior {

    // ========================================
    // Static Variables
    // ========================================

    // Maximum voter opinion drift per tick (0-100 scale)
    private static final double OPINION_DRIFT_FACTOR = 0.25;
    // Divisor to dampen loyalty effect on switching probability (higher = less impact)
    private static final double LOYALTY_DAMPING_FACTOR = 180.0;
    // Maximum probability a voter will consider switching parties
    private static final double MAX_SWITCH_PROBABILITY = 0.65;
    // Multiplier for undecided voters' mobility (they switch more easily)
    private static final double UNDECIDED_MOBILITY_BONUS = 1.3;
    // Probability a dissatisfied voter becomes undecided instead of choosing another party
    private static final double RESIGNATION_PROBABILITY = 0.15;
    // Divisor converting scandal penalty to switch probability increase
    private static final double PENALTY_PRESSURE_DIVISOR = 1800.0;
    // Scandal pressure threshold triggering panic-mode party switching
    private static final double DISASTER_FLIGHT_THRESHOLD = 25.0;
    // Weight multiplier for permanent scandal damage in decision calculation
    private static final double PERMANENT_DAMAGE_WEIGHT = 1.5;
    // Base attractiveness score before distance penalty (0-100 scale)
    private static final double DISTANCE_SCORE_BASE = 40.0;
    // Sensitivity factor for political distance penalty (higher = distance matters more)
    private static final double DISTANCE_SENSITIVITY = 0.04;
    // Random noise amplitude added to party selection scores
    private static final double DECISION_NOISE_FACTOR = 12.0;
    // Maximum change in global trend (Zeitgeist) per tick
    private static final double ZEITGEIST_DRIFT_STRENGTH = 0.15;
    // Maximum absolute value for global political trend
    private static final double ZEITGEIST_MAX_AMPLITUDE = 8.0;

    // ========================================
    // Instance Variables
    // ========================================

    // Volatile ensures visibility across parallel streams in processVoterDecisions()
    private volatile double currentZeitgeist;

    // ========================================
    // Constructors
    // ========================================

    public VoterBehavior() {
        this.currentZeitgeist = (ThreadLocalRandom.current().nextDouble() - 0.5) * 2.0;
    }

    // ========================================
    // Business Logic Methods
    // ========================================

    public List<VoterTransition> processVoterDecisions(
            VoterPopulation population,
            List<Party> parties,
            SimulationParameters params,
            double[] acutePressures,
            ScandalImpactCalculator impactCalculator
    ) {
        ConcurrentLinkedQueue<VoterTransition> visualTransitions = new ConcurrentLinkedQueue<>();
        int partyCount = parties.size();

        updateZeitgeist();

        AtomicInteger[] partyDeltas = initDeltas(partyCount);
        PartyCalculationCache cache = createPartyCache(parties, params);

        final double activeZeitgeist = this.currentZeitgeist;

        IntStream.range(0, population.size()).parallel().forEach(i -> {
            ThreadLocalRandom rnd = ThreadLocalRandom.current();

            applyOpinionDrift(population, i, rnd, activeZeitgeist);

            int currentIdx = population.getPartyIndex(i);
            if (currentIdx >= partyCount) {
                currentIdx = 0;
                population.setPartyIndex(i, (byte) 0);
            }

            double totalPenalty = calculatePenalty(currentIdx, acutePressures, impactCalculator);
            double switchProb = calculateSwitchProbability(population, i, params, totalPenalty, currentIdx);

            if (rnd.nextDouble() < switchProb) {
                int targetIdx = findBestTargetParty(
                        population.getPosition(i),
                        population.getMediaInfluence(i),
                        currentIdx,
                        partyCount,
                        totalPenalty,
                        cache,
                        acutePressures,
                        impactCalculator,
                        rnd
                );

                if (targetIdx != currentIdx) {
                    population.setPartyIndex(i, (byte) targetIdx);
                    partyDeltas[currentIdx].decrementAndGet();
                    partyDeltas[targetIdx].incrementAndGet();

                    if (rnd.nextDouble() < SimulationConfig.VISUALIZATION_SAMPLE_RATE) {
                        visualTransitions.add(new VoterTransition(parties.get(currentIdx), parties.get(targetIdx)));
                    }
                }
            }
        });

        applyPopulationChanges(parties, partyDeltas);
        return new ArrayList<>(visualTransitions);
    }

    // ========================================
    // Utility Methods
    // ========================================

    private void updateZeitgeist() {
        double nextZeitgeist = this.currentZeitgeist;
        double change = (ThreadLocalRandom.current().nextDouble() - 0.5) * ZEITGEIST_DRIFT_STRENGTH;
        nextZeitgeist += change;
        if (nextZeitgeist > ZEITGEIST_MAX_AMPLITUDE) {
            nextZeitgeist = ZEITGEIST_MAX_AMPLITUDE;
        } else if (nextZeitgeist < -ZEITGEIST_MAX_AMPLITUDE) {
            nextZeitgeist = -ZEITGEIST_MAX_AMPLITUDE;
        }
        this.currentZeitgeist = nextZeitgeist;
    }

    private void applyOpinionDrift(VoterPopulation population, int index, ThreadLocalRandom rnd, double globalTrend) {
        double individualDrift = (rnd.nextDouble() - 0.5) * OPINION_DRIFT_FACTOR;
        double totalDrift = individualDrift + (globalTrend * 0.1);

        float newPos = (float) (population.getPosition(index) + totalDrift);
        if (newPos < 0) newPos = 0;
        if (newPos > 100) newPos = 100;
        population.setPosition(index, newPos);
    }

    private double calculatePenalty(int partyIdx, double[] acutePressures, ScandalImpactCalculator calc) {
        if (partyIdx <= 0) return 0.0;
        return acutePressures[partyIdx] + calc.getPermanentDamage(partyIdx);
    }

    /**
     * Calculates the probability of a voter switching their current party.
     * @param pop voter population
     * @param idx voter index
     * @param params simulation settings
     * @param penalty current scandal pressure
     * @param currentIdx index of the voter's current party
     * @return calculated switch probability
     */
    private double calculateSwitchProbability(VoterPopulation pop, int idx, SimulationParameters params, double penalty, int currentIdx) {
        double baseMobility = params.volatilityRate() / 100.0;

        double switchProb = baseMobility *
                (1.0 - pop.getLoyalty(idx) / LOYALTY_DAMPING_FACTOR) *
                pop.getMediaInfluence(idx);

        if (penalty > 0) {
            switchProb += (penalty / PENALTY_PRESSURE_DIVISOR);
        }

        if (currentIdx == 0) {
            switchProb *= UNDECIDED_MOBILITY_BONUS;
        }

        return Math.min(switchProb, MAX_SWITCH_PROBABILITY);
    }

    private int findBestTargetParty(
            float voterPos,
            float mediaInfluence,
            int currentIdx,
            int pSize,
            double currentPenalty,
            PartyCalculationCache cache,
            double[] acutePressures,
            ScandalImpactCalculator impactCalc,
            ThreadLocalRandom rnd
    ) {
        boolean isPanicMode = currentPenalty > DISASTER_FLIGHT_THRESHOLD;
        if (!isPanicMode && currentIdx != 0 && rnd.nextDouble() < RESIGNATION_PROBABILITY) {
            return 0;
        }

        double bestScore = -Double.MAX_VALUE;
        int targetIdx = currentIdx;
        double campaignEffectiveness = rnd.nextDouble() * cache.uniformRange;

        for (int pIdx = 1; pIdx < pSize; pIdx++) {
            if (pIdx == currentIdx) continue;

            double dist = Math.abs(voterPos - cache.positions[pIdx]);
            double distScore = DISTANCE_SCORE_BASE / (1.0 + (dist * DISTANCE_SENSITIVITY));

            double budgetScore = cache.budgetScores[pIdx] * campaignEffectiveness * mediaInfluence * cache.globalMediaFactor;
            double score = distScore + (budgetScore * cache.dailyMomentum[pIdx]);

            double targetPenalty = acutePressures[pIdx] + (impactCalc.getPermanentDamage(pIdx) * PERMANENT_DAMAGE_WEIGHT);
            score -= targetPenalty;

            double noise = (rnd.nextDouble() - 0.5) * DECISION_NOISE_FACTOR * cache.uniformRange;

            if ((score + noise) > bestScore) {
                bestScore = score + noise;
                targetIdx = pIdx;
            }
        }

        return (bestScore < 0) ? 0 : targetIdx;
    }

    private AtomicInteger[] initDeltas(int size) {
        AtomicInteger[] deltas = new AtomicInteger[size];
        for (int i = 0; i < size; i++) deltas[i] = new AtomicInteger(0);
        return deltas;
    }

    private void applyPopulationChanges(List<Party> parties, AtomicInteger[] deltas) {
        for (int i = 0; i < parties.size(); i++) {
            int delta = deltas[i].get();
            if (delta != 0) {
                Party p = parties.get(i);
                p.setCurrentSupporterCount(Math.max(0, p.getCurrentSupporterCount() + delta));
            }
        }
    }

    private PartyCalculationCache createPartyCache(List<Party> parties, SimulationParameters params) {
        int size = parties.size();
        double[] positions = new double[size];
        double[] budgetScores = new double[size];
        double[] momentum = new double[size];

        for (int k = 0; k < size; k++) {
            Party p = parties.get(k);
            positions[k] = p.getPoliticalPosition();
            budgetScores[k] = (p.getCampaignBudget() / SimulationConfig.CAMPAIGN_BUDGET_FACTOR) * 12.0;
            momentum[k] = 0.8 + (ThreadLocalRandom.current().nextDouble() * 0.4);
        }

        return new PartyCalculationCache(
                positions,
                budgetScores,
                momentum,
                params.chaosFactor(),
                params.mediaInfluence() / 100.0
        );
    }

    private record PartyCalculationCache(
            double[] positions,
            double[] budgetScores,
            double[] dailyMomentum,
            double uniformRange,
            double globalMediaFactor
    ) {}
}
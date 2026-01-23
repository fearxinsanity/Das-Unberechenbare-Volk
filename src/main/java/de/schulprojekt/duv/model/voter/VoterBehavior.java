package de.schulprojekt.duv.model.voter;

import de.schulprojekt.duv.model.calculation.PartyCalculationCache;
import de.schulprojekt.duv.model.calculation.VoterDecisionContext;
import de.schulprojekt.duv.model.calculation.PartyEvaluationResult;
import de.schulprojekt.duv.model.core.SimulationParameters;
import de.schulprojekt.duv.model.dto.VoterTransition;
import de.schulprojekt.duv.model.party.Party;
import de.schulprojekt.duv.model.random.DistributionProvider;
import de.schulprojekt.duv.model.scandal.ScandalImpactCalculator;
import de.schulprojekt.duv.util.config.SimulationConfig;
import de.schulprojekt.duv.util.config.VoterBehaviorConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * Controls the behavior and decision-making of the voter population.
 * Updated to support high-performance SoA access and separated logic.
 * @author Nico Hoffmann
 * @version 1.6
 */
public class VoterBehavior {

    /**
     * Initialisiert die Population mit Werten (ehemals in VoterPopulation).
     */
    public void initializePopulation(VoterPopulation pop, int totalVoters, int partyCount, DistributionProvider dist) {
        pop.allocate(totalVoters);
        IntStream.range(0, totalVoters).parallel().forEach(i -> {
            ThreadLocalRandom rnd = ThreadLocalRandom.current();

            boolean isUndecided = rnd.nextDouble() < VoterBehaviorConfig.UNDECIDED_RATIO;
            int assignedParty = (!isUndecided && partyCount > 1) ? 1 + rnd.nextInt(partyCount - 1) : 0;
            pop.setPartyIndexRaw(i, (byte) assignedParty);

            pop.setVoterTypeRaw(i, (byte) dist.sampleVoterType().ordinal());
            pop.setLoyaltyRaw(i, (float) dist.sampleLoyalty());

            double rawPos = VoterBehaviorConfig.POS_MEAN + (rnd.nextGaussian() * VoterBehaviorConfig.POS_STD_DEV);
            pop.setPositionRaw(i, (float) Math.max(0, Math.min(100, rawPos)));
            pop.setMediaInfluenceRaw(i, (float) Math.pow(rnd.nextDouble(), VoterBehaviorConfig.MEDIA_INFLUENCE_EXPONENT));
        });
    }

    /**
     * Berechnet die dynamische Entwicklung der Wählerattribute (ehemals in VoterPopulation).
     */
    public void evolvePopulation(VoterPopulation pop, SimulationParameters params) {
        double volatilityFactor = params.volatilityRate() / 50.0;

        IntStream.range(0, pop.size()).parallel().forEach(i -> {
            ThreadLocalRandom rnd = ThreadLocalRandom.current();

            float currentLoyalty = pop.getLoyaltyRaw(i);
            float deltaLoyalty = (float) ((rnd.nextDouble() - 0.5) * VoterBehaviorConfig.LOYALTY_FLUCTUATION * volatilityFactor);
            pop.setLoyaltyRaw(i, Math.max(0, Math.min(100, currentLoyalty + deltaLoyalty)));

            float currentMedia = pop.getMediaInfluenceRaw(i);
            float deltaMedia = (float) ((rnd.nextDouble() - 0.5) * VoterBehaviorConfig.MEDIA_INFLUENCE_DRIFT * volatilityFactor);
            pop.setMediaInfluenceRaw(i, Math.max(0.0f, Math.min(1.0f, currentMedia + deltaMedia)));

            if (rnd.nextDouble() < (VoterBehaviorConfig.TYPE_CHANGE_PROBABILITY * volatilityFactor)) {
                pop.setVoterTypeRaw(i, (byte) rnd.nextInt(VoterType.values().length));
            }
        });
    }

    public List<VoterTransition> processVoterDecisions(
            VoterPopulation population,
            List<Party> parties,
            SimulationParameters params,
            double[] acutePressures,
            ScandalImpactCalculator impactCalculator,
            double activeZeitgeist
    ) {
        ConcurrentLinkedQueue<VoterTransition> visualTransitions = new ConcurrentLinkedQueue<>();
        int partyCount = parties.size();

        AtomicInteger[] partyDeltas = initDeltas(partyCount);
        PartyCalculationCache cache = createPartyCache(parties, params);

        IntStream.range(0, population.size()).parallel().forEach(i -> {
            Random rnd = ThreadLocalRandom.current();

            applyOpinionDrift(population, i, rnd, activeZeitgeist);

            int currentIdx = population.getPartyIndexRaw(i);
            if (currentIdx >= partyCount) {
                currentIdx = 0;
                population.setPartyIndexRaw(i, (byte) 0);
            }

            VoterType voterType = VoterType.values()[population.getVoterTypeRaw(i)];
            double totalPenalty = calculatePenalty(currentIdx, acutePressures, impactCalculator);

            VoterDecisionContext context = new VoterDecisionContext(
                    population.getPositionRaw(i),
                    population.getLoyaltyRaw(i),
                    population.getMediaInfluenceRaw(i),
                    voterType,
                    currentIdx,
                    totalPenalty
            );

            double switchProb = calculateSwitchProbability(context, params);

            if (rnd.nextDouble() < switchProb) {
                int targetIdx = findBestTargetParty(context, partyCount, cache, acutePressures, impactCalculator, rnd);

                if (targetIdx != currentIdx) {
                    population.setPartyIndexRaw(i, (byte) targetIdx);
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

    private void applyOpinionDrift(VoterPopulation population, int index, Random rnd, double globalTrend) {
        double individualDrift = (rnd.nextDouble() - 0.5) * VoterBehaviorConfig.OPINION_DRIFT_FACTOR;
        double totalDrift = individualDrift + (globalTrend * VoterBehaviorConfig.GLOBAL_TREND_WEIGHT);

        float newPos = (float) (population.getPositionRaw(index) + totalDrift);
        population.setPositionRaw(index, Math.max(0, Math.min(100, newPos)));
    }

    private double calculatePenalty(int partyIdx, double[] acutePressures, ScandalImpactCalculator calc) {
        if (partyIdx <= 0) return 0.0;
        return acutePressures[partyIdx] + calc.getPermanentDamage(partyIdx);
    }

    private double calculateSwitchProbability(VoterDecisionContext context, SimulationParameters params) {
        double baseMobility = params.volatilityRate() / 100.0;

        double switchProb = baseMobility *
                context.voterType().getLoyaltyModifier() *
                (1.0 - context.loyalty() / VoterBehaviorConfig.LOYALTY_DAMPING_FACTOR) *
                context.mediaInfluence() *
                context.voterType().getMediaModifier();

        if (context.currentPenalty() > 0) {
            double acuteScandalImpact = Math.min(context.currentPenalty() *
                            VoterBehaviorConfig.ACUTE_SCANDAL_MULTIPLIER,
                    VoterBehaviorConfig.MAX_ACUTE_SCANDAL_BOOST);
            switchProb += acuteScandalImpact;
        }

        if (context.currentPartyIndex() == 0) {
            switchProb *= VoterBehaviorConfig.UNDECIDED_MOBILITY_BONUS;
        }

        return Math.min(switchProb, VoterBehaviorConfig.MAX_SWITCH_PROBABILITY);
    }

    private int findBestTargetParty(VoterDecisionContext context, int partyCount, PartyCalculationCache cache,
                                    double[] acutePressures, ScandalImpactCalculator impactCalc, Random rnd) {
        boolean isPanicMode = context.currentPenalty() > VoterBehaviorConfig.DISASTER_FLIGHT_THRESHOLD;

        if (!isPanicMode && context.currentPartyIndex() != 0 &&
                rnd.nextDouble() < VoterBehaviorConfig.RESIGNATION_PROBABILITY) {
            return 0;
        }

        double bestScore = -Double.MAX_VALUE;
        int targetIdx = context.currentPartyIndex();
        double campaignEffectiveness = rnd.nextDouble() * cache.uniformRange();

        for (int pIdx = 1; pIdx < partyCount; pIdx++) {
            if (pIdx == context.currentPartyIndex()) continue;

            PartyEvaluationResult evaluation = evaluateParty(pIdx, context, cache, acutePressures, impactCalc, campaignEffectiveness, rnd);

            if (evaluation.finalScore() > bestScore) {
                bestScore = evaluation.finalScore();
                targetIdx = pIdx;
            }
        }
        return (bestScore < 0) ? 0 : targetIdx;
    }

    private PartyEvaluationResult evaluateParty(int partyIdx, VoterDecisionContext context, PartyCalculationCache cache,
                                                double[] acutePressures, ScandalImpactCalculator impactCalc, double campaignEffectiveness, Random rnd) {
        double dist = Math.abs(context.position() - cache.positions()[partyIdx]);
        double typeSensitivity = VoterBehaviorConfig.DISTANCE_SENSITIVITY * context.voterType().getDistanceSensitivity();
        double distScore = VoterBehaviorConfig.DISTANCE_SCORE_BASE / (1.0 + (dist * typeSensitivity));

        double budgetScore = cache.budgetScores()[partyIdx] * campaignEffectiveness * context.mediaInfluence() * context.voterType().getMediaModifier() * cache.globalMediaFactor();

        double score = distScore + (budgetScore * cache.dailyMomentum()[partyIdx]);
        score -= acutePressures[partyIdx] * VoterBehaviorConfig.ACUTE_SCANDAL_PENALTY_WEIGHT;
        score -= impactCalc.getPermanentDamage(partyIdx) * VoterBehaviorConfig.PERMANENT_SCANDAL_PENALTY_WEIGHT;

        double noise = (rnd.nextDouble() - 0.5) * VoterBehaviorConfig.DECISION_NOISE_FACTOR * cache.uniformRange();
        return new PartyEvaluationResult(partyIdx, distScore, budgetScore, 0, score + noise);
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
        Random rnd = new Random();
        int size = parties.size();
        double[] positions = new double[size];
        double[] budgetScores = new double[size];
        double[] momentum = new double[size];

        for (int k = 0; k < size; k++) {
            Party p = parties.get(k);
            positions[k] = p.getPoliticalPosition();
            budgetScores[k] = (p.getCampaignBudget() / SimulationConfig.CAMPAIGN_BUDGET_FACTOR) * VoterBehaviorConfig.BUDGET_SCORE_MULTIPLIER;
            momentum[k] = VoterBehaviorConfig.MOMENTUM_BASE + (rnd.nextDouble() * VoterBehaviorConfig.MOMENTUM_VARIANCE);
        }

        return new PartyCalculationCache(positions, budgetScores, momentum, params.chaosFactor(), params.mediaInfluence() / 100.0);
    }
}
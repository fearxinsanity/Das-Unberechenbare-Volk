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

public class VoterBehavior {

    /**
     * @param acutePressures Array mit NUR akutem Druck (von ScandalImpactCalculator)
     */
    public List<VoterTransition> processVoterDecisions(
            VoterPopulation population,
            List<Party> parties,
            SimulationParameters params,
            double[] acutePressures,
            ScandalImpactCalculator impactCalculator
    ) {
        ConcurrentLinkedQueue<VoterTransition> visualTransitions = new ConcurrentLinkedQueue<>();
        int pSize = parties.size();

        AtomicInteger[] partyDeltas = new AtomicInteger[pSize];
        for(int i=0; i<pSize; i++) partyDeltas[i] = new AtomicInteger(0);

        // Pre-Calculation
        double baseMobility = params.getBaseMobilityRate() / 100.0;
        double globalMedia = params.getGlobalMediaInfluence() / 100.0;
        double uniformRange = params.getUniformRandomRange();

        // Caching für Performance
        double[] partyPos = new double[pSize];
        double[] partyBudgetScores = new double[pSize];
        double[] partyDailyMomentum = new double[pSize];

        for(int k=0; k < pSize; k++) {
            Party p = parties.get(k);
            partyPos[k] = p.getPoliticalPosition();
            partyBudgetScores[k] = (p.getCampaignBudget() / SimulationConfig.CAMPAIGN_BUDGET_FACTOR) * 12.0;
            partyDailyMomentum[k] = 0.95 + (ThreadLocalRandom.current().nextDouble() * 0.1);
        }

        // --- PARALLEL LOOP ---
        IntStream.range(0, population.size()).parallel().forEach(i -> {
            ThreadLocalRandom rnd = ThreadLocalRandom.current();

            // 1. Meinungsdrift
            double drift = (rnd.nextDouble() - 0.5) * 0.2;
            float newPos = (float) (population.getPosition(i) + drift);
            if (newPos < 0) newPos = 0; if (newPos > 100) newPos = 100;
            population.setPosition(i, newPos);

            int currentIdx = population.getPartyIndex(i);
            // Safety check
            if (currentIdx >= pSize) { currentIdx = 0; population.setPartyIndex(i, (byte)0); }

            // 2. Penalty Berechnung (Original Logik: Summe aus Akut + Perm)
            double totalPenalty = 0;
            if (currentIdx > 0) {
                totalPenalty = acutePressures[currentIdx] + impactCalculator.getPermanentDamage(currentIdx);
            }

            // 3. Wechselwahrscheinlichkeit
            double switchProb = baseMobility * (1.0 - population.getLoyalty(i) / 200.0) * population.getMediaInfluence(i);

            // Penalty erhöht Wechseldruck (Faktor 600.0 aus Original)
            if (totalPenalty > 0) switchProb += (totalPenalty / 600.0);

            if (currentIdx == 0) switchProb *= 1.2;
            if (switchProb > 0.60) switchProb = 0.60; // Cap

            // 4. Entscheidung
            if (rnd.nextDouble() < switchProb) {
                int targetIdx = currentIdx;

                // Flucht bei extremem Skandal (Original Schwelle: 12.0)
                boolean fleeingFromDisaster = (totalPenalty > 12.0);

                if (!fleeingFromDisaster && currentIdx != 0 && rnd.nextDouble() < 0.15) {
                    targetIdx = 0; // Zurück zu Unsicher
                } else {
                    double bestScore = -Double.MAX_VALUE;
                    double campaignEffectiveness = rnd.nextDouble() * uniformRange;

                    for (int pIdx = 1; pIdx < pSize; pIdx++) {
                        if (pIdx == currentIdx) continue;

                        // Distanz Score
                        double dist = Math.abs(population.getPosition(i) - partyPos[pIdx]);
                        double distScore = 40.0 / (1.0 + (dist * 0.04));

                        // Budget Score
                        double budgetScore = partyBudgetScores[pIdx] * campaignEffectiveness * population.getMediaInfluence(i) * globalMedia;
                        double score = distScore + (budgetScore * partyDailyMomentum[pIdx]);

                        // Penalty Score Abzug (Original Logik: Akut + Perm * 1.5)
                        double penaltyScore = acutePressures[pIdx] + (impactCalculator.getPermanentDamage(pIdx) * 1.5);
                        score -= penaltyScore;

                        double noise = (rnd.nextDouble() - 0.5) * 10.0 * uniformRange;

                        if ((score + noise) > bestScore) {
                            bestScore = score + noise;
                            targetIdx = pIdx;
                        }
                    }
                    if (bestScore < 0) targetIdx = 0;
                }

                // 5. Durchführung
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

        // 6. Aggregation
        for (int i = 0; i < pSize; i++) {
            int delta = partyDeltas[i].get();
            if (delta != 0) {
                Party p = parties.get(i);
                p.setCurrentSupporterCount(Math.max(0, p.getCurrentSupporterCount() + delta));
            }
        }

        return new ArrayList<>(visualTransitions);
    }
}
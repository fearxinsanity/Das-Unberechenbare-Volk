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

    // --- SIMULATIONS-KONSTANTEN (Tuning) ---
    // Bestimmen, wie stark Meinungen driften
    private static final double OPINION_DRIFT_FACTOR = 0.2;

    // Beeinflusst, wie stark Loyalität den Wechsel hemmt (höher = weniger Wechsel)
    private static final double LOYALTY_DAMPING_FACTOR = 200.0;

    // Wie stark Skandale (Penalty) den Wechseldruck erhöhen (niedriger = empfindlicher)
    private static final double PENALTY_PRESSURE_DIVISOR = 600.0;

    // Bonus-Wahrscheinlichkeit, wenn man aktuell "Unsicher" ist (schnellerer Wechsel weg von Unsicher)
    private static final double UNDECIDED_MOBILITY_BONUS = 1.2;

    // Maximale Wechselwahrscheinlichkeit pro Tick (Cap)
    private static final double MAX_SWITCH_PROBABILITY = 0.60;

    // Ab welchem Druck ("Skandal-Wert") fliehen Wähler panisch (egal wohin)?
    private static final double DISASTER_FLIGHT_THRESHOLD = 12.0;

    // Chance, frustriert ins Lager der Nichtwähler ("Unsicher") zurückzukehren
    private static final double RESIGNATION_PROBABILITY = 0.15;

    // Gewichtung der politischen Distanz (Score-Berechnung)
    private static final double DISTANCE_SCORE_BASE = 40.0;
    private static final double DISTANCE_SENSITIVITY = 0.04;

    // Gewichtung permanenter Skandalschäden im Vergleich zu akuten
    private static final double PERMANENT_DAMAGE_WEIGHT = 1.5;

    // Zufallsrauschen bei der Entscheidungsfindung
    private static final double DECISION_NOISE_FACTOR = 10.0;

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
            // Budget-Faktor 12.0 ist ein Skalierungswert für die GUI/Balance
            partyBudgetScores[k] = (p.getCampaignBudget() / SimulationConfig.CAMPAIGN_BUDGET_FACTOR) * 12.0;
            partyDailyMomentum[k] = 0.95 + (ThreadLocalRandom.current().nextDouble() * 0.1);
        }

        // --- PARALLEL LOOP ---
        IntStream.range(0, population.size()).parallel().forEach(i -> {
            ThreadLocalRandom rnd = ThreadLocalRandom.current();

            // 1. Meinungsdrift (Zufällige leichte Änderung der politischen Einstellung)
            double drift = (rnd.nextDouble() - 0.5) * OPINION_DRIFT_FACTOR;
            float newPos = (float) (population.getPosition(i) + drift);
            if (newPos < 0) newPos = 0; if (newPos > 100) newPos = 100;
            population.setPosition(i, newPos);

            int currentIdx = population.getPartyIndex(i);
            if (currentIdx >= pSize) { currentIdx = 0; population.setPartyIndex(i, (byte)0); }

            // 2. Penalty Berechnung (Druck durch Skandale)
            double totalPenalty = 0;
            if (currentIdx > 0) {
                totalPenalty = acutePressures[currentIdx] + impactCalculator.getPermanentDamage(currentIdx);
            }

            // 3. Wechselwahrscheinlichkeit berechnen
            // Basis: Mobilität * (1 - Loyalität%) * Medieneinfluss
            double switchProb = baseMobility * (1.0 - population.getLoyalty(i) / LOYALTY_DAMPING_FACTOR) * population.getMediaInfluence(i);

            // Skandale erhöhen den Wechseldruck
            if (totalPenalty > 0) {
                switchProb += (totalPenalty / PENALTY_PRESSURE_DIVISOR);
            }

            // Wer unentschlossen ist, wechselt leichter zu einer Partei
            if (currentIdx == 0) switchProb *= UNDECIDED_MOBILITY_BONUS;

            // Obergrenze (Cap), damit das System nicht chaotisch explodiert
            if (switchProb > MAX_SWITCH_PROBABILITY) switchProb = MAX_SWITCH_PROBABILITY;

            // 4. Entscheidung treffen
            if (rnd.nextDouble() < switchProb) {
                int targetIdx = currentIdx;

                // Sonderfall: Flucht vor Katastrophe (Panikreaktion)
                boolean fleeingFromDisaster = (totalPenalty > DISASTER_FLIGHT_THRESHOLD);

                // Chance, frustriert ins "Unsicher"-Lager zu wechseln (Nichtwähler)
                if (!fleeingFromDisaster && currentIdx != 0 && rnd.nextDouble() < RESIGNATION_PROBABILITY) {
                    targetIdx = 0;
                } else {
                    // Suche nach der besten Alternative
                    double bestScore = -Double.MAX_VALUE;
                    double campaignEffectiveness = rnd.nextDouble() * uniformRange;

                    for (int pIdx = 1; pIdx < pSize; pIdx++) {
                        if (pIdx == currentIdx) continue;

                        // A: Politische Nähe (Distanz-Score)
                        double dist = Math.abs(population.getPosition(i) - partyPos[pIdx]);
                        double distScore = DISTANCE_SCORE_BASE / (1.0 + (dist * DISTANCE_SENSITIVITY));

                        // B: Wahlkampf & Budget
                        double budgetScore = partyBudgetScores[pIdx] * campaignEffectiveness * population.getMediaInfluence(i) * globalMedia;

                        // Gesamt-Score
                        double score = distScore + (budgetScore * partyDailyMomentum[pIdx]);

                        // C: Abzug für Skandale der Zielpartei (Penalty Score)
                        // Hier wiegen permanente Schäden (Ruf) schwerer als akute!
                        double penaltyScore = acutePressures[pIdx] + (impactCalculator.getPermanentDamage(pIdx) * PERMANENT_DAMAGE_WEIGHT);
                        score -= penaltyScore;

                        // Zufallsrauschen (Irrationalität des Wählers)
                        double noise = (rnd.nextDouble() - 0.5) * DECISION_NOISE_FACTOR * uniformRange;

                        if ((score + noise) > bestScore) {
                            bestScore = score + noise;
                            targetIdx = pIdx;
                        }
                    }
                    // Wenn selbst die beste Partei einen negativen Score hat, werde Nichtwähler
                    if (bestScore < 0) targetIdx = 0;
                }

                // 5. Durchführung des Wechsels
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

        // 6. Aggregation der Ergebnisse
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
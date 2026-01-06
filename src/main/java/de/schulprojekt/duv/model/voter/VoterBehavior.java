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
 * Steuert das Verhalten der Wählerpopulation.
 * Beinhaltet Meinungsdrift, Wechselwahrscheinlichkeiten und Parteiwahl.
 */
public class VoterBehavior {

    // --- Konstanten: Mobilität & Wechsel ---
    private static final double OPINION_DRIFT_FACTOR = 0.2;
    private static final double LOYALTY_DAMPING_FACTOR = 200.0;
    private static final double MAX_SWITCH_PROBABILITY = 0.60;
    private static final double UNDECIDED_MOBILITY_BONUS = 1.2;
    private static final double RESIGNATION_PROBABILITY = 0.15; // Chance, Nichtwähler zu werden

    // --- Konstanten: Skandale & Druck ---
    private static final double PENALTY_PRESSURE_DIVISOR = 600.0;
    private static final double DISASTER_FLIGHT_THRESHOLD = 12.0; // Panik-Grenze
    private static final double PERMANENT_DAMAGE_WEIGHT = 1.5;

    // --- Konstanten: Scoring (Parteiwahl) ---
    private static final double DISTANCE_SCORE_BASE = 40.0;
    private static final double DISTANCE_SENSITIVITY = 0.04;
    private static final double DECISION_NOISE_FACTOR = 10.0;

    // --- Konstruktor ---
    public VoterBehavior() {
        // Stateless Service - kein expliziter Konstruktor nötig
    }

    // --- Business Logik ---

    /**
     * Hauptmethode zur Berechnung der Wählerwanderung in einem Zeitschritt.
     * Nutzt Parallel-Streams für Performance.
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

        // 1. Vorbereitung (Thread-Safe Counter & Caching)
        AtomicInteger[] partyDeltas = initDeltas(pSize);
        PartyCalculationCache cache = createPartyCache(parties, params);

        // 2. Parallele Verarbeitung der Population
        IntStream.range(0, population.size()).parallel().forEach(i -> {
            ThreadLocalRandom rnd = ThreadLocalRandom.current();

            // A. Meinungsdrift anwenden
            applyOpinionDrift(population, i, rnd);

            int currentIdx = population.getPartyIndex(i);
            // Korrektur falls Index Out-of-Bounds (Sicherheitsnetz)
            if (currentIdx >= pSize) {
                currentIdx = 0;
                population.setPartyIndex(i, (byte) 0);
            }

            // B. Skandaldruck berechnen
            double totalPenalty = calculatePenalty(currentIdx, acutePressures, impactCalculator);

            // C. Soll gewechselt werden? (Switch Probability)
            double switchProb = calculateSwitchProbability(population, i, params, totalPenalty, currentIdx);

            if (rnd.nextDouble() < switchProb) {
                // D. Wohin wird gewechselt? (Target Selection)
                int targetIdx = findBestTargetParty(
                        population.getPosition(i),
                        population.getMediaInfluence(i),
                        currentIdx,
                        pSize,
                        totalPenalty,
                        cache,
                        acutePressures,
                        impactCalculator,
                        rnd
                );

                // E. Wechsel durchführen
                if (targetIdx != currentIdx) {
                    population.setPartyIndex(i, (byte) targetIdx);
                    partyDeltas[currentIdx].decrementAndGet();
                    partyDeltas[targetIdx].incrementAndGet();

                    // Visualisierung sampeln
                    if (rnd.nextDouble() < SimulationConfig.VISUALIZATION_SAMPLE_RATE) {
                        visualTransitions.add(new VoterTransition(parties.get(currentIdx), parties.get(targetIdx)));
                    }
                }
            }
        });

        // 3. Ergebnisse aggregieren
        applyPopulationChanges(parties, partyDeltas);

        return new ArrayList<>(visualTransitions);
    }

    // --- Private Hilfsmethoden: Logik-Details ---

    private void applyOpinionDrift(VoterPopulation population, int index, ThreadLocalRandom rnd) {
        double drift = (rnd.nextDouble() - 0.5) * OPINION_DRIFT_FACTOR;
        float newPos = (float) (population.getPosition(index) + drift);
        if (newPos < 0) newPos = 0;
        if (newPos > 100) newPos = 100;
        population.setPosition(index, newPos);
    }

    private double calculatePenalty(int partyIdx, double[] acutePressures, ScandalImpactCalculator calc) {
        if (partyIdx <= 0) return 0.0;
        return acutePressures[partyIdx] + calc.getPermanentDamage(partyIdx);
    }

    private double calculateSwitchProbability(VoterPopulation pop, int idx, SimulationParameters params, double penalty, int currentIdx) {
        double baseMobility = params.getBaseMobilityRate() / 100.0;

        // Basis: Mobilität * (1 - Loyalität%) * Medieneinfluss
        double switchProb = baseMobility *
                (1.0 - pop.getLoyalty(idx) / LOYALTY_DAMPING_FACTOR) *
                pop.getMediaInfluence(idx);

        // Skandale erhöhen Druck
        if (penalty > 0) {
            switchProb += (penalty / PENALTY_PRESSURE_DIVISOR);
        }

        // Unsichere Wähler wechseln leichter
        if (currentIdx == 0) {
            switchProb *= UNDECIDED_MOBILITY_BONUS;
        }

        return Math.min(switchProb, MAX_SWITCH_PROBABILITY);
    }

    /**
     * Kernlogik der Parteiwahl: Findet die Partei mit dem höchsten Score.
     */
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
        // 1. Panik-Check: Ist der Druck so hoch, dass der Wähler fliehen MUSS?
        boolean isPanicMode = currentPenalty > DISASTER_FLIGHT_THRESHOLD;
        if (!isPanicMode && currentIdx != 0 && rnd.nextDouble() < RESIGNATION_PROBABILITY) {
            return 0; // Zurück zu "Unsicher"
        }

        // Ab hier beginnt die Suche nach der besten Alternative (Passiert bei Panik IMMER)
        double bestScore = -Double.MAX_VALUE;
        int targetIdx = currentIdx;
        double campaignEffectiveness = rnd.nextDouble() * cache.uniformRange;

        // Alle Parteien bewerten (außer Unsicher=0, Start bei 1)
        for (int pIdx = 1; pIdx < pSize; pIdx++) {
            if (pIdx == currentIdx) continue;

            // 1. Politische Nähe
            double dist = Math.abs(voterPos - cache.positions[pIdx]);
            double distScore = DISTANCE_SCORE_BASE / (1.0 + (dist * DISTANCE_SENSITIVITY));

            // 2. Budget & Kampagne
            double budgetScore = cache.budgetScores[pIdx] * campaignEffectiveness * mediaInfluence * cache.globalMediaFactor;

            // Zwischensumme
            double score = distScore + (budgetScore * cache.dailyMomentum[pIdx]);

            // 3. Abzug für Skandale der Zielpartei
            double targetPenalty = acutePressures[pIdx] + (impactCalc.getPermanentDamage(pIdx) * PERMANENT_DAMAGE_WEIGHT);
            score -= targetPenalty;

            // 4. Zufallsrauschen (Irrationalität)
            double noise = (rnd.nextDouble() - 0.5) * DECISION_NOISE_FACTOR * cache.uniformRange;

            if ((score + noise) > bestScore) {
                bestScore = score + noise;
                targetIdx = pIdx;
            }
        }

        // Wenn alle Parteien unattraktiv sind (negativer Score), werde Nichtwähler
        if (bestScore < 0) {
            return 0;
        }

        return targetIdx;
    }

    // --- Private Hilfsmethoden: Setup & Aggregation ---

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
            momentum[k] = 0.95 + (ThreadLocalRandom.current().nextDouble() * 0.1);
        }

        return new PartyCalculationCache(
                positions,
                budgetScores,
                momentum,
                params.getUniformRandomRange(),
                params.getGlobalMediaInfluence() / 100.0
        );
    }

    /**
     * Interne Datenstruktur (Record) zum Bündeln von Read-Only Werten für den Loop.
     */
    private record PartyCalculationCache(
            double[] positions,
            double[] budgetScores,
            double[] dailyMomentum,
            double uniformRange,
            double globalMediaFactor
    ) {}
}
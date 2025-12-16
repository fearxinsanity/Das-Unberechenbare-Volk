package de.schulprojekt.duv.model.engine;

import de.schulprojekt.duv.model.entities.Party;
import de.schulprojekt.duv.model.entities.PartyTemplate;
import de.schulprojekt.duv.model.entities.Scandal;
import de.schulprojekt.duv.util.CSVLoader;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class SimulationEngine {

    // Arrays (Structure of Arrays)
    private byte[] voterPartyIndices;
    private float[] voterLoyalties;
    private float[] voterPositions;
    private float[] voterMediaInfluence;

    private final List<Party> partyList;
    private Party undecidedParty;
    private final CSVLoader csvLoader;
    private SimulationParameters parameters;
    private final Random random = new Random();

    // Verteilungen
    private NormalDistribution normalDistribution;
    private UniformRealDistribution uniformRealDistribution;
    private ExponentialDistribution exponentialDistribution;

    // Status
    private double timeUntilNextScandal;
    private int currentStep = 0;
    private final List<ScandalEvent> activeScandals = new ArrayList<>();
    private ScandalEvent lastScandal = null;

    public SimulationEngine(SimulationParameters params) {
        this.parameters = params;
        this.partyList = new ArrayList<>();
        this.csvLoader = new CSVLoader();
        initializeDistributions();
    }

    private void initializeDistributions() {
        this.normalDistribution = new NormalDistribution(parameters.getInitialLoyaltyMean(), SimulationConfig.DEFAULT_LOYALTY_STD_DEV);
        this.uniformRealDistribution = new UniformRealDistribution(0.0, parameters.getUniformRandomRange());
        double scandalProb = parameters.getScandalChance();
        double scandalLambda = Math.max(0.00001, scandalProb / 3000.0);
        this.exponentialDistribution = new ExponentialDistribution(1.0 / scandalLambda);
    }

    // --- WICHTIG: Die Methode zum Updaten der Parameter ---
    public void updateParameters(SimulationParameters newParams) {
        // Prüfen, ob strukturelle Änderungen vorliegen (Anzahl Parteien oder Wähler)
        boolean structuralChange = (newParams.getNumberOfParties() != parameters.getNumberOfParties()) ||
                (newParams.getTotalVoterCount() != parameters.getTotalVoterCount());

        this.parameters = newParams;
        initializeDistributions();

        // Wenn sich Parteien/Wähler geändert haben -> Welt neu erschaffen!
        if (structuralChange) {
            initializeSimulation();
        }
    }

    public void initializeSimulation() {
        partyList.clear();
        activeScandals.clear();
        currentStep = 0;

        // 1. Parteien initialisieren
        int partyCount = parameters.getNumberOfParties();
        undecidedParty = new Party(SimulationConfig.UNDECIDED_NAME, SimulationConfig.UNDECIDED_COLOR, SimulationConfig.UNDECIDED_POSITION, 0, 0);
        partyList.add(undecidedParty);

        List<PartyTemplate> templates = csvLoader.getRandomPartyTemplates(partyCount);
        for (int i = 0; i < partyCount; i++) {
            PartyTemplate template = templates.get(i);
            // Gleichmäßige Verteilung der Parteien auf dem Spektrum
            double pos = Math.max(5, Math.min(95, (100.0 / (partyCount + 1)) * (i + 1) + (random.nextDouble() - 0.5) * 10));
            double budget = 300000.0 + random.nextDouble() * 400000.0;
            partyList.add(template.toParty(pos, budget));
        }

        // 2. Wähler Arrays initialisieren
        int totalVoters = parameters.getTotalVoterCount();
        voterPartyIndices = new byte[totalVoters];
        voterLoyalties = new float[totalVoters];
        voterPositions = new float[totalVoters];
        voterMediaInfluence = new float[totalVoters];

        NormalDistribution posDist = new NormalDistribution(50.0, 25.0);

        IntStream.range(0, totalVoters).parallel().forEach(i -> {
            boolean isUndecided = Math.random() < 0.20;
            voterPartyIndices[i] = (byte) (isUndecided ? 0 : 1 + random.nextInt(partyCount));
            voterLoyalties[i] = (float) Math.max(0, Math.min(100, normalDistribution.sample()));
            voterPositions[i] = (float) Math.max(0, Math.min(100, posDist.sample()));
            voterMediaInfluence[i] = (float) Math.pow(random.nextDouble(), 0.7);
        });

        recalculatePartyCounts();
        scheduleNextScandal();
    }

    private void scheduleNextScandal() {
        this.timeUntilNextScandal = exponentialDistribution.sample();
    }

    private void recalculatePartyCounts() {
        // Reset counts
        for(Party p : partyList) p.setCurrentSupporterCount(0);

        // Count parallel friendly? Für Init reicht sequenziell oder synchronisiert
        int[] counts = new int[partyList.size()];
        for(byte idx : voterPartyIndices) {
            counts[idx]++;
        }
        for(int i=0; i<counts.length; i++) {
            partyList.get(i).setCurrentSupporterCount(counts[i]);
        }
    }

    public List<VoterTransition> runSimulationStep() {
        currentStep++;
        ConcurrentLinkedQueue<VoterTransition> visualTransitions = new ConcurrentLinkedQueue<>();

        if (voterPartyIndices == null || voterPartyIndices.length == 0) return new ArrayList<>();

        AtomicInteger[] partyDeltas = new AtomicInteger[partyList.size()];
        for(int i=0; i<partyDeltas.length; i++) partyDeltas[i] = new AtomicInteger(0);

        double baseMobility = parameters.getBaseMobilityRate() / 100.0;
        double globalMedia = parameters.getGlobalMediaInfluence() / 100.0;
        double uniformRange = parameters.getUniformRandomRange();

        // --- SKANDAL MANAGEMENT (SANFTERE VERSION) ---
        int scandalDuration = 100; // Dauer bleibt gleich, damit man es sieht
        activeScandals.removeIf(e -> currentStep - e.getOccurredAtStep() > scandalDuration);
        if (shouldScandalOccur() && partyList.size() > 1) triggerScandal();

        // 1. Skandal-Druck ("Pressure") berechnen
        double[] currentScandalPressure = new double[partyList.size()];

        for (ScandalEvent event : activeScandals) {
            Party affected = event.getAffectedParty();
            int pIndex = partyList.indexOf(affected);
            if (pIndex != -1) {
                int age = currentStep - event.getOccurredAtStep();
                double strength = event.getScandal().getStrength();

                double timeFactor;
                if (age < 15) { // Etwas längerer Ramp-Up (15 Ticks), wirkt natürlicher
                    timeFactor = (double) age / 15.0;
                } else {
                    timeFactor = 1.0 - ((double) (age - 15) / (scandalDuration - 15));
                }

                // ANPASSUNG 1: Maximaler Druck reduziert (30 statt 60)
                // Ein Skandal haut nicht mehr so extrem rein.
                currentScandalPressure[pIndex] += strength * 30.0 * timeFactor;
            }
        }

        double[] partyPos = partyList.stream().mapToDouble(Party::getPoliticalPosition).toArray();
        double[] partyBudgetFactor = partyList.stream()
                .mapToDouble(p -> p.getCampaignBudget() / SimulationConfig.CAMPAIGN_BUDGET_FACTOR)
                .toArray();

        // Momentum
        double[] partyDailyMomentum = new double[partyList.size()];
        for(int k=0; k < partyList.size(); k++) {
            partyDailyMomentum[k] = 0.9 + (java.util.concurrent.ThreadLocalRandom.current().nextDouble() * 0.2);
        }

        IntStream.range(0, voterPartyIndices.length).parallel().forEach(i -> {
            // Wähler-Drift
            double drift = (java.util.concurrent.ThreadLocalRandom.current().nextDouble() - 0.5) * 0.3;
            voterPositions[i] += (float) drift;
            if (voterPositions[i] < 0) voterPositions[i] = 0;
            if (voterPositions[i] > 100) voterPositions[i] = 100;

            int currentIdx = voterPartyIndices[i];

            double myPartyPressure = (currentIdx > 0) ? currentScandalPressure[currentIdx] : 0;
            double switchProb = baseMobility * (1.0 - voterLoyalties[i] / 200.0) * voterMediaInfluence[i];

            // ANPASSUNG 2: Wechselwahrscheinlichkeit gedämpft
            // Statt /200 teilen wir durch /300.
            // Beispiel: Druck 30 -> +10% Wechselchance (vorher +15% bis +30%).
            // Das sorgt für einen "Delle" im Graphen statt einen "Absturz".
            if (myPartyPressure > 0) {
                switchProb += (myPartyPressure / 300.0);
            }

            if (currentIdx == 0) switchProb *= 1.5;
            if (switchProb > 0.7) switchProb = 0.7; // Cap etwas niedriger

            double rnd = java.util.concurrent.ThreadLocalRandom.current().nextDouble();

            if (rnd < switchProb) {
                int targetIdx = currentIdx;

                // ANPASSUNG 3: Nur noch bei stärkeren Skandalen (Druck > 8) flüchten Wähler gezielt ins "Unsicher"-Lager.
                // Kleine Skandale führen eher zum Wechsel zur Konkurrenz.
                boolean fleeingFromScandal = (myPartyPressure > 8.0);

                if (!fleeingFromScandal && currentIdx != 0 && java.util.concurrent.ThreadLocalRandom.current().nextDouble() < 0.2) {
                    targetIdx = 0;
                } else {
                    double bestScore = -Double.MAX_VALUE;
                    double campaignEffectiveness = java.util.concurrent.ThreadLocalRandom.current().nextDouble() * uniformRange;

                    for (int pIdx = 1; pIdx < partyList.size(); pIdx++) {
                        if (pIdx == currentIdx) continue;

                        double dist = Math.abs(voterPositions[i] - partyPos[pIdx]);
                        double distScore = 40.0 / (1.0 + (dist * 0.05));

                        double budgetScore = (partyBudgetFactor[pIdx] * campaignEffectiveness * voterMediaInfluence[i] * globalMedia) * 15.0;
                        budgetScore *= partyDailyMomentum[pIdx];

                        double score = distScore + budgetScore;

                        // Konkurrenz wird durch deren Skandale unattraktiver
                        score -= currentScandalPressure[pIdx];

                        double noise = (java.util.concurrent.ThreadLocalRandom.current().nextDouble() - 0.5) * 10.0 * uniformRange;
                        double finalScore = score + noise;

                        if (finalScore > bestScore) {
                            bestScore = finalScore;
                            targetIdx = pIdx;
                        }
                    }

                    if (bestScore < 0) {
                        targetIdx = 0;
                    }
                }

                if (targetIdx != currentIdx) {
                    voterPartyIndices[i] = (byte) targetIdx;
                    partyDeltas[currentIdx].decrementAndGet();
                    partyDeltas[targetIdx].incrementAndGet();

                    if (java.util.concurrent.ThreadLocalRandom.current().nextDouble() < SimulationConfig.VISUALIZATION_SAMPLE_RATE) {
                        visualTransitions.add(new VoterTransition(partyList.get(currentIdx), partyList.get(targetIdx)));
                    }
                }
            }
        });

        for (int i = 0; i < partyList.size(); i++) {
            int delta = partyDeltas[i].get();
            if (delta != 0) {
                Party p = partyList.get(i);
                p.setCurrentSupporterCount(p.getCurrentSupporterCount() + delta);
            }
        }

        return new ArrayList<>(visualTransitions);
    }

    private boolean shouldScandalOccur() {
        timeUntilNextScandal -= 1.0;
        if (timeUntilNextScandal <= 0) {
            scheduleNextScandal();
            return true;
        }
        return false;
    }

    private void triggerScandal() {
        List<Party> realParties = partyList.stream().filter(p -> !p.getName().equals(SimulationConfig.UNDECIDED_NAME)).toList();
        if (!realParties.isEmpty()) {
            Party target = realParties.get(random.nextInt(realParties.size()));
            Scandal s = csvLoader.getRandomScandal();
            ScandalEvent e = new ScandalEvent(s, target, currentStep);
            activeScandals.add(e);
            lastScandal = e;
        }
    }

    public void resetState() { initializeSimulation(); }
    public List<Party> getParties() { return partyList; }
    public SimulationParameters getParameters() { return parameters; }
    public ScandalEvent getLastScandal() { ScandalEvent s = lastScandal; lastScandal = null; return s; }
    public int getCurrentStep() { return currentStep; }
}
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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class SimulationEngine {

    // Arrays (Structure of Arrays)
    private byte[] voterPartyIndices;
    private float[] voterLoyalties;
    private float[] voterPositions;
    private float[] voterMediaInfluence;

    private final List<Party> partyList = new CopyOnWriteArrayList<>();

    private Party undecidedParty;
    private final CSVLoader csvLoader;
    private SimulationParameters parameters;
    private final Random random = new Random();
    private double[] partyPermanentDamage;

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

    public void updateParameters(SimulationParameters newParams) {
        boolean structuralChange = (newParams.getNumberOfParties() != parameters.getNumberOfParties()) ||
                (newParams.getTotalVoterCount() != parameters.getTotalVoterCount());

        this.parameters = newParams;
        initializeDistributions();

        if (structuralChange) {
            initializeSimulation();
        }
    }

    public void initializeSimulation() {
        partyList.clear();
        activeScandals.clear();
        currentStep = 0;

        int partyCount = parameters.getNumberOfParties();
        partyPermanentDamage = new double[partyCount + 1];

        // ANPASSUNG: Unsicher-Partei mit gleichem Namen für voll/kurz initialisieren
        undecidedParty = new Party(
                SimulationConfig.UNDECIDED_NAME,
                SimulationConfig.UNDECIDED_NAME,
                SimulationConfig.UNDECIDED_COLOR,
                SimulationConfig.UNDECIDED_POSITION,
                0,
                0
        );
        partyList.add(undecidedParty);

        List<PartyTemplate> templates = csvLoader.getRandomPartyTemplates(partyCount);
        for (int i = 0; i < partyCount; i++) {
            PartyTemplate template = templates.get(i);
            double pos = Math.max(5, Math.min(95, (100.0 / (partyCount + 1)) * (i + 1) + (random.nextDouble() - 0.5) * 10));

            double baseBudget = 300000.0 + random.nextDouble() * 400000.0;
            double budget = baseBudget * parameters.getCampaignBudgetFactor();

            partyList.add(template.toParty(pos, budget));
        }

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
        for(Party p : partyList) p.setCurrentSupporterCount(0);

        int[] counts = new int[partyList.size()];
        int maxIdx = counts.length - 1;

        for(byte idx : voterPartyIndices) {
            if (idx <= maxIdx) {
                counts[idx]++;
            }
        }
        for(int i=0; i<counts.length; i++) {
            if (i < partyList.size()) {
                partyList.get(i).setCurrentSupporterCount(counts[i]);
            }
        }
    }

    public List<VoterTransition> runSimulationStep() {
        currentStep++;
        ConcurrentLinkedQueue<VoterTransition> visualTransitions = new ConcurrentLinkedQueue<>();

        if (voterPartyIndices == null || voterPartyIndices.length == 0) return new ArrayList<>();

        int pSize = partyList.size();
        AtomicInteger[] partyDeltas = new AtomicInteger[pSize];
        for(int i=0; i<pSize; i++) partyDeltas[i] = new AtomicInteger(0);

        double baseMobility = parameters.getBaseMobilityRate() / 100.0;
        double globalMedia = parameters.getGlobalMediaInfluence() / 100.0;
        double uniformRange = parameters.getUniformRandomRange();

        int scandalDuration = 100;
        activeScandals.removeIf(e -> currentStep - e.getOccurredAtStep() > scandalDuration);
        if (shouldScandalOccur() && pSize > 1) triggerScandal();

        double[] currentScandalPressure = new double[pSize];

        for (ScandalEvent event : activeScandals) {
            Party affected = event.getAffectedParty();
            int pIndex = partyList.indexOf(affected);
            if (pIndex != -1 && pIndex < pSize) {
                int age = currentStep - event.getOccurredAtStep();
                double strength = event.getScandal().getStrength();
                double timeFactor = (age < 15) ? (double) age / 15.0 : 1.0 - ((double) (age - 15) / (scandalDuration - 15));
                currentScandalPressure[pIndex] += strength * 30.0 * timeFactor;
            }
        }

        double[] partyPos = new double[pSize];
        double[] partyBudgetFactor = new double[pSize];
        double[] partyDailyMomentum = new double[pSize];

        for(int k=0; k < pSize; k++) {
            Party p = partyList.get(k);
            partyPos[k] = p.getPoliticalPosition();
            partyBudgetFactor[k] = p.getCampaignBudget() / SimulationConfig.CAMPAIGN_BUDGET_FACTOR;
            partyDailyMomentum[k] = 0.9 + (java.util.concurrent.ThreadLocalRandom.current().nextDouble() * 0.2);
        }

        IntStream.range(0, voterPartyIndices.length).parallel().forEach(i -> {
            double drift = (java.util.concurrent.ThreadLocalRandom.current().nextDouble() - 0.5) * 0.3;
            voterPositions[i] += (float) drift;
            if (voterPositions[i] < 0) voterPositions[i] = 0;
            if (voterPositions[i] > 100) voterPositions[i] = 100;

            int currentIdx = voterPartyIndices[i];

            if (currentIdx >= pSize) {
                currentIdx = 0;
                voterPartyIndices[i] = 0;
            }

            double totalPenalty = 0;
            if (currentIdx > 0) {
                totalPenalty = currentScandalPressure[currentIdx] + partyPermanentDamage[currentIdx];
            }

            double switchProb = baseMobility * (1.0 - voterLoyalties[i] / 200.0) * voterMediaInfluence[i];
            if (totalPenalty > 0) switchProb += (totalPenalty / 300.0);
            if (currentIdx == 0) switchProb *= 1.5;
            if (switchProb > 0.7) switchProb = 0.7;

            if (java.util.concurrent.ThreadLocalRandom.current().nextDouble() < switchProb) {
                int targetIdx = currentIdx;
                boolean fleeingFromDesaster = (totalPenalty > 8.0);

                if (!fleeingFromDesaster && currentIdx != 0 && java.util.concurrent.ThreadLocalRandom.current().nextDouble() < 0.2) {
                    targetIdx = 0;
                } else {
                    double bestScore = -Double.MAX_VALUE;
                    double campaignEffectiveness = java.util.concurrent.ThreadLocalRandom.current().nextDouble() * uniformRange;

                    for (int pIdx = 1; pIdx < pSize; pIdx++) {
                        if (pIdx == currentIdx) continue;

                        double dist = Math.abs(voterPositions[i] - partyPos[pIdx]);
                        double distScore = 40.0 / (1.0 + (dist * 0.05));
                        double budgetScore = (partyBudgetFactor[pIdx] * campaignEffectiveness * voterMediaInfluence[i] * globalMedia) * 15.0;
                        budgetScore *= partyDailyMomentum[pIdx];

                        double score = distScore + budgetScore;
                        score -= (currentScandalPressure[pIdx] + partyPermanentDamage[pIdx]);

                        double noise = (java.util.concurrent.ThreadLocalRandom.current().nextDouble() - 0.5) * 10.0 * uniformRange;
                        double finalScore = score + noise;

                        if (finalScore > bestScore) {
                            bestScore = finalScore;
                            targetIdx = pIdx;
                        }
                    }
                    if (bestScore < 0) targetIdx = 0;
                }

                if (targetIdx != currentIdx) {
                    voterPartyIndices[i] = (byte) targetIdx;
                    partyDeltas[currentIdx].decrementAndGet();
                    partyDeltas[targetIdx].incrementAndGet();

                    if (java.util.concurrent.ThreadLocalRandom.current().nextDouble() < SimulationConfig.VISUALIZATION_SAMPLE_RATE) {
                        if (currentIdx < partyList.size() && targetIdx < partyList.size()) {
                            visualTransitions.add(new VoterTransition(partyList.get(currentIdx), partyList.get(targetIdx)));
                        }
                    }
                }
            }
        });

        for (int i = 0; i < pSize; i++) {
            int delta = partyDeltas[i].get();
            if (delta != 0 && i < partyList.size()) {
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
        List<Party> realParties = partyList.stream()
                .filter(p -> !p.getName().equals(SimulationConfig.UNDECIDED_NAME))
                .toList();

        if (!realParties.isEmpty()) {
            Party target = realParties.get(random.nextInt(realParties.size()));
            Scandal s = csvLoader.getRandomScandal();
            ScandalEvent e = new ScandalEvent(s, target, currentStep);
            activeScandals.add(e);
            lastScandal = e;

            // ANPASSUNG: Zähler hochzählen
            target.incrementScandalCount();

            int pIndex = partyList.indexOf(target);
            if (pIndex != -1) {
                partyPermanentDamage[pIndex] += s.getStrength() * 5.0;
            }
        }
    }

    public void resetState() { initializeSimulation(); }
    public List<Party> getParties() { return partyList; }
    public SimulationParameters getParameters() { return parameters; }
    public ScandalEvent getLastScandal() { ScandalEvent s = lastScandal; lastScandal = null; return s; }
    public int getCurrentStep() { return currentStep; }
}
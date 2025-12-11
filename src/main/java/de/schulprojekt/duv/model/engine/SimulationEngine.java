package de.schulprojekt.duv.model.engine;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
//--- Distributions ---
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;
//--- Internal imports ---
import de.schulprojekt.duv.model.entities.Voter;
import de.schulprojekt.duv.model.entities.Party;

public class SimulationEngine {

    // --- Distributions ---
    private NormalDistribution normalDistribution;
    private UniformRealDistribution uniformRealDistribution;
    private ExponentialDistribution exponentialDistribution;

    //--- Lists ---
    private final List<Voter> voterList;
    private final List<Party> partyList;

    //--- Saved parameters locally ---
    private SimulationParameters parameters;

    // --- CONSTRUCTOR ---
    public SimulationEngine(SimulationParameters params) {

        this.parameters = params;
        this.voterList = new ArrayList<>();
        this.partyList = new ArrayList<>();

        // Initialisiere Verteilungen basierend auf Startparametern
        this.normalDistribution = new NormalDistribution(
                params.getInitialLoyaltyMean(),
                10.0
        );

        this.uniformRealDistribution = new UniformRealDistribution(
                0.0,
                params.getUniformRandomRange()
        );

        this.exponentialDistribution = new ExponentialDistribution(
                params.getScandalChance() / 100.0
        );
    }

    // --- DISTRIBUTION GENERATORS ---
    public double generateLoyaltyValue(){
        return normalDistribution.sample();
    }

    public double generateCampaignEffectiveness(){
        return uniformRealDistribution.sample();
    }

    public double generateWaitingTimeForEvent() {
        return exponentialDistribution.sample();
    }

    // --- INITIALIZATION / RESET ---
    public void initializeSimulation(){
        int partyCount = parameters.getNumberOfParties();
        if (partyCount == 0) {
            return;
        }

        // Feste, kontrastreiche Farben für die Visualisierung (Checklistenpunkt 1)
        String[] partyColors = {"007bff", "dc3545", "ffc107", "28a745", "6f42c1", "20c997", "fd7e14", "6c757d"};

        // 1. Parteien erstellen
        for(int i=0; i < partyCount; i++){
            String name = "Partei " + (char)('A' + i);
            double position;

            // Setzt die Position gleichmäßig auf der politischen Skala (0 bis 100)
            if (partyCount <= 1) {
                position = 50.0;
            } else {
                position = 100.0 / (partyCount - 1) * i;
            }

            String color = partyColors[i % partyColors.length];
            double budget = 500000.0;

            Party party = new Party(name, color, position, budget, 0);
            this.partyList.add(party);
        }

        // 2. Wähler erstellen
        int totalVoters = parameters.getTotalVoterCount();
        Random generalRandom = new Random();

        for (int i = 0; i < totalVoters; i++) {
            Party initialParty = this.partyList.get(i % partyCount);
            double loyalty = generateLoyaltyValue();
            double politicalPosition = generalRandom.nextDouble() * 100.0;
            double mediaInfluenceability = generalRandom.nextDouble();

            Voter voter = new Voter(initialParty, loyalty, politicalPosition, mediaInfluenceability);
            this.voterList.add(voter);

            initialParty.setCurrentSupporterCount(initialParty.getCurrentSupporterCount() + 1);
        }
    }

    public void resetState() {
        this.voterList.clear();
        this.partyList.clear();

        initializeSimulation();
    }

    // --- UPDATE PARAMETERS ---
    public void updateParameters(SimulationParameters newParams) {
        this.parameters = newParams;

        // Re-initialisiere Verteilungen basierend auf den neuen Parametern
        this.normalDistribution = new NormalDistribution(
                newParams.getInitialLoyaltyMean(),
                10.0
        );
        this.uniformRealDistribution = new UniformRealDistribution(
                0.0,
                newParams.getUniformRandomRange()
        );
        this.exponentialDistribution = new ExponentialDistribution(
                newParams.getScandalChance() / 100.0
        );
    }

    // --- MAIN SIMULATION STEP LOGIC ---

    private double getPartyTargetScore(Voter voter, Party party, double campaignEffectiveness) {
        double distance = Math.abs(voter.getPoliticalPosition() - party.getPoliticalPosition());
        double baseScore = 1.0 / (distance + 1.0);
        double campaignInfluence = party.getCampaignBudget() * campaignEffectiveness;
        return baseScore + (campaignInfluence * voter.getMediaInfluenceability());
    }

    /**
     * Führt einen diskreten Zeitschritt der Simulation aus.
     */
    public List<VoterTransition> runSimulationStep() {
        List<VoterTransition> transitions = new ArrayList<>();
        Random random = new Random();

        boolean scandalOccurred = (random.nextDouble() * 100.0 < parameters.getScandalChance());
        Party affectedParty = null;

        if (scandalOccurred) {
            int partyIndex = random.nextInt(partyList.size());
            affectedParty = partyList.get(partyIndex);
        }

        for (Voter voter : voterList) {
            double chanceToConsiderSwitch = parameters.getBaseMobilityRate() * voter.getMediaInfluenceability();

            if (random.nextDouble() < chanceToConsiderSwitch) {
                Party currentParty = voter.getCurrentParty();
                Party bestTargetParty = currentParty;
                double highestScore = -1.0;
                double campaignEffectiveness = generateCampaignEffectiveness();

                for (Party targetParty : partyList) {
                    if (scandalOccurred && targetParty.equals(affectedParty)) {
                        continue;
                    }
                    double score = getPartyTargetScore(voter, targetParty, campaignEffectiveness);
                    if (score > highestScore) {
                        highestScore = score;
                        bestTargetParty = targetParty;
                    }
                }

                if (!currentParty.equals(bestTargetParty)) {
                    voter.setCurrentParty(bestTargetParty);
                    currentParty.setCurrentSupporterCount(currentParty.getCurrentSupporterCount() - 1);
                    bestTargetParty.setCurrentSupporterCount(bestTargetParty.getCurrentSupporterCount() + 1);
                    transitions.add(new VoterTransition(currentParty, bestTargetParty));
                }
            }
        }
        return transitions;
    }

    // --- GETTERS (Für Controller-Zugriff) ---
    public List<Voter> getVoters() {
        return Collections.unmodifiableList(this.voterList);
    }

    public List<Party> getParties() {
        return Collections.unmodifiableList(this.partyList);
    }

    public SimulationParameters getParameters() {
        return parameters;
    }
}
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

/**
 * The central class of the Model (MVC). It controls the main simulation loop,
 * manages the state of all entities, and contains all business logic,
 * completely decoupled from the GUI.
 */
public class SimulationEngine {

    //--- Distributions ---
    private final NormalDistribution normalDistribution;
    private final UniformRealDistribution uniformRealDistribution;
    private final ExponentialDistribution exponentialDistribution;

    //--- Lists ---
    private final List<Voter> voterList;
    private final List<Party> partyList;

    //--- Saved parameters locally ---
    private final SimulationParameters parameters;

    public SimulationEngine(SimulationParameters params) {

        this.parameters = params;
        this.voterList = new ArrayList<>();
        this.partyList = new ArrayList<>();

        //Normal distribution for Voter loyalty
        this.normalDistribution = new NormalDistribution(
                params.getInitialLoyaltyMean(),
                10.0 //Standard deviation for dispersion
        );

        //Uniform distribution for media effectivity
        this.uniformRealDistribution = new UniformRealDistribution(
                0.0,
                params.getUniformRandomRange()
        );

        this.exponentialDistribution = new ExponentialDistribution(
                params.getScandalChance() / 100.0 //Scandal chance percentage
        );
    }

    public double generateLoyaltyValue(){
        return normalDistribution.sample();
    }

    public double generateCampaignEffectiveness(){
        return uniformRealDistribution.sample();
    }

    public double generateWaitingTimeForEvent() {
        return exponentialDistribution.sample();
    }

    public void initializeSimulation(){
        int partyCount = parameters.getNumberOfParties();
        if (partyCount == 0) {
            return;
        }
        for(int i=0; i < partyCount; i++){
            String name = "Partei " + (char)('A' + i);
            double position;
            if (partyCount <= 1) {
                position = 50.0; // Setzt die Position auf die Mitte (50.0)
            } else {
                position = 100.0 / (partyCount - 1) * i;
            }
            //TODO: Color & Budget initializing
            String color = "5E2028";
            double budget = 500000.0;

            Party party = new Party(name, color, position, budget, 0);
            this.partyList.add(party);
        }

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

    // --- Getter for Controller (Encapsulation) ---
    public List<Voter> getVoters() {
        return Collections.unmodifiableList(this.voterList);
    }

    public List<Party> getParties() {
        return Collections.unmodifiableList(this.partyList);
    }

    // --- Helper Method for Simulation Logic (Needed for runSimulationStep) ---

    /**
     * Calculates an attractiveness score for a target party for a given voter.
     * @param voter The voter currently considering a switch.
     * @param party The target party.
     * @param campaignEffectiveness The uniform random value for budget effectiveness.
     * @return A double score, where a higher score means higher attractiveness.
     */
    private double getPartyTargetScore(Voter voter, Party party, double campaignEffectiveness) {
        double distance = Math.abs(voter.getPoliticalPosition() - party.getPoliticalPosition());
        double baseScore = 1.0 / (distance + 1.0);
        double campaignInfluence = party.getCampaignBudget() * campaignEffectiveness;
        return baseScore + (campaignInfluence * voter.getMediaInfluenceability());
    }

    // --- Main Simulation Step ---

    /**
     * Executes one discrete time step of the simulation.
     * It iterates through all voters and calculates potential party changes
     * and random events. Uses Normal, Uniform, and Exponential Distributions.
     * @return A list of all VoterTransition DTOs for the View's animation update.
     */
    public List<VoterTransition> runSimulationStep() {
        List<VoterTransition> transitions = new ArrayList<>();

        boolean scandalOccurred = (uniformRealDistribution.sample() * 100.0 < parameters.getScandalChance());
        Party affectedParty = null;

        if (scandalOccurred) {
            int partyIndex = new Random().nextInt(partyList.size());
            affectedParty = partyList.get(partyIndex);
        }

        for (Voter voter : voterList) {
            double chanceToConsiderSwitch = parameters.getBaseMobilityRate() * voter.getMediaInfluenceability();

            if (new Random().nextDouble() < chanceToConsiderSwitch) {
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
    public void resetState() {
        this.voterList.clear();
        this.partyList.clear();

        initializeSimulation();
    }
}
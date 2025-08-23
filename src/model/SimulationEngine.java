package model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The `SimulationEngine` class simulates the interaction between voters and parties
 * in a political system. It initializes a set of parties and voters, assigns budgets
 * to parties, and simulates voter preference updates over a series of steps.
 */
public class SimulationEngine {
    private List<Party> parties; // List of political parties in the simulation
    private List<Voter> voters; // List of voters in the simulation

    /**
     * Constructs a `SimulationEngine` with the specified number of voters, parties,
     * and a total campaign budget to be distributed among the parties.
     *
     * @param numberOfVoters      The number of voters in the simulation.
     * @param numberOfParties     The number of parties in the simulation.
     * @param totalCampaignBudget The total budget to be distributed among the parties.
     */
    public SimulationEngine(int numberOfVoters, int numberOfParties, double totalCampaignBudget) {
        this.parties = new ArrayList<>(numberOfParties);
        this.voters = new ArrayList<>(numberOfVoters);

        Random rand = new Random();
        double remainingBudget = totalCampaignBudget; // Remaining budget to be distributed
        double totalRandomWeight = 0; // Sum of random weights for budget distribution
        List<Double> randomWeight = new ArrayList<>();

        // Generate random weights for each party and calculate the total weight
        for (int i = 0; i < numberOfParties; i++) {
            double weight = rand.nextDouble();
            randomWeight.add(weight);
            totalRandomWeight += weight;
        }

        // Distribute the total campaign budget among the parties based on random weights
        for (int i = 0; i < numberOfParties; i++) {
            double budget = (randomWeight.get(i) / totalRandomWeight) * totalCampaignBudget;
            parties.add(new Party("Party " + (i + 1), "Ideology " + (i + 1), budget, 0));
        }

        // Initialize voters with random preferences and attributes
        for (int i = 0; i < numberOfVoters; i++) {
            Party initialPreference = parties.get(rand.nextInt(parties.size()));
            double opinionStrength = rand.nextDouble();
            double susceptibilityToInfluence = rand.nextDouble();
            voters.add(new Voter(initialPreference, opinionStrength, susceptibilityToInfluence));
        }
    }

    /**
     * Runs the simulation for a specified number of steps. During each step:
     * - Voters update their preferences based on the parties.
     * - Party supporter counts are reset and recalculated based on voter preferences.
     *
     * @param numberOfSteps The number of steps to run the simulation.
     */
    public void runSimulation(int numberOfSteps) {
        for (int step = 0; step < numberOfSteps; step++) {
            // Update voter preferences based on the parties
            for (Voter voter : voters) {
                voter.updatePreference(parties);
            }

            // Reset supporter counts for all parties
            for (Party party : parties) {
                party.setSupporter(0);
            }

            // Recalculate supporter counts based on voter preferences
            for (Voter voter : voters) {
                Party preferredParty = voter.getCurrentPreference();
                preferredParty.setSupporter(preferredParty.getSupporter() + 1);
            }
        }
    }

    /**
     * Returns the list of parties in the simulation.
     *
     * @return A list of `Party` objects.
     */
    public List<Party> getParties() {
        return parties;
    }
}
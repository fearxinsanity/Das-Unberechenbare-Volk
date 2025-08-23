package model;

import java.util.List;
import java.util.Random;

/**
 * The `Voter` class represents an individual voter in the simulation.
 * Each voter has a current party preference, an opinion strength, and a susceptibility to influence.
 * Voters can update their preferences based on the influence of political parties.
 */
public class Voter {
    private Party currentPreference; // The current party preference of the voter
    private double opinionStrength; // The strength of the voter's opinion towards their preferred party
    private double susceptibilityToInfluence; // The susceptibility of the voter to be influenced by other parties

    /**
     * Constructs a `Voter` object with the specified initial preference, opinion strength, and susceptibility to influence.
     *
     * @param currentPreference       The initial party preference of the voter.
     * @param opinionStrength         The strength of the voter's opinion towards their preferred party.
     * @param susceptibilityToInfluence The susceptibility of the voter to be influenced by other parties.
     */
    public Voter(Party currentPreference, double opinionStrength, double susceptibilityToInfluence) {
        this.currentPreference = currentPreference;
        this.opinionStrength = opinionStrength;
        this.susceptibilityToInfluence = susceptibilityToInfluence;
    }

    /**
     * Returns the current party preference of the voter.
     *
     * @return The current party preference of the voter.
     */
    public Party getCurrentPreference() {
        return currentPreference;
    }

    /**
     * Sets the current party preference of the voter.
     *
     * @param currentPreference The new party preference of the voter.
     */
    public void setCurrentPreference(Party currentPreference) {
        this.currentPreference = currentPreference;
    }

    /**
     * Returns the strength of the voter's opinion towards their preferred party.
     *
     * @return The strength of the voter's opinion.
     */
    public double getOpinionStrength() {
        return opinionStrength;
    }

    /**
     * Sets the strength of the voter's opinion towards their preferred party.
     *
     * @param opinionStrength The new strength of the voter's opinion.
     */
    public void setOpinionStrength(double opinionStrength) {
        this.opinionStrength = opinionStrength;
    }

    /**
     * Returns the susceptibility of the voter to be influenced by other parties.
     *
     * @return The susceptibility to influence.
     */
    public double getSusceptibilityToInfluence() {
        return susceptibilityToInfluence;
    }

    /**
     * Sets the susceptibility of the voter to be influenced by other parties.
     *
     * @param susceptibilityToInfluence The new susceptibility to influence.
     */
    public void setSusceptibilityToInfluence(double susceptibilityToInfluence) {
        this.susceptibilityToInfluence = susceptibilityToInfluence;
    }

    /**
     * Updates the voter's party preference based on the influence of all parties.
     * The voter may reconsider their preference based on their opinion strength and the influence of other parties.
     *
     * @param allParties A list of all parties in the simulation.
     */
    public void updatePreference(List<Party> allParties) {
        Random random = new Random();

        // Calculate the chance of reconsidering the current preference
        double reconsiderationChance = 1.0 - this.opinionStrength;
        if (random.nextDouble() < reconsiderationChance) {
            Party potentialNewPreference = findPotentialNewPreference(allParties);

            // Update preference if the new party is more influential
            if (potentialNewPreference != null && isMoreInfluential(potentialNewPreference, this.currentPreference)) {
                this.currentPreference = potentialNewPreference;
                this.opinionStrength = this.opinionStrength * 0.9; // Reduce opinion strength after switching
            }
        }
    }

    /**
     * Finds a potential new party preference for the voter based on the campaign budgets of all parties.
     *
     * @param allParties A list of all parties in the simulation.
     * @return A potential new party preference, or null if no party is selected.
     */
    private Party findPotentialNewPreference(List<Party> allParties) {
        double totalBudget = allParties.stream().mapToDouble(Party::getCampaignBudget).sum();
        double randomValue = new Random().nextDouble() * totalBudget;

        // Select a party based on their proportional campaign budget
        for (Party party : allParties) {
            randomValue -= party.getCampaignBudget();
            if (randomValue <= 0) {
                return party;
            }
        }
        return null;
    }

    /**
     * Determines if a potential new party is more influential than the current party.
     * The influence is calculated based on the campaign budgets and the voter's susceptibility to influence.
     *
     * @param potentialNewParty The potential new party preference.
     * @param currentParty      The current party preference.
     * @return True if the potential new party is more influential, false otherwise.
     */
    private boolean isMoreInfluential(Party potentialNewParty, Party currentParty) {
        Random random = new Random();
        double currentInfluence = currentParty.getCampaignBudget() * random.nextDouble();
        double potentialInfluence = potentialNewParty.getCampaignBudget() * random.nextDouble();

        return potentialInfluence > currentInfluence * (1.0 - this.susceptibilityToInfluence);
    }
}
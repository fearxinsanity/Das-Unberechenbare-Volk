package model;

import java.util.List;
import java.util.Random;

public class Voter {
    private Party currentPreference;
    private double opinionStrength;
    private double susceptibilityToInfluence;

    public Voter(Party currentPreference, double opinionStrength, double susceptibilityToInfluence) {
        this.currentPreference = currentPreference;
        this.opinionStrength = opinionStrength;
        this.susceptibilityToInfluence = susceptibilityToInfluence;
    }

    public Party getCurrentPreference() {
        return currentPreference;
    }

    public void setCurrentPreference(Party currentPreference) {
        this.currentPreference = currentPreference;
    }

    public double getOpinionStrength() {
        return opinionStrength;
    }

    public void setOpinionStrength(double opinionStrength) {
        this.opinionStrength = opinionStrength;
    }

    public double getSusceptibilityToInfluence() {
        return susceptibilityToInfluence;
    }

    public void setSusceptibilityToInfluence(double susceptibilityToInfluence) {
        this.susceptibilityToInfluence = susceptibilityToInfluence;
    }

    public void updatePreference(List<Party> allParties) {
        Random random = new Random();

        if (random.nextDouble() < this.susceptibilityToInfluence) {
            Party mostInfluentialParty = findMostInfluentialParty(allParties);

            if (mostInfluentialParty != null) {
                this.currentPreference = mostInfluentialParty;
            }
        }
    }

    private Party findMostInfluentialParty(List<Party> allParties) {
        Party mostInfluential = null;
        double highestInfluenceScore = -1;
        Random random = new Random();

        for (Party party : allParties) {
            double influenceScore = party.getCampaignBudget() * random.nextDouble();
            if (influenceScore > highestInfluenceScore) {
                highestInfluenceScore = influenceScore;
                mostInfluential = party;
            }
        }
        return mostInfluential;
    }
}

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

        double reconsiderationChance = 1.0 - this.opinionStrength;
        if (random.nextDouble() < reconsiderationChance) {
            Party potentialNewPreference = findPotentialNewPreference(allParties);

            if (potentialNewPreference != null && isMoreInfluential(potentialNewPreference, this.currentPreference)) {
                this.currentPreference = potentialNewPreference;
                this.opinionStrength = this.opinionStrength * 0.9;
            }
        }
    }

    private Party findPotentialNewPreference(List<Party> allParties) {
        double totalBudget = allParties.stream().mapToDouble(Party::getCampaignBudget).sum();
        double randomValue = new Random().nextDouble() * totalBudget;

        for (Party party : allParties) {
            randomValue -= party.getCampaignBudget();
            if (randomValue <= 0) {
                return party;
            }
        }
        return null;
    }

    private boolean isMoreInfluential(Party potentialNewParty, Party currentParty) {
        Random random = new Random();
        double currentInfluence = currentParty.getCampaignBudget() * random.nextDouble();
        double potentialInfluence = potentialNewParty.getCampaignBudget() * random.nextDouble();

        return potentialInfluence > currentInfluence * (1.0 - this.susceptibilityToInfluence);
    }
}
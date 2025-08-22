package model;

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
}

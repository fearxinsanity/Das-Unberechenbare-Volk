package de.schulprojekt.duv.model.entities;

public class Voter {

    private Party currentParty;
    private double partyLoyalty;
    private double politicalPosition;
    private double mediaInfluenceability;

    public Voter(Party currentParty, double partyLoyalty, double politicalPosition, double mediaInfluenceability) {
        this.currentParty = currentParty;
        this.partyLoyalty = partyLoyalty;
        this.politicalPosition = politicalPosition;
        this.mediaInfluenceability = mediaInfluenceability;
    }

    public Party getCurrentParty() {
        return currentParty;
    }

    public void setCurrentParty(Party currentParty) {
        this.currentParty = currentParty;
    }

    public double getPoliticalPosition() {
        return politicalPosition;
    }

    public void setPoliticalPosition(double politicalPosition) {
        this.politicalPosition = politicalPosition;
    }

    public double getPartyLoyalty() {
        return partyLoyalty;
    }

    public void setPartyLoyalty(double partyLoyalty) {
        this.partyLoyalty = partyLoyalty;
    }

    public double getMediaInfluenceability() {
        return mediaInfluenceability;
    }

    public void setMediaInfluenceability(double mediaInfluenceability) {
        this.mediaInfluenceability = mediaInfluenceability;
    }
}

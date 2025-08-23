package model;

/**
 * The `Party` class represents a political party in the simulation.
 * It contains attributes such as the party's name, political ideology,
 * campaign budget, and the number of supporters.
 */
public class Party {
    private String partyName; // The name of the party
    private String politicalIdeology; // The political ideology of the party
    private double campaignBudget; // The campaign budget allocated to the party
    private int supporter; // The number of supporters the party has

    /**
     * Constructs a `Party` object with the specified name, political ideology,
     * campaign budget, and initial number of supporters.
     *
     * @param partyName         The name of the party.
     * @param politicalIdeology The political ideology of the party.
     * @param campaignBudget    The campaign budget allocated to the party.
     * @param supporter         The initial number of supporters of the party.
     */
    public Party(String partyName, String politicalIdeology, double campaignBudget, int supporter) {
        this.partyName = partyName;
        this.politicalIdeology = politicalIdeology;
        this.campaignBudget = campaignBudget;
        this.supporter = supporter;
    }

    /**
     * Returns the name of the party.
     *
     * @return The name of the party.
     */
    public String getPartyName() {
        return partyName;
    }

    /**
     * Sets the name of the party.
     *
     * @param partyName The new name of the party.
     */
    public void setPartyName(String partyName) {
        this.partyName = partyName;
    }

    /**
     * Returns the political ideology of the party.
     *
     * @return The political ideology of the party.
     */
    public String getPoliticalIdeology() {
        return politicalIdeology;
    }

    /**
     * Sets the political ideology of the party.
     *
     * @param politicalIdeology The new political ideology of the party.
     */
    public void setPoliticalIdeology(String politicalIdeology) {
        this.politicalIdeology = politicalIdeology;
    }

    /**
     * Returns the campaign budget of the party.
     *
     * @return The campaign budget of the party.
     */
    public double getCampaignBudget() {
        return campaignBudget;
    }

    /**
     * Sets the campaign budget of the party.
     *
     * @param campaignBudget The new campaign budget of the party.
     */
    public void setCampaignBudget(double campaignBudget) {
        this.campaignBudget = campaignBudget;
    }

    /**
     * Returns the number of supporters of the party.
     *
     * @return The number of supporters of the party.
     */
    public int getSupporter() {
        return supporter;
    }

    /**
     * Sets the number of supporters of the party.
     *
     * @param supporter The new number of supporters of the party.
     */
    public void setSupporter(int supporter) {
        this.supporter = supporter;
    }

    /**
     * Returns a string representation of the party, including its name
     * and the number of supporters.
     *
     * @return A string representation of the party.
     */
    @Override
    public String toString() {
        return partyName + ": " + supporter + " supporters";
    }
}
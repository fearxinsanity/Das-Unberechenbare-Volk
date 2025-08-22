package model;

public class Party {
    private String partyName;
    private String politicalIdeology;
    private double campaignBudget;
    private int supporter;

    public Party(String partyName, String politicalIdeology, double campaignBudget, int supporter) {
        this.partyName = partyName;
        this.politicalIdeology = politicalIdeology;
        this.campaignBudget = campaignBudget;
        this.supporter = supporter;
    }

    public String getPartyName() {
        return partyName;
    }

    public void setPartyName(String partyName) {
        this.partyName = partyName;
    }

    public String getPoliticalIdeology() {
        return politicalIdeology;
    }

    public void setPoliticalIdeology(String politicalIdeology) {
        this.politicalIdeology = politicalIdeology;
    }

    public double getCampaignBudget() {
        return campaignBudget;
    }

    public void setCampaignBudget(double campaignBudget) {
        this.campaignBudget = campaignBudget;
    }

    public int getSupporter() {
        return supporter;
    }

    public void setSupporter(int supporter) {
        this.supporter = supporter;
    }
}

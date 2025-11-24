package de.schulprojekt.duv.model.engine;

/**
 * A Data Transfer Object (DTO) that represents a single voter transition event
 * Used by SimulationEngine to pass animation data to the SimulationController.
 */
public class VoterTransition {

    private final String fromPartyName;
    private final String toPartyName;
    private final int voterCount;

    /**
     * @param fromPartyName The name of the originating party.
     * @param toPartyName   The name of the destination party.
     * @param voterCount    The number of voters in this specific transitioning.
     */
    public VoterTransition(String fromPartyName, String toPartyName, int voterCount) {
        this.fromPartyName = fromPartyName;
        this.toPartyName = toPartyName;
        this.voterCount = voterCount;
    }

    public String getFromPartyName() {
        return fromPartyName;
    }

    public String getToPartyName() {
        return toPartyName;
    }

    public int getVoterCount() {
        return voterCount;
    }
}

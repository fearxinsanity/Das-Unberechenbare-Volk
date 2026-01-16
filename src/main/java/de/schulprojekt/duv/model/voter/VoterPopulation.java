package de.schulprojekt.duv.model.voter;

import de.schulprojekt.duv.model.random.DistributionProvider;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

/**
 * Stores voter data in a memory-efficient Structure of Arrays (SoA) format.
 * @author Nico Hoffmann
 * @version 1.2
 */
public class VoterPopulation {

    // ========================================
    // Static Variables
    // ========================================

    private static final double UNDECIDED_RATIO = 0.20;
    private static final double POS_MEAN = 50.0;
    private static final double POS_STD_DEV = 25.0;
    private static final double MEDIA_INFLUENCE_EXPONENT = 0.7;

    // Voter type distribution probabilities (must sum to 1.0)
    private static final double PROB_PRAGMATIC = 0.25;
    private static final double PROB_IDEOLOGICAL = 0.15;
    private static final double PROB_RATIONAL_CHOICE = 0.20;
    private static final double PROB_AFFECTIVE = 0.15;
    private static final double PROB_HEURISTIC = 0.15;
    // PROB_POLITIKFERN is implicit (remaining probability after others)

    // ========================================
    // Instance Variables
    // ========================================

    private byte[] voterPartyIndices;
    private byte[] voterTypes;
    private float[] voterLoyalties;
    private float[] voterPositions;
    private float[] voterMediaInfluence;

    // ========================================
    // Getter Methods
    // ========================================

    public int size() {
        return voterPartyIndices != null ? voterPartyIndices.length : 0;
    }

    public byte getPartyIndex(int i) {
        validateIndex(i);
        return voterPartyIndices[i];
    }

    public VoterType getVoterType(int i) {
        validateIndex(i);
        return VoterType.values()[voterTypes[i]];
    }

    public float getPosition(int i) {
        validateIndex(i);
        return voterPositions[i];
    }

    public float getLoyalty(int i) {
        validateIndex(i);
        return voterLoyalties[i];
    }

    public float getMediaInfluence(int i) {
        validateIndex(i);
        return voterMediaInfluence[i];
    }

    // ========================================
    // Setter Methods
    // ========================================

    public void setPartyIndex(int i, byte idx) {
        validateIndex(i);
        voterPartyIndices[i] = idx;
    }

    public void setPosition(int i, float pos) {
        validateIndex(i);
        voterPositions[i] = pos;
    }

    // ========================================
    // Business Logic Methods
    // ========================================

    /**
     * Initializes the population with randomized values.
     * @param totalVoters total count of voters to generate
     * @param partyCount number of active parties
     * @param distProvider provider for statistical distributions
     */
    public void initialize(int totalVoters, int partyCount, DistributionProvider distProvider) {
        this.voterPartyIndices = new byte[totalVoters];
        this.voterTypes = new byte[totalVoters];
        this.voterLoyalties = new float[totalVoters];
        this.voterPositions = new float[totalVoters];
        this.voterMediaInfluence = new float[totalVoters];

        IntStream.range(0, totalVoters).parallel().forEach(i -> {
            ThreadLocalRandom rnd = ThreadLocalRandom.current();

            boolean isUndecided = rnd.nextDouble() < UNDECIDED_RATIO;
            int assignedParty = 0;

            if (!isUndecided && partyCount > 1) {
                assignedParty = 1 + rnd.nextInt(partyCount - 1);
            }
            voterPartyIndices[i] = (byte) assignedParty;

            voterTypes[i] = (byte) selectVoterType(rnd);

            voterLoyalties[i] = (float) distProvider.sampleLoyalty();

            double rawPos = POS_MEAN + (rnd.nextGaussian() * POS_STD_DEV);
            voterPositions[i] = (float) Math.max(0, Math.min(100, rawPos));
            voterMediaInfluence[i] = (float) Math.pow(rnd.nextDouble(), MEDIA_INFLUENCE_EXPONENT);
        });
    }

    // ========================================
    // Utility Methods
    // ========================================

    /**
     * Selects a voter type based on weighted probability distribution.
     * Uses cumulative probability matching to ensure all types sum to 1.0.
     * POLITIKFERN voters get the remaining probability (implicit).
     * @param rnd thread-local random instance
     * @return ordinal value of selected VoterType
     */
    private int selectVoterType(ThreadLocalRandom rnd) {
        double roll = rnd.nextDouble();
        double cumulative = 0.0;

        cumulative += PROB_PRAGMATIC;
        if (roll < cumulative) return VoterType.PRAGMATIC.ordinal();

        cumulative += PROB_IDEOLOGICAL;
        if (roll < cumulative) return VoterType.IDEOLOGICAL.ordinal();

        cumulative += PROB_RATIONAL_CHOICE;
        if (roll < cumulative) return VoterType.RATIONAL_CHOICE.ordinal();

        cumulative += PROB_AFFECTIVE;
        if (roll < cumulative) return VoterType.AFFECTIVE.ordinal();

        cumulative += PROB_HEURISTIC;
        if (roll < cumulative) return VoterType.HEURISTIC.ordinal();

        // Remaining probability (0.10) implicitly goes to POLITIKFERN
        return VoterType.POLITIKFERN.ordinal();
    }

    /**
     * Validates that the given index is within bounds.
     * @param index the index to validate
     * @throws IndexOutOfBoundsException if index is invalid
     */
    private void validateIndex(int index) {
        if (index < 0 || index >= size()) {
            throw new IndexOutOfBoundsException("Voter index out of bounds: " + index + " (size: " + size() + ")");
        }
    }
}

package de.schulprojekt.duv.model.voter;

import de.schulprojekt.duv.model.random.DistributionProvider;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

/**
 * Stores voter data in a memory-efficient Structure of Arrays (SoA) format.
 * @author Nico Hoffmann
 * @version 1.1
 */
public class VoterPopulation {

    // ========================================
    // Static Variables
    // ========================================

    private static final double UNDECIDED_RATIO = 0.20;
    private static final double POS_MEAN = 50.0;
    private static final double POS_STD_DEV = 25.0;
    private static final double MEDIA_INFLUENCE_EXPONENT = 0.7;

    // ========================================
    // Instance Variables
    // ========================================

    private byte[] voterPartyIndices;
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
        return voterPartyIndices[i];
    }

    public float getPosition(int i) {
        return voterPositions[i];
    }

    public float getLoyalty(int i) {
        return voterLoyalties[i];
    }

    public float getMediaInfluence(int i) {
        return voterMediaInfluence[i];
    }

    // ========================================
    // Setter Methods
    // ========================================

    public void setPartyIndex(int i, byte idx) {
        voterPartyIndices[i] = idx;
    }

    public void setPosition(int i, float pos) {
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
            voterLoyalties[i] = (float) distProvider.sampleLoyalty();

            double rawPos = POS_MEAN + (rnd.nextGaussian() * POS_STD_DEV);
            voterPositions[i] = (float) Math.max(0, Math.min(100, rawPos));
            voterMediaInfluence[i] = (float) Math.pow(rnd.nextDouble(), MEDIA_INFLUENCE_EXPONENT);
        });
    }
}
package de.schulprojekt.duv.model.voter;

import de.schulprojekt.duv.model.core.SimulationParameters;
import de.schulprojekt.duv.model.random.DistributionProvider;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

/**
 * Stores voter data in a memory-efficient Structure of Arrays (SoA) format.
 * Manages data access and dynamic evolution of voter attributes.
 * @author Nico Hoffmann
 * @version 1.3
 */
public class VoterPopulation {

    // ========================================
    // Static Variables
    // ========================================

    private static final double UNDECIDED_RATIO = 0.20;
    private static final double POS_MEAN = 50.0;
    private static final double POS_STD_DEV = 25.0;
    private static final double MEDIA_INFLUENCE_EXPONENT = 0.7;

    // Dynamics constants
    private static final double TYPE_CHANGE_PROBABILITY = 0.0001; // 0.01% chance per tick
    private static final float LOYALTY_FLUCTUATION = 2.0f;        // Max +/- 2.0 change per tick
    private static final float MEDIA_INFLUENCE_DRIFT = 0.05f;     // Max +/- 0.05 change per tick

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

    public void setVoterType(int i, VoterType type) {
        validateIndex(i);
        voterTypes[i] = (byte) type.ordinal();
    }

    public void setLoyalty(int i, float loyalty) {
        validateIndex(i);
        voterLoyalties[i] = loyalty;
    }

    public void setMediaInfluence(int i, float influence) {
        validateIndex(i);
        voterMediaInfluence[i] = influence;
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

    /**
     * Evolves voter attributes (loyalty, media influence, type) based on simulation parameters.
     * Uses parallel processing for performance.
     * @param params current simulation parameters (provides volatility rate)
     */
    public void updateVoterAttributes(SimulationParameters params) {
        double volatilityFactor = params.volatilityRate() / 50.0;

        IntStream.range(0, size()).parallel().forEach(i -> {
            ThreadLocalRandom rnd = ThreadLocalRandom.current();

            float currentLoyalty = voterLoyalties[i];
            float deltaLoyalty = (float) ((rnd.nextDouble() - 0.5) * LOYALTY_FLUCTUATION * volatilityFactor);
            float newLoyalty = Math.max(0, Math.min(100, currentLoyalty + deltaLoyalty));

            if (Math.abs(newLoyalty - currentLoyalty) > 0.01f) {
                voterLoyalties[i] = newLoyalty;
            }

            float currentMedia = voterMediaInfluence[i];
            float deltaMedia = (float) ((rnd.nextDouble() - 0.5) * MEDIA_INFLUENCE_DRIFT * volatilityFactor);
            float newMedia = Math.max(0.0f, Math.min(1.0f, currentMedia + deltaMedia));

            if (Math.abs(newMedia - currentMedia) > 0.001f) {
                voterMediaInfluence[i] = newMedia;
            }

            if (rnd.nextDouble() < (TYPE_CHANGE_PROBABILITY * volatilityFactor)) {
                VoterType[] allTypes = VoterType.values();
                byte newType = (byte) allTypes[rnd.nextInt(allTypes.length)].ordinal();
                if (newType != voterTypes[i]) {
                    voterTypes[i] = newType;
                }
            }
        });
    }

    // ========================================
    // Utility Methods
    // ========================================

    /**
     * Selects a voter type based on weighted probability distribution.
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

        return VoterType.POLITIKFERN.ordinal();
    }

    /**
     * Validates that the given index is within bounds.
     */
    private void validateIndex(int index) {
        if (index < 0 || index >= size()) {
            throw new IndexOutOfBoundsException("Voter index out of bounds: " + index + " (size: " + size() + ")");
        }
    }
}
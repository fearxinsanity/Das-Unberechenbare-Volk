package de.schulprojekt.duv.model.voter;

import de.schulprojekt.duv.model.random.DistributionProvider;
import org.apache.commons.math3.distribution.NormalDistribution;
import java.util.Random;
import java.util.stream.IntStream;

public class VoterPopulation {

    // Structure of Arrays (SoA)
    private byte[] voterPartyIndices;
    private float[] voterLoyalties;
    private float[] voterPositions;
    private float[] voterMediaInfluence;

    private final Random random = new Random();

    public void initialize(int totalVoters, int partyCount, DistributionProvider distProvider) {
        voterPartyIndices = new byte[totalVoters];
        voterLoyalties = new float[totalVoters];
        voterPositions = new float[totalVoters];
        voterMediaInfluence = new float[totalVoters];

        NormalDistribution posDist = new NormalDistribution(50.0, 25.0);

        // Parallele Initialisierung (1:1 aus deiner Engine übernommen)
        IntStream.range(0, totalVoters).parallel().forEach(i -> {
            boolean isUndecided = Math.random() < 0.20;
            voterPartyIndices[i] = (byte) (isUndecided ? 0 : 1 + random.nextInt(partyCount));
            voterLoyalties[i] = (float) distProvider.sampleLoyalty();
            voterPositions[i] = (float) Math.max(0, Math.min(100, posDist.sample()));
            voterMediaInfluence[i] = (float) Math.pow(random.nextDouble(), 0.7);
        });
    }

    public int size() { return voterPartyIndices.length; }

    // Performanter Zugriff
    public byte getPartyIndex(int i) { return voterPartyIndices[i]; }
    public void setPartyIndex(int i, byte idx) { voterPartyIndices[i] = idx; }

    public float getPosition(int i) { return voterPositions[i]; }
    public void setPosition(int i, float pos) { voterPositions[i] = pos; }

    public float getLoyalty(int i) { return voterLoyalties[i]; }
    public float getMediaInfluence(int i) { return voterMediaInfluence[i]; }
}
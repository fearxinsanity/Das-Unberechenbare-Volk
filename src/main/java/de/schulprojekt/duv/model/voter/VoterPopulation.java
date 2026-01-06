package de.schulprojekt.duv.model.voter;

import de.schulprojekt.duv.model.random.DistributionProvider;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

/**
 * Hält die Daten aller Wähler in einem speicher effizienten "Structure of Arrays" (SoA) Format.
 * Ermöglicht performante parallele Verarbeitung der Population.
 */
public class VoterPopulation {

    // --- Konstanten (Initialisierung) ---
    private static final double UNDECIDED_RATIO = 0.20;
    private static final double POS_MEAN = 50.0;
    private static final double POS_STD_DEV = 25.0;
    private static final double MEDIA_INFLUENCE_EXPONENT = 0.7;

    // --- Felder (Structure of Arrays) ---
    private byte[] voterPartyIndices;
    private float[] voterLoyalties;
    private float[] voterPositions;
    private float[] voterMediaInfluence;

    // --- Business Logik (Initialisierung) ---

    /**
     * Initialisiert die Population mit Zufallswerten basierend auf Verteilungen.
     * Nutzt Parallelisierung für schnelle Generierung.
     */
    public void initialize(int totalVoters, int partyCount, DistributionProvider distProvider) {
        // 1. Speicher allokieren
        this.voterPartyIndices = new byte[totalVoters];
        this.voterLoyalties = new float[totalVoters];
        this.voterPositions = new float[totalVoters];
        this.voterMediaInfluence = new float[totalVoters];

        // 2. Parallel befüllen
        // WICHTIG: ThreadLocalRandom nutzen, da shared Random in parallel streams blockiert!
        IntStream.range(0, totalVoters).parallel().forEach(i -> {
            ThreadLocalRandom rnd = ThreadLocalRandom.current();

            // A. Parteizugehörigkeit
            boolean isUndecided = rnd.nextDouble() < UNDECIDED_RATIO;
            int assignedParty = 0;

            // FIX: Index-Berechnung korrekt für Index 1 bis (partyCount-1)
            if (!isUndecided && partyCount > 1) {
                assignedParty = 1 + rnd.nextInt(partyCount - 1);
            }
            voterPartyIndices[i] = (byte) assignedParty;

            // B. Loyalität (via Provider)
            voterLoyalties[i] = (float) distProvider.sampleLoyalty();

            // C. Politische Position (Normalverteilung)
            // nextGaussian() liefert Mean 0.0, SD 1.0 -> Skalieren auf Mean 50, SD 25
            double rawPos = POS_MEAN + (rnd.nextGaussian() * POS_STD_DEV);
            voterPositions[i] = (float) Math.max(0, Math.min(100, rawPos));

            // D. Medieneinfluss (Verzerrte Verteilung)
            voterMediaInfluence[i] = (float) Math.pow(rnd.nextDouble(), MEDIA_INFLUENCE_EXPONENT);
        });
    }

    // --- Standard Getter & Setter ---

    public int size() {
        return voterPartyIndices != null ? voterPartyIndices.length : 0;
    }

    public byte getPartyIndex(int i) {
        return voterPartyIndices[i];
    }

    public void setPartyIndex(int i, byte idx) {
        voterPartyIndices[i] = idx;
    }

    public float getPosition(int i) {
        return voterPositions[i];
    }

    public void setPosition(int i, float pos) {
        voterPositions[i] = pos;
    }

    public float getLoyalty(int i) {
        return voterLoyalties[i];
    }

    public float getMediaInfluence(int i) {
        return voterMediaInfluence[i];
    }
}
package de.schulprojekt.duv.model.voter;

/**
 * Speicher-effiziente Datenstruktur (SoA) für Wählerdaten.
 * Alle Methoden sind auf maximale Performance ausgelegt.
 */
public class VoterPopulation {

    private byte[] voterPartyIndices;
    private byte[] voterTypes;
    private float[] voterLoyalties;
    private float[] voterPositions;
    private float[] voterMediaInfluence;

    public void allocate(int size) {
        this.voterPartyIndices = new byte[size];
        this.voterTypes = new byte[size];
        this.voterLoyalties = new float[size];
        this.voterPositions = new float[size];
        this.voterMediaInfluence = new float[size];
    }

    public int size() {
        return voterPartyIndices != null ? voterPartyIndices.length : 0;
    }

    // High-Performance Zugriffsmethoden (Raw)
    public byte getPartyIndexRaw(int i) { return voterPartyIndices[i]; }
    public void setPartyIndexRaw(int i, byte val) { voterPartyIndices[i] = val; }

    public byte getVoterTypeRaw(int i) { return voterTypes[i]; }
    public void setVoterTypeRaw(int i, byte val) { voterTypes[i] = val; }

    public float getPositionRaw(int i) { return voterPositions[i]; }
    public void setPositionRaw(int i, float val) { voterPositions[i] = val; }

    public float getLoyaltyRaw(int i) { return voterLoyalties[i]; }
    public void setLoyaltyRaw(int i, float val) { voterLoyalties[i] = val; }

    public float getMediaInfluenceRaw(int i) { return voterMediaInfluence[i]; }
    public void setMediaInfluenceRaw(int i, float val) { voterMediaInfluence[i] = val; }
}
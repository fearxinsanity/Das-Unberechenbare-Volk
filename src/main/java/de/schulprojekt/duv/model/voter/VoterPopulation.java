package de.schulprojekt.duv.model.voter;

/**
 * Structure-of-Arrays für die Wählerpopulation.
 * @author Nico Hoffmann
 * @version 1.0
 */
public class VoterPopulation {

    // ========================================
    // Instance Variables (SoA-Arrays)
    // ========================================

    private byte[] voterPartyIndices;
    private byte[] voterTypes;
    private float[] voterLoyalties;
    private float[] voterPositions;
    private float[] voterMediaInfluence;

    // ========================================
    // Lifecycle & Memory Management
    // ========================================

    /**
     * Allokiert den benötigten Speicher für die gesamte Population.
     * @param size Die Anzahl der zu simulierenden Wähler.
     */
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

    // ========================================
    // High-Performance Accessors (Raw)
    // ========================================

    /** @return Der Index der Partei, die der Wähler an Stelle i unterstützt. */
    public byte getPartyIndexRaw(int i) { return voterPartyIndices[i]; }

    /** Setzt den Parteien-Index für den Wähler an Stelle i. */
    public void setPartyIndexRaw(int i, byte val) { voterPartyIndices[i] = val; }

    /** @return Der ordinale Wert des VoterType für den Wähler an Stelle i. */
    public byte getVoterTypeRaw(int i) { return voterTypes[i]; }

    /** Setzt den Wählertyp-Index für den Wähler an Stelle i. */
    public void setVoterTypeRaw(int i, byte val) { voterTypes[i] = val; }

    /** @return Die politische Position (0-100) des Wählers an Stelle i. */
    public float getPositionRaw(int i) { return voterPositions[i]; }

    /** Setzt die politische Position für den Wähler an Stelle i. */
    public void setPositionRaw(int i, float val) { voterPositions[i] = val; }

    /** @return Der Loyalitätswert des Wählers an Stelle i. */
    public float getLoyaltyRaw(int i) { return voterLoyalties[i]; }

    /** Setzt den Loyalitätswert für den Wähler an Stelle i. */
    public void setLoyaltyRaw(int i, float val) { voterLoyalties[i] = val; }

    /** @return Der Medieneinfluss-Faktor (0.0-1.0) des Wählers an Stelle i. */
    public float getMediaInfluenceRaw(int i) { return voterMediaInfluence[i]; }

    /** Setzt den Medieneinfluss-Faktor für den Wähler an Stelle i. */
    public void setMediaInfluenceRaw(int i, float val) { voterMediaInfluence[i] = val; }
}
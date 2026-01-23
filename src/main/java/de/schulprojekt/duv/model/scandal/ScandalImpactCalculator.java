package de.schulprojekt.duv.model.scandal;

import de.schulprojekt.duv.model.party.Party;
import de.schulprojekt.duv.util.config.ScandalConfig;

import java.util.Arrays;
import java.util.List;

/**
 * Berechnet die Auswirkungen von Skandalen auf Wähler und Parteien.
 * @author Nico Hoffmann
 * @version 1.0
 */
public class ScandalImpactCalculator {

    // ========================================
    // Instance Variables
    // ========================================

    private final double[] partyPermanentDamage;

    // ========================================
    // Constructors
    // ========================================

    /**
     * Initialisiert den Rechner mit einer festen Größe für die Parteien-Verfolgung
     * @param maxParties Maximale Anzahl der erwarteten Parteien
     */
    public ScandalImpactCalculator(int maxParties) {
        this.partyPermanentDamage = new double[maxParties + 10];
    }

    // ========================================
    // Getter Methods
    // ========================================

    public double getPermanentDamage(int partyIndex) {
        if (isValidIndex(partyIndex)) {
            return partyPermanentDamage[partyIndex];
        }
        return 0.0;
    }

    // ========================================
    // Business Logic Methods
    // ========================================

    public double[] calculateAcutePressure(List<ScandalEvent> activeScandals, List<Party> parties, int currentStep) {
        double[] acutePressure = new double[parties.size()];

        for (ScandalEvent event : activeScandals) {
            Party affected = event.affectedParty();
            int pIndex = parties.indexOf(affected);

            if (pIndex == -1 || pIndex >= acutePressure.length) continue;

            int age = currentStep - event.occurredAtStep();
            if (age > ScandalConfig.SCANDAL_DURATION) continue;

            double strength = event.scandal().strength();
            double timeFactor = ScandalConfig.calculateTimeFactor(age);

            acutePressure[pIndex] += strength * ScandalConfig.ACUTE_PRESSURE_FACTOR * timeFactor;
            accumulatePermanentDamage(pIndex, strength);
        }
        return acutePressure;
    }

    public void processRecovery(List<Party> parties, int totalVoters) {
        for (int i = 1; i < parties.size(); i++) {
            if (isValidIndex(i) && partyPermanentDamage[i] > 0) {
                Party p = parties.get(i);
                double voterShare = (double) p.getCurrentSupporterCount() / Math.max(1, totalVoters);

                double recoveryRate = ScandalConfig.calculateRecoveryRate(voterShare);

                partyPermanentDamage[i] -= recoveryRate;
                if (partyPermanentDamage[i] < 0) {
                    partyPermanentDamage[i] = 0;
                }
            }
        }
    }

    public void reset() {
        Arrays.fill(partyPermanentDamage, 0.0);
    }

    // ========================================
    // Utility Methods
    // ========================================

    private void accumulatePermanentDamage(int index, double strength) {
        if (isValidIndex(index)) {
            double damageBuildUp = ScandalConfig.calculatePermanentDamagePerTick(strength);
            partyPermanentDamage[index] += damageBuildUp;
        }
    }

    private boolean isValidIndex(int index) {
        return index >= 0 && index < partyPermanentDamage.length;
    }
}

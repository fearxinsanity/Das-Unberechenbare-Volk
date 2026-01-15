package de.schulprojekt.duv.model.scandal;

import de.schulprojekt.duv.model.party.Party;
import java.util.Arrays;
import java.util.List;

/**
 * Calculates the impact of scandals on voters and parties.
 * * @author Nico Hoffmann
 * @version 1.1
 */
public class ScandalImpactCalculator {

    // ========================================
    // Static Variables
    // ========================================

    private static final int SCANDAL_DURATION = 300;
    private static final int FADE_IN_TICKS = 90;
    private static final double ACUTE_PRESSURE_FACTOR = 4.0;
    private static final double PERMANENT_DAMAGE_FACTOR = 2.0;
    private static final double BASE_RECOVERY_RATE = 0.005;
    private static final double VOTER_SHARE_RECOVERY_FACTOR = 0.04;

    // ========================================
    // Instance Variables
    // ========================================

    private final double[] partyPermanentDamage;

    // ========================================
    // Constructors
    // ========================================

    /**
     * Initializes the calculator with a fixed size for party tracking.
     * * @param maxParties maximum number of expected parties
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

            if (pIndex == -1) continue;

            int age = currentStep - event.occurredAtStep();
            if (age > SCANDAL_DURATION) continue;

            double strength = event.scandal().strength();
            double timeFactor = calculateTimeFactor(age);

            acutePressure[pIndex] += strength * ACUTE_PRESSURE_FACTOR * timeFactor;
            accumulatePermanentDamage(pIndex, strength);
        }
        return acutePressure;
    }

    public void processRecovery(List<Party> parties, int totalVoters) {
        for (int i = 1; i < parties.size(); i++) {
            if (isValidIndex(i) && partyPermanentDamage[i] > 0) {
                Party p = parties.get(i);
                double voterShare = (double) p.getCurrentSupporterCount() / Math.max(1, totalVoters);

                double recoveryRate = BASE_RECOVERY_RATE + (voterShare * VOTER_SHARE_RECOVERY_FACTOR);

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

    private double calculateTimeFactor(int age) {
        if (age < FADE_IN_TICKS) {
            double progress = (double) age / FADE_IN_TICKS;
            return progress * progress;
        }
        return 1.0 - ((double) (age - FADE_IN_TICKS) / (SCANDAL_DURATION - FADE_IN_TICKS));
    }

    private void accumulatePermanentDamage(int index, double strength) {
        if (isValidIndex(index)) {
            double damageBuildUp = (strength * PERMANENT_DAMAGE_FACTOR) / (double) SCANDAL_DURATION;
            partyPermanentDamage[index] += damageBuildUp;
        }
    }

    private boolean isValidIndex(int index) {
        return index >= 0 && index < partyPermanentDamage.length;
    }
}
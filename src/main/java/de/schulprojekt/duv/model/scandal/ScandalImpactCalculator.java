package de.schulprojekt.duv.model.scandal;

import de.schulprojekt.duv.model.party.Party;
import java.util.Arrays;
import java.util.List;

/**
 * Berechnet die Auswirkungen von Skandalen.
 * KORREKTUR: Trennt strikt zwischen akutem Druck und Langzeitschäden,
 * um die Original-Logik (unterschiedliche Gewichtung) zu erhalten.
 */
public class ScandalImpactCalculator {

    // --- Konstanten (Konfiguration) ---
    private static final int SCANDAL_DURATION = 200;
    private static final int FADE_IN_TICKS = 20;

    // Gewichtungsfaktoren (Original-Werte)
    private static final double ACUTE_PRESSURE_FACTOR = 8.0;
    private static final double PERMANENT_DAMAGE_FACTOR = 2.5;

    // Recovery-Werte
    private static final double BASE_RECOVERY_RATE = 0.008;
    private static final double VOTER_SHARE_RECOVERY_FACTOR = 0.05;

    // --- Felder ---
    private final double[] partyPermanentDamage;

    // --- Konstruktor ---
    public ScandalImpactCalculator(int maxParties) {
        // Puffer für Sicherheit bei Index-Zugriffen
        this.partyPermanentDamage = new double[maxParties + 10];
    }

    // --- Business Logik ---

    /**
     * Berechnet NUR den akuten Druck durch laufende Skandale.
     * Der permanente Schaden wird hier als Nebeneffekt berechnet, aber nicht zurückgegeben.
     */
    public double[] calculateAcutePressure(List<ScandalEvent> activeScandals, List<Party> parties, int currentStep) {
        double[] acutePressure = new double[parties.size()];

        for (ScandalEvent event : activeScandals) {
            Party affected = event.affectedParty();
            int pIndex = parties.indexOf(affected);

            if (pIndex == -1) continue;

            int age = currentStep - event.occurredAtStep();
            if (age > SCANDAL_DURATION) continue;

            // 1. Akuten Druck berechnen
            double strength = event.scandal().strength();
            double timeFactor = calculateTimeFactor(age);

            acutePressure[pIndex] += strength * ACUTE_PRESSURE_FACTOR * timeFactor;

            // 2. Langzeitschaden aufbauen
            accumulatePermanentDamage(pIndex, strength);
        }
        return acutePressure;
    }

    public void processRecovery(List<Party> parties, int totalVoters) {
        for (int i = 1; i < parties.size(); i++) { // Index 0 (Unsicher) wird ignoriert
            if (isValidIndex(i) && partyPermanentDamage[i] > 0) {
                Party p = parties.get(i);
                double voterShare = (double) p.getCurrentSupporterCount() / Math.max(1, totalVoters);

                // Original Recovery Formel
                double recoveryRate = BASE_RECOVERY_RATE + (voterShare * VOTER_SHARE_RECOVERY_FACTOR);

                partyPermanentDamage[i] -= recoveryRate;
                if (partyPermanentDamage[i] < 0) {
                    partyPermanentDamage[i] = 0;
                }
            }
        }
    }

    // --- Getter & Setter ---

    public double getPermanentDamage(int partyIndex) {
        if (isValidIndex(partyIndex)) {
            return partyPermanentDamage[partyIndex];
        }
        return 0.0;
    }

    public void reset() {
        Arrays.fill(partyPermanentDamage, 0.0);
    }

    // --- Private Hilfsmethoden ---

    private double calculateTimeFactor(int age) {
        // 0-20 Ticks: Anstieg (Fade-In)
        if (age < FADE_IN_TICKS) {
            return (double) age / FADE_IN_TICKS;
        }
        // Danach: Langsames Abklingen (Fade-Out)
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
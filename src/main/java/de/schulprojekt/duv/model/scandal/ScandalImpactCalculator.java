package de.schulprojekt.duv.model.scandal;

import de.schulprojekt.duv.model.party.Party;
import java.util.Arrays;
import java.util.List;

/**
 * Berechnet die Auswirkungen von Skandalen.
 * UPDATE: Deutlich sanftere Kurven (Fade-In), damit Wähler nicht schlagartig fliehen.
 */
public class ScandalImpactCalculator {

    // --- Konstanten (Konfiguration) ---
    private static final int SCANDAL_DURATION = 300; // Längere Gesamtdauer (war 200)

    // WICHTIG: Das Fade-In stark verlängert (war 20).
    // Der Skandal baut sich jetzt langsam über ~90 Ticks auf, statt sofort zu explodieren.
    private static final int FADE_IN_TICKS = 90;

    // Gewichtungsfaktoren (Reduziert für weniger "Schock")
    private static final double ACUTE_PRESSURE_FACTOR = 4.0; // War 8.0 (halbiert!)
    private static final double PERMANENT_DAMAGE_FACTOR = 2.0; // War 2.5

    // Recovery-Werte
    private static final double BASE_RECOVERY_RATE = 0.005;
    private static final double VOTER_SHARE_RECOVERY_FACTOR = 0.04;

    // --- Felder ---
    private final double[] partyPermanentDamage;

    // --- Konstruktor ---
    public ScandalImpactCalculator(int maxParties) {
        this.partyPermanentDamage = new double[maxParties + 10];
    }

    // --- Business Logik ---

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

            // 2. Langzeitschaden aufbauen (passiert langsamer)
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
        // 0-90 Ticks: Langsamer Anstieg (die Nachricht verbreitet sich)
        if (age < FADE_IN_TICKS) {
            // Nutzung einer quadratischen Kurve für sanfteren Start (Ease-In)
            double progress = (double) age / FADE_IN_TICKS;
            return progress * progress;
        }
        // Danach: Langsames Abklingen
        return 1.0 - ((double) (age - FADE_IN_TICKS) / (SCANDAL_DURATION - FADE_IN_TICKS));
    }

    private void accumulatePermanentDamage(int index, double strength) {
        if (isValidIndex(index)) {
            // Langzeitschaden baut sich über die gesamte Dauer auf
            double damageBuildUp = (strength * PERMANENT_DAMAGE_FACTOR) / (double) SCANDAL_DURATION;
            partyPermanentDamage[index] += damageBuildUp;
        }
    }

    private boolean isValidIndex(int index) {
        return index >= 0 && index < partyPermanentDamage.length;
    }
}
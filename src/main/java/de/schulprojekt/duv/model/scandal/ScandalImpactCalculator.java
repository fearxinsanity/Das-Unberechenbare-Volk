package de.schulprojekt.duv.model.scandal;

import de.schulprojekt.duv.model.party.Party;
import java.util.List;

/**
 * Berechnet die Auswirkungen von Skandalen.
 * KORREKTUR: Trennt strikt zwischen akutem Druck und Langzeitschäden,
 * um die Original-Logik (unterschiedliche Gewichtung) zu erhalten.
 */
public class ScandalImpactCalculator {

    private final double[] partyPermanentDamage;
    private static final int SCANDAL_DURATION = 200;

    public ScandalImpactCalculator(int maxParties) {
        // Puffer für Sicherheit bei Index-Zugriffen
        this.partyPermanentDamage = new double[maxParties + 10];
    }

    /**
     * Berechnet NUR den akuten Druck durch laufende Skandale.
     * Der permanente Schaden wird hier NICHT addiert (passiert im VoterBehavior).
     */
    public double[] calculateAcutePressure(List<ScandalEvent> activeScandals, List<Party> parties, int currentStep) {
        double[] acutePressure = new double[parties.size()];

        for (ScandalEvent event : activeScandals) {
            Party affected = event.getAffectedParty();
            int pIndex = parties.indexOf(affected);

            if (pIndex != -1) {
                int age = currentStep - event.getOccurredAtStep();
                if (age > SCANDAL_DURATION) continue;

                double strength = event.getScandal().getStrength();

                // Original Fade-In / Fade-Out Logik
                // 0-20 Ticks: Anstieg, danach langsames Abklingen
                double timeFactor = (age < 20) ? (double) age / 20.0 : 1.0 - ((double) (age - 20) / (SCANDAL_DURATION - 20));

                // Faktor 8.0 für akuten Druck (Original-Wert)
                acutePressure[pIndex] += strength * 8.0 * timeFactor;

                // Langzeitschaden aufbauen (Faktor 2.5 / Duration)
                double damageBuildUp = (strength * 2.5) / (double) SCANDAL_DURATION;
                if (pIndex < partyPermanentDamage.length) {
                    partyPermanentDamage[pIndex] += damageBuildUp;
                }
            }
        }
        return acutePressure;
    }

    public void processRecovery(List<Party> parties, int totalVoters) {
        for (int i = 1; i < parties.size(); i++) { // Index 0 (Unsicher) ignoriert
            if (i < partyPermanentDamage.length && partyPermanentDamage[i] > 0) {
                Party p = parties.get(i);
                double voterShare = (double) p.getCurrentSupporterCount() / Math.max(1, totalVoters);

                // Original Recovery Formel
                double recoveryRate = 0.008 + (voterShare * 0.05);

                partyPermanentDamage[i] -= recoveryRate;
                if (partyPermanentDamage[i] < 0) partyPermanentDamage[i] = 0;
            }
        }
    }

    public double getPermanentDamage(int partyIndex) {
        if (partyIndex >= 0 && partyIndex < partyPermanentDamage.length) {
            return partyPermanentDamage[partyIndex];
        }
        return 0.0;
    }

    public void reset() {
        for(int i=0; i<partyPermanentDamage.length; i++) partyPermanentDamage[i] = 0;
    }
}
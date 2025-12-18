package de.schulprojekt.duv.model.scandal;

import de.schulprojekt.duv.model.party.Party;
import de.schulprojekt.duv.model.core.SimulationParameters;
import java.util.List;

public class ScandalImpactCalculator {

    private final double[] partyPermanentDamage;
    private static final int SCANDAL_DURATION = 200;

    public ScandalImpactCalculator(int maxParties) {
        this.partyPermanentDamage = new double[maxParties + 5];
    }

    public double[] calculateCurrentPressure(List<ScandalEvent> activeScandals, List<Party> parties, int currentStep) {
        double[] pressure = new double[parties.size()];

        for (ScandalEvent event : activeScandals) {
            Party affected = event.getAffectedParty();
            int pIndex = parties.indexOf(affected);

            if (pIndex != -1) {
                int age = currentStep - event.getOccurredAtStep();
                if (age > SCANDAL_DURATION) continue;

                double strength = event.getScandal().getStrength();

                // Fade-In / Fade-Out Logik aus deiner Engine
                double timeFactor = (age < 20) ? (double) age / 20.0 : 1.0 - ((double) (age - 20) / (SCANDAL_DURATION - 20));

                pressure[pIndex] += strength * 8.0 * timeFactor;

                // Langzeitschaden aufbauen
                double damageBuildUp = (strength * 2.5) / (double) SCANDAL_DURATION;
                if (pIndex < partyPermanentDamage.length) {
                    partyPermanentDamage[pIndex] += damageBuildUp;
                }
            }
        }

        // Addiere permanenten Schaden
        for (int i = 0; i < pressure.length; i++) {
            if (i < partyPermanentDamage.length) {
                pressure[i] += partyPermanentDamage[i];
            }
        }
        return pressure;
    }

    public void processRecovery(List<Party> parties, int totalVoters) {
        for (int i = 1; i < parties.size(); i++) {
            if (i < partyPermanentDamage.length && partyPermanentDamage[i] > 0) {
                Party p = parties.get(i);
                double voterShare = (double) p.getCurrentSupporterCount() / Math.max(1, totalVoters);
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
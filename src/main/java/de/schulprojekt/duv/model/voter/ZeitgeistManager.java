package de.schulprojekt.duv.model.voter;

import de.schulprojekt.duv.model.core.SimulationParameters;
import de.schulprojekt.duv.util.config.VoterBehaviorConfig;
import java.util.Random;

public class ZeitgeistManager {
    private volatile double currentZeitgeist;

    public void setZeitgeist(double zeitgeist) {
        this.currentZeitgeist = zeitgeist;
    }

    public double getCurrentZeitgeist() {
        return currentZeitgeist;
    }

    public void updateZeitgeist(SimulationParameters params, int currentStep) {
        Random rnd = new Random(params.seed() + currentStep + 1);
        double nextZeitgeist = this.currentZeitgeist;
        double change = (rnd.nextDouble() - 0.5) * VoterBehaviorConfig.ZEITGEIST_DRIFT_STRENGTH;
        nextZeitgeist += change;

        if (nextZeitgeist > VoterBehaviorConfig.ZEITGEIST_MAX_AMPLITUDE) {
            nextZeitgeist = VoterBehaviorConfig.ZEITGEIST_MAX_AMPLITUDE;
        } else if (nextZeitgeist < -VoterBehaviorConfig.ZEITGEIST_MAX_AMPLITUDE) {
            nextZeitgeist = -VoterBehaviorConfig.ZEITGEIST_MAX_AMPLITUDE;
        }

        this.currentZeitgeist = nextZeitgeist;
    }
}
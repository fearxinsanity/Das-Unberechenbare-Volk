package de.schulprojekt.duv.model.voter;

import de.schulprojekt.duv.util.config.VoterBehaviorConfig;
import java.util.Random;

/**
 * Verwaltet die globale politische Stimmung (Zeitgeist) in der Simulation.
 * Die Aktualisierung erfolgt nun nicht-deterministisch ohne Seed.
 * * @author Nico Hoffmann
 * @version 1.1
 */
public class ZeitgeistManager {

    private volatile double currentZeitgeist;
    private final Random random = new Random();

    public void setZeitgeist(double zeitgeist) {
        this.currentZeitgeist = zeitgeist;
    }

    public double getCurrentZeitgeist() {
        return currentZeitgeist;
    }

    public void updateZeitgeist() {
        double change = (random.nextDouble() - 0.5) * VoterBehaviorConfig.ZEITGEIST_DRIFT_STRENGTH;
        double nextZeitgeist = this.currentZeitgeist + change;

        if (nextZeitgeist > VoterBehaviorConfig.ZEITGEIST_MAX_AMPLITUDE) {
            nextZeitgeist = VoterBehaviorConfig.ZEITGEIST_MAX_AMPLITUDE;
        } else if (nextZeitgeist < -VoterBehaviorConfig.ZEITGEIST_MAX_AMPLITUDE) {
            nextZeitgeist = -VoterBehaviorConfig.ZEITGEIST_MAX_AMPLITUDE;
        }

        this.currentZeitgeist = nextZeitgeist;
    }
}
package de.schulprojekt.duv.model.scandal;

import de.schulprojekt.duv.model.random.DistributionProvider;

/**
 * Plant das Auftreten von Skandalen basierend auf einer Wahrscheinlichkeitsverteilung.
 */
public class ScandalScheduler {

    // --- Dependencies & State ---
    private final DistributionProvider distributionProvider;
    private double timeUntilNextScandal;

    // --- Konstruktor ---
    public ScandalScheduler(DistributionProvider distributionProvider) {
        this.distributionProvider = distributionProvider;
        reset();
    }

    // --- Business Logik ---

    /**
     * Prüft in jedem Zeitschritt, ob ein neuer Skandal ausgelöst werden soll.
     * Zählt den internen Timer herunter.
     *
     * @return true, wenn ein Skandal auftreten soll, sonst false.
     */
    public boolean shouldScandalOccur() {
        timeUntilNextScandal -= 1.0;

        if (timeUntilNextScandal <= 0) {
            scheduleNext(); // Planen für das nächste Mal
            return true;
        }
        return false;
    }

    // --- Lifecycle / Reset ---

    public void reset() {
        scheduleNext();
    }

    // --- Private Hilfsmethoden ---

    private void scheduleNext() {
        this.timeUntilNextScandal = distributionProvider.sampleTimeUntilNextScandal();
    }
}
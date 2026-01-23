package de.schulprojekt.duv.model.scandal;

import de.schulprojekt.duv.model.random.DistributionProvider;

/**
 * Plant das Auftreten von Skandalen basierend auf Wahrscheinlichkeitsverteilungen.
 * @author Nico Hoffmann
 * @version 1.0
 */
public class ScandalScheduler {

    // ========================================
    // Instance Variables
    // ========================================

    private final DistributionProvider distributionProvider;
    private double timeUntilNextScandal;

    // ========================================
    // Constructors
    // ========================================

    public ScandalScheduler(DistributionProvider distributionProvider) {
        this.distributionProvider = distributionProvider;
        reset();
    }

    // ========================================
    // Business Logic Methods
    // ========================================
    
    public boolean shouldScandalOccur() {
        timeUntilNextScandal -= 1.0;

        if (timeUntilNextScandal <= 0) {
            scheduleNext();
            return true;
        }
        return false;
    }

    public void reset() {
        scheduleNext();
    }

    // ========================================
    // Utility Methods
    // ========================================

    private void scheduleNext() {
        this.timeUntilNextScandal = distributionProvider.sampleTimeUntilNextScandal();
    }
}
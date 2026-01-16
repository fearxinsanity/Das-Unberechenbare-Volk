package de.schulprojekt.duv.model.scandal;

import de.schulprojekt.duv.model.random.DistributionProvider;

/**
 * Schedules the occurrence of scandals based on probability distributions.
 * @author Nico Hoffmann
 * @version 1.1
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

    /**
     * Checks if a new scandal should be triggered in the current step.
     * @return true if a scandal occurs
     */
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
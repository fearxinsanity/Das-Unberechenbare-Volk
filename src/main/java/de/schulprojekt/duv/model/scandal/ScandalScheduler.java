package de.schulprojekt.duv.model.scandal;

import de.schulprojekt.duv.model.random.DistributionProvider;

public class ScandalScheduler {
    private double timeUntilNextScandal;
    private final DistributionProvider distributionProvider;

    public ScandalScheduler(DistributionProvider distributionProvider) {
        this.distributionProvider = distributionProvider;
        reset();
    }

    public void reset() {
        scheduleNext();
    }

    private void scheduleNext() {
        this.timeUntilNextScandal = distributionProvider.sampleTimeUntilNextScandal();
    }

    public boolean shouldScandalOccur() {
        timeUntilNextScandal -= 1.0;
        if (timeUntilNextScandal <= 0) {
            scheduleNext();
            return true;
        }
        return false;
    }
}
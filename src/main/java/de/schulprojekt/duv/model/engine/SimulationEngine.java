package de.schulprojekt.duv.model.engine;

import de.schulprojekt.duv.model.entities.Party;
import de.schulprojekt.duv.model.entities.Voter;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SimulationEngine {

    // --- State ---
    private final List<Voter> voters;
    private final List<Party> parties;
    private SimulationParameters parameters;
    private boolean isRunning = false;
    private int currentTick = 0;
    private String latestEvent = "Simulation ready.";

    /**
     * Provides the random numbers for all distributions.
     * I use {@link SecureRandom} instead of {@code java.util.Random} to fulfill
     * the requirement for realistic, non-deterministic randomness based on OS entropy.
     */
    private final Random randomGenerator = new SecureRandom();

    public SimulationEngine() {
        this.voters = new ArrayList<>();
        this.parties = new ArrayList<>();
        this.parameters = new SimulationParameters(); // Default parameters
    }

    // --- 1. Control API (called by Controller) ---

    /**
     * Initializes or resets the simulation with new parameters.
     * This method is called when the new setting are applied from the GUI.
     * It rebuilds the lists of voters and parties.
     * @param params The settings object gathered from the GUI by the Controller.
     */
    public void setupSimulation(SimulationParameters params) {
        this.parameters = params;
        this.voters.clear();
        this.parties.clear();
        this.currentTick = 0;
        this.latestEvent = "Simulation initialized.";
        this.isRunning = false;

        // TODO: Add logic to create voters and parties
        // (based on params.getVoterCount() etc.)
    }

    public void performTick() {
        if (!isRunning) {
            return; // Do nothing if paused
        }

        // 1. Process global events (Exponential Distribution)
        processGlobalEvents();

        // 2. Process campaign effects (Uniform Distribution)
        processCampaignEffects();

        // 3. Process voter decisions (Normal Distribution)
        processVoterDecisions();

        // 4. Aggregate data (for the View)
        updatePartySupport();

        this.currentTick++;
    }

    // --- 2. Data API (called by Controller) ---

    /**
     * This is used to feed the Bar Chart with data
     * @return A list of Party objects with updated supporter counts.
     */
    public List<Party> getPartyResults() {
        // TODO: May need to return a deep copy
        // to ensure thread-safety and MVC separation.
        return this.parties;
    }

    /**
     * Provides the last significant event as a text.
     * Populates the Event Log in the GUI.
     * @return The last event string.
     */
    public String getLatestEvent() {
        return latestEvent;
    }

    public void startSimulation() {
        if (!isRunning) {
            this.isRunning = true;
            this.latestEvent = "Simulation started.";
        }
    }

    public void stopSimulation() {
        if (isRunning) {
            this.isRunning = false;
            this.latestEvent = "Simulation paused.";
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    public int getCurrentTick() {
        return currentTick;
    }

    // --- 3. Internal Simulation Logic (Stubs) ---

    private void processGlobalEvents() {
        // TODO
    }

    private void processCampaignEffects() {
        // TODO
    }

    private void processVoterDecisions() {
        // TODO: Iterate over up to 2,000,000 voters
    }

    private void updatePartySupport() {
        // TODO
    }

    // --- 4. Random Distribution Helpers (Internal) ---

    /**
     * Models events where any outcome in an interval is equally likely.
     * @param min Inclusive minimum value.
     * @param max Exclusive maximum value.
     * @return A uniformly distributed random number.
     */
    private double getUniformDistributed(double min, double max) {
        return min + (max - min) * randomGenerator.nextDouble();
    }

    /**
     * Models natural fluctuations around an average value.
     * @param mean   The mean (µ) of the distribution.
     * @param stdDev The standard deviation (σ) of the distribution.
     * @return A normally (Gaussian) distributed random number.
     */
    private double getNormalDistributed(double mean, double stdDev) {
        return mean + stdDev * randomGenerator.nextGaussian();
    }

    /**
     * Models the time between independent, random events.
     * @param lambda The rate (λ), often defined as 1 / (average time).
     * A higher rate means *shorter* average time between events.
     * @return The calculated "wait time" for the next event.
     */
    private double getExponentialDistributed(double lambda) {
        double u = randomGenerator.nextDouble() + Double.MIN_VALUE;
        return -Math.log(u) / lambda;
    }
}
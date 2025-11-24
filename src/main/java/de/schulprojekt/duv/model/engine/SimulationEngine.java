package de.schulprojekt.duv.model.engine;

import de.schulprojekt.duv.model.entities.Party;
import de.schulprojekt.duv.model.entities.Voter;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The central class of the Model (MVC). It controls the main simulation loop,
 * manages the state of all entities, and contains all business logic,
 * completely decoupled from the GUI.
 *
 */
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
     * We use {@link SecureRandom} instead of {@code java.util.Random} to fulfill
     * the requirement for "truer", non-deterministic randomness based on OS entropy.
     * 🐛 QA-Warning: This is significantly slower than standard PRNGs and must be
     * monitored during the stress test (2M voters).
     */
    private final Random randomGenerator = new SecureRandom();

    /**
     * Stores the voter transitions that occurred in the *current* tick.
     * This list is cleared and rebuilt every tick.
     */
    private final List<VoterTransition> recentTransitions;

    /**
     * Stores the remaining ticks until the next global event triggers.
     * This value is set by the exponential distribution.
     */
    private double ticksUntilNextEvent;

    /**
     * Stores the calculated "influence pull" for each party in the current tick.
     * This map is cleared and rebuilt by processCampaignEffects() every tick.
     */
    private final Map<Party, Double> campaignInfluencePerParty;

    /**
     * Stores the current party assignment for every single voter.
     * Why: This is a crucial optimization. Instead of recalculating
     * the "closest party" 8 million times per tick (2M voters * 4 parties),
     * we only calculate it when a voter *actually* changes their orientation.
     * updatePartySupport() just counts the values in this map.
     */
    private final Map<Voter, Party> voterAssignments;


    /**
     * Constructs the SimulationEngine.
     * The SimulationEngine initializes all its internal components itself.
     */
    public SimulationEngine() {
        this.voters = new ArrayList<>();
        this.parties = new ArrayList<>();
        this.parameters = new SimulationParameters();
        this.recentTransitions = new ArrayList<>();
        this.campaignInfluencePerParty = new HashMap<>();
        this.voterAssignments = new HashMap<>();
    }

    // --- 1. Control API (called by Controller) ---

    /**
     * Initializes or resets the simulation with new parameters.
     * This method is called when the user applies new settings from the GUI
     * (e.g., via "Start" or "Reset").
     * It rebuilds the lists of voters and parties.
     *
     * @param params The settings object gathered from the GUI by the Controller.
     */
    public void setupSimulation(SimulationParameters params) {
        this.parameters = params;
        this.voters.clear();
        this.parties.clear();
        this.recentTransitions.clear();
        this.campaignInfluencePerParty.clear();
        this.voterAssignments.clear();
        this.currentTick = 0;
        this.latestEvent = "Simulation initialized.";
        this.isRunning = false;

        // Initialize the event timer
        this.ticksUntilNextEvent = getExponentialDistributed(
                parameters.getScandalLambda()
        );

        // 1. Create Parties
        int partyCount = params.getPartyCount();
        for (int i = 0; i < partyCount; i++) {
            // Distribute parties evenly on a 0.0 to 1.0 scale
            double corePosition = (partyCount == 1) ? 0.5 : (double) i / (partyCount - 1);

            // Use A, B, C... for names
            String partyName = "Party " + (char) ('A' + i);

            // Get the specific budget for this party (Index i)
            double initialBudget = params.getBudgetForParty(i);

            this.parties.add(new Party(partyName, corePosition, initialBudget));
        }

        // 2. Create Voters
        int voterCount = params.getVoterCount();
        for (int i = 0; i < voterCount; i++) {

            // Create voter with a normal distribution around the center (0.5)
            // (Clamped to 0.0 - 1.0)
            double initialOrientation = clamp(
                    getNormalDistributed(0.5, 0.2), 0.0, 1.0
            );

            // Create voter mobility (susceptibility)
            // (Clamped to 0.0 - 1.0)
            double susceptibility = clamp(
                    getNormalDistributed(params.getVoterMobility(), 0.1), 0.0, 1.0
            );

            Voter voter = new Voter(i, initialOrientation, susceptibility);
            this.voters.add(voter);

            // Assign initial party
            this.voterAssignments.put(voter, findClosestParty(voter));
        }

        // 3. Calculate initial support
        updatePartySupport();
    }

    /**
     * Executes a single simulation step (tick).
     * This method acts as the "heartbeat" of the simulation.
     */
    public void performTick() {
        if (!isRunning) {
            return; // Do nothing if paused
        }

        // Clear per-tick data
        this.recentTransitions.clear();
        this.campaignInfluencePerParty.clear();

        // Decrement event timer
        this.ticksUntilNextEvent -= 1.0;

        // Run simulation phases
        processGlobalEvents();    // Exponential
        processCampaignEffects(); // Uniform
        processVoterDecisions();  // Normal

        // Recalculate results
        updatePartySupport();

        this.currentTick++;
    }

    // --- 2. Data API (called by Controller) ---

    /**
     * Helper to get the calculated influence for a party.
     * Used by processVoterDecisions().
     * @param party The party to check.
     * @return The calculated influence (pull strength) for this tick.
     */
    public double getCampaignInfluence(Party party) {
        return this.campaignInfluencePerParty.getOrDefault(party, 0.0);
    }

    /**
     * Provides the aggregated results for visualization (Bar Chart).
     *
     * @return A shallow copy of the list of Party objects.
     */
    public List<Party> getPartyResults() {
        // We return a "shallow copy" (new List, same objects) of the list.
        // Why: Prevents ConcurrentModificationExceptions and is fast,
        // which is crucial for performance on school computers.
        return new ArrayList<>(this.parties);
    }

    /**
     * Provides the last significant event as a text (Event Feed).
     * @return The last event string.
     */
    public String getLatestEvent() {
        return latestEvent;
    }

    /**
     * Provides the list of voter transitions that occurred in this tick.
     * This is used by the Controller to draw the animation.
     *
     * @return A list of {@link VoterTransition} objects for the current tick.
     */
    public List<VoterTransition> getRecentTransitions() {
        return this.recentTransitions;
    }

    // Trivial methods
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

    // --- 3. Internal Simulation Logic ---

    /**
     * Checks if a global event is due and executes it.
     * Fulfills the "Exponential Distribution" requirement.
     */
    private void processGlobalEvents() {
        if (this.ticksUntilNextEvent <= 0) {

            // Trigger event
            Party affectedParty = parties.get(
                    randomGenerator.nextInt(parties.size())
            );

            this.latestEvent = String.format(
                    "TICK %d: ⚠️ Politischer Skandal trifft %s!",
                    this.currentTick,
                    affectedParty.getName()
            );

            // Apply effect (e.g., halve budget)
            double currentBudget = affectedParty.getCampaignBudget();
            affectedParty.setCampaignBudget(currentBudget / 2.0);

            // Calculate wait time for the *next* event
            this.ticksUntilNextEvent = getExponentialDistributed(
                    parameters.getScandalLambda()
            );
        }
    }

    /**
     * Processes party campaign effects.
     * Fulfills the "Uniform Distribution" requirement.
     * This method spends party budget and calculates the "influence"
     * for this tick, storing it in the 'campaignInfluencePerParty' map.
     */
    private void processCampaignEffects() {

        final double budgetSpendRate = parameters.getBudgetSpendRatePerTick();
        final double effectivenessMin = parameters.getCampaignEffectivenessMin();
        final double effectivenessMax = parameters.getCampaignEffectivenessMax();
        final double influenceScaling = parameters.getCampaignInfluenceScalingFactor();
        for (Party party : parties) {
            double budget = party.getCampaignBudget();
            if (budget <= 0) {
                this.campaignInfluencePerParty.put(party, 0.0);
                continue;
            }

            // 1. Spend budget
            double budgetSpentThisTick = budget * budgetSpendRate;
            party.setCampaignBudget(budget - budgetSpentThisTick);

            // 2. Calculate effectiveness (Uniform Distribution)
            double effectiveness = getUniformDistributed(
                    effectivenessMin,
                    effectivenessMax
            );

            // 3. Calculate influence pull
            double influence = (budgetSpentThisTick * effectiveness) * influenceScaling;

            // 4. Store influence for processVoterDecisions()
            this.campaignInfluencePerParty.put(party, influence);
        }
    }

    /**
     * Processes voter decisions based on influences.
     * Fulfills the "Normal Distribution" requirement.
     * This is the performance hotspot and MUST be efficient.
     */
    private void processVoterDecisions() {
        // 🐛 QA-Warning: This is the performance hotspot!

        // Aggregate transitions here to avoid creating 2M objects
        Map<String, Map<String, Integer>> transitionCounts = new HashMap<>();

        // Get parameters for this tick
        double mediaStdDev = parameters.getMediaInfluence() * 0.01; // Scale 0-100% -> 0.0-0.01

        for (Voter voter : voters) {
            // 1. Check voter's susceptibility
            double susceptibility = voter.getSusceptibility();
            if (susceptibility <= 0) {
                continue; // This voter never changes
            }

            // 2. Get current state
            Party oldParty = this.voterAssignments.get(voter);
            double oldOrientation = voter.getPoliticalOrientation();

            // 3. Calculate influences

            // A. Media influence (Normal Distribution "noise")
            double mediaNudge = getNormalDistributed(0.0, mediaStdDev);

            // B. Campaign influence (Uniform Distribution "pull")
            double totalCampaignPull = 0.0;
            for (Party party : parties) {
                double influence = getCampaignInfluence(party);
                // Vector: (party_pos - voter_pos) * strength
                totalCampaignPull += (party.getCorePosition() - oldOrientation) * influence;
            }

            // 4. Apply change
            double delta = (mediaNudge + totalCampaignPull) * susceptibility;
            double newOrientation = clamp(oldOrientation + delta, 0.0, 1.0);
            voter.setPoliticalOrientation(newOrientation);

            // 5. Find new party
            Party newParty = findClosestParty(voter);

            // 6. Record transition (if changed)
            if (newParty != oldParty && oldParty != null) {
                // Aggregate in the map
                transitionCounts
                        .computeIfAbsent(oldParty.getName(), k -> new HashMap<>())
                        .merge(newParty.getName(), 1, Integer::sum);

                // Update the master assignment map
                this.voterAssignments.put(voter, newParty);
            }
        }

        // 7. Convert aggregated map to the final transition list
        for (Map.Entry<String, Map<String, Integer>> fromEntry : transitionCounts.entrySet()) {
            String fromParty = fromEntry.getKey();
            for (Map.Entry<String, Integer> toEntry : fromEntry.getValue().entrySet()) {
                String toParty = toEntry.getKey();
                Integer count = toEntry.getValue();
                this.recentTransitions.add(
                        new VoterTransition(fromParty, toParty, count)
                );
            }
        }
    }

    /**
     * Helper method to find the party closest to a voter's orientation.
     * @param voter The voter to check.
     * @return The closest Party object.
     */
    private Party findClosestParty(Voter voter) {
        Party closestParty = null;
        double minDistance = Double.MAX_VALUE;

        for (Party party : parties) {
            double distance = Math.abs(
                    voter.getPoliticalOrientation() - party.getCorePosition()
            );

            if (distance < minDistance) {
                minDistance = distance;
                closestParty = party;
            }
        }
        return closestParty; // Can be null if parties list is empty
    }

    /**
     * Updates the supporterCount of each party.
     * (Refactored, FAST version)
     * Why: Uses the 'voterAssignments' map instead of recalculating distances.
     */
    private void updatePartySupport() {
        // 1. Reset all counters
        for (Party party : parties) {
            party.setSupporterCount(0);
        }

        // 2. Fast-count based on the map
        for (Party assignedParty : this.voterAssignments.values()) {
            if (assignedParty != null) {
                assignedParty.setSupporterCount(
                        assignedParty.getSupporterCount() + 1
                );
            }
        }
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
     * @return The calculated "wait time" for the next event.
     */
    private double getExponentialDistributed(double lambda) {
        // Implemented via the inversion method.
        // We add Double.MIN_VALUE to prevent log(0).
        double u = randomGenerator.nextDouble() + Double.MIN_VALUE;
        return -Math.log(u) / lambda;
    }

    /**
     * Helper method to constrain a value to a min/max range.
     */
    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
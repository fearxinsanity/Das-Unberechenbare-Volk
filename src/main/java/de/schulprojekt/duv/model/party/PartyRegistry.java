package de.schulprojekt.duv.model.party;

import de.schulprojekt.duv.model.core.SimulationParameters;
import de.schulprojekt.duv.model.random.DistributionProvider;
import de.schulprojekt.duv.util.CSVLoader;
import de.schulprojekt.duv.util.SimulationConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Manages the list of all active parties in the simulation.
 * Handles initialization (loading from CSV) and creation of the "Undecided" party.
 */
public class PartyRegistry {

    // --- CONSTANTS (Configuration) ---
    private static final String UNDECIDED_ABBREVIATION = "UNS";
    private static final String UNDECIDED_COLOR_CODE = "#808080"; // Gray
    private static final double UNDECIDED_POSITION = 50.0;        // Center
    private static final double SPECTRUM_WIDTH = 100.0;
    private static final double POSITION_JITTER = 10.0;           // Random variance in positioning
    private static final double BASE_BUDGET_MIN = 300000.0;
    private static final double BASE_BUDGET_VARIANCE = 400000.0;

    // --- FIELDS ---
    private final List<Party> partyList = new ArrayList<>();
    private final CSVLoader csvLoader;
    private final Random random = new Random(); // Used only for position jitter

    // --- CONSTRUCTOR ---
    public PartyRegistry(CSVLoader csvLoader) {
        this.csvLoader = csvLoader;
    }

    // --- INITIALIZATION ---

    /**
     * Initializes parties based on simulation parameters.
     * Uses DistributionProvider for budget calculation to satisfy the Uniform Distribution requirement explicitly.
     * Always creates the "Undecided" party first, then fills up with random parties from CSV.
     */
    public void initializeParties(SimulationParameters params, DistributionProvider distribution) {
        partyList.clear();
        int partyCount = params.getPartyCount();

        // 1. The "Undecided" Party (Non-voters)
        Party undecided = new Party(
                SimulationConfig.UNDECIDED_NAME,
                UNDECIDED_ABBREVIATION,
                UNDECIDED_COLOR_CODE,
                UNDECIDED_POSITION,
                0.0, // No Budget
                0    // Initial count calculated later
        );
        partyList.add(undecided);

        // 2. Load random real parties from CSV
        List<PartyTemplate> templates = csvLoader.getRandomPartyTemplates(partyCount);

        for (int i = 0; i < templates.size(); i++) {
            PartyTemplate template = templates.get(i);

            // Positioning: Distribute evenly with some random jitter
            double step = SPECTRUM_WIDTH / (partyCount + 1);
            double basePos = step * (i + 1);
            double jitter = (random.nextDouble() - 0.5) * POSITION_JITTER;
            double position = Math.max(0, Math.min(100, basePos + jitter));

            // Budget: Use Uniform Distribution from provider
            double randomFactor = distribution.sampleUniform(); // Returns 0.0 to RandomRange (usually 1.0)
            double budgetVariance = BASE_BUDGET_VARIANCE * randomFactor;

            double budget = (BASE_BUDGET_MIN + budgetVariance) * params.getBudgetEffectiveness();

            // Create real party instance from template
            partyList.add(template.toParty(position, budget));
        }
    }

    // --- MAIN LOGIC ---

    /**
     * Updates the supporter counts for all parties.
     * Called by the engine after population calculation.
     */
    public void updateSupporterCounts(int[] counts) {
        // Ensure arrays match size
        int limit = Math.min(counts.length, partyList.size());

        for (int i = 0; i < limit; i++) {
            partyList.get(i).setCurrentSupporterCount(counts[i]);
        }
    }

    // --- GETTERS ---

    /**
     * Returns the list of all parties (including "Undecided" at index 0).
     */
    public List<Party> getParties() {
        return partyList;
    }
}
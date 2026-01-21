package de.schulprojekt.duv.model.party;

import de.schulprojekt.duv.model.core.SimulationParameters;
import de.schulprojekt.duv.model.random.DistributionProvider;
import de.schulprojekt.duv.util.io.CSVLoader;
import de.schulprojekt.duv.util.config.SimulationConfig;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Manages the list of active parties in the simulation.
 * @author Nico Hoffmann
 * @version 1.0
 */
public class PartyRegistry {

    // ========================================
    // Static Variables
    // ========================================

    private static final String UNDECIDED_ABBREVIATION = "UNS";
    private static final String UNDECIDED_COLOR_CODE = "#808080";
    private static final double UNDECIDED_POSITION = 50.0;
    private static final double SPECTRUM_WIDTH = 100.0;
    private static final double POSITION_JITTER = 10.0;
    private static final double BASE_BUDGET_MIN = 300000.0;
    private static final double BASE_BUDGET_VARIANCE = 400000.0;

    // ========================================
    // Instance Variables
    // ========================================

    private final List<Party> partyList = new ArrayList<>();
    private final CSVLoader csvLoader;
    private final Random random = new Random();

    // ========================================
    // Constructors
    // ========================================

    public PartyRegistry(CSVLoader csvLoader) {
        this.csvLoader = csvLoader;
    }

    // ========================================
    // Getter Methods
    // ========================================

    public List<Party> getParties() {
        return partyList;
    }

    // ========================================
    // Business Logic Methods
    // ========================================

    /**
     * Initializes parties based on parameters and templates.
     * @param params current simulation parameters
     * @param distribution provider for random sampling
     */
    public void initializeParties(SimulationParameters params, DistributionProvider distribution) {
        partyList.clear();
        int partyCount = params.partyCount();

        Set<String> usedColors = new HashSet<>();
        usedColors.add(UNDECIDED_COLOR_CODE.toLowerCase());

        Party undecided = new Party(
                SimulationConfig.UNDECIDED_NAME,
                UNDECIDED_ABBREVIATION,
                UNDECIDED_COLOR_CODE,
                UNDECIDED_POSITION,
                0.0,
                0
        );
        partyList.add(undecided);

        List<PartyTemplate> templates = csvLoader.getRandomPartyTemplates(partyCount);

        for (int i = 0; i < templates.size(); i++) {
            PartyTemplate template = templates.get(i);

            double step = SPECTRUM_WIDTH / (partyCount + 1);
            double basePos = step * (i + 1);
            double jitter = (random.nextDouble() - 0.5) * POSITION_JITTER;
            double position = Math.max(0, Math.min(100, basePos + jitter));

            double randomFactor = distribution.sampleUniform();
            double budgetVariance = BASE_BUDGET_VARIANCE * randomFactor;
            double budget = (BASE_BUDGET_MIN + budgetVariance) * params.budgetEffectiveness();

            String color = template.colorCode();
            while (usedColors.contains(color.toLowerCase())) {
                color = String.format("#%06x", random.nextInt(0xffffff + 1));
            }
            usedColors.add(color.toLowerCase());

            partyList.add(new Party(template.name(), template.abbreviation(), color, position, budget, 0));
        }
    }

    public void updateSupporterCounts(int[] counts) {
        int limit = Math.min(counts.length, partyList.size());

        for (int i = 0; i < limit; i++) {
            partyList.get(i).setCurrentSupporterCount(counts[i]);
        }
    }
}
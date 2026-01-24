package de.schulprojekt.duv.model.party;

import de.schulprojekt.duv.model.core.SimulationParameters;
import de.schulprojekt.duv.model.random.DistributionProvider;
import de.schulprojekt.duv.util.config.PartyConfig;
import de.schulprojekt.duv.util.io.CSVLoader;
import de.schulprojekt.duv.util.config.SimulationConfig;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Verwaltet die Liste der aktiven Parteien in der Simulation.
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
    private static final double UNDECIDED_BUDGET = 0.0;
    private static final int UNDECIDED_SUPPORTERS = 0;

    private static final double SPECTRUM_WIDTH = 100.0;
    private static final double POSITION_JITTER = 10.0;
    private static final double JITTER_OFFSET_FACTOR = 0.5;

    private static final double BASE_BUDGET_MIN = 300000.0;
    private static final double BASE_BUDGET_VARIANCE = 400000.0;

    private static final int COLOR_MAX_VALUE = 0xFFFFFF;
    private static final int COLOR_RANDOM_OFFSET = 1;
    private static final String HEX_COLOR_FORMAT = "#%06x";

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

    public List<Party> getTargetableParties() {
        return partyList.stream()
                .filter(p -> !p.getName().equals(SimulationConfig.UNDECIDED_NAME))
                .collect(Collectors.toList());
    }

    // ========================================
    // Business Logic Methods
    // ========================================

    /**
     * Initialisiert die Parteien basierend auf Parametern und Vorlagen.
     * @param params aktuelle Simulationsparameter
     * @param distribution Provider für Zufallsverteilungen
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
                UNDECIDED_BUDGET,
                UNDECIDED_SUPPORTERS
        );
        partyList.add(undecided);

        List<PartyTemplate> templates = csvLoader.getRandomPartyTemplates(partyCount);

        for (int i = 0; i < templates.size(); i++) {
            PartyTemplate template = templates.get(i);

            double step = SPECTRUM_WIDTH / (partyCount + 1);
            double basePos = step * (i + 1);
            double jitter = (random.nextDouble() - JITTER_OFFSET_FACTOR) * POSITION_JITTER;
            double position = Math.max(PartyConfig.MIN_POSITION,
                    Math.min(PartyConfig.MAX_POSITION, basePos + jitter));

            double randomFactor = distribution.sampleUniform();
            double budgetVariance = BASE_BUDGET_VARIANCE * randomFactor;
            double budget = (BASE_BUDGET_MIN + budgetVariance) * params.budgetEffectiveness();

            String color = template.colorCode();
            while (usedColors.contains(color.toLowerCase())) {
                color = String.format(HEX_COLOR_FORMAT, random.nextInt(COLOR_MAX_VALUE + COLOR_RANDOM_OFFSET));
            }
            usedColors.add(color.toLowerCase());

            partyList.add(template.toParty(position, budget, color));
        }
    }

    public void updateSupporterCounts(int[] counts) {
        int limit = Math.min(counts.length, partyList.size());

        for (int i = 0; i < limit; i++) {
            partyList.get(i).setCurrentSupporterCount(counts[i]);
        }
    }
}
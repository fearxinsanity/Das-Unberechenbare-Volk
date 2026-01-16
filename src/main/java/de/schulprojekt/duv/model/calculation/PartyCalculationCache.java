package de.schulprojekt.duv.model.calculation;

/**
 * Cache object holding pre-calculated party attributes for voter decisions.
 * Avoids redundant calculations during parallel voter processing.
 *
 * @param positions political position of each party (0-100 scale)
 * @param budgetScores normalized campaign budget effectiveness scores
 * @param dailyMomentum random daily performance variance per party
 * @param uniformRange chaos factor affecting decision randomness
 * @param globalMediaFactor normalized media influence (0-1 scale)
 *
 * @author Nico Hoffmann
 * @version 1.0
 */
public record PartyCalculationCache(
        double[] positions,
        double[] budgetScores,
        double[] dailyMomentum,
        double uniformRange,
        double globalMediaFactor) {}
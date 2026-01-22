package de.schulprojekt.duv.model.core;

/**
 * Zentrales Datenobjekt für die Konfiguration eines Simulationslaufs.
 * <p>
 * Dient als "Single Source of Truth" für die Initialisierung der Engine. Die Parameter sind hier
 * schreibgeschützt gebündelt, um inkonsistente Zustandsänderungen während der Laufzeit zu verhindern
 * und Thread-Safety zu garantieren.
 * </p>
 *
 * @param populationSize Bestimmt die statistische Signifikanz und die Systemlast.
 * @param mediaInfluence Modelliert die Empfänglichkeit der Wähler für externe Narrative.
 * @param volatilityRate Definiert die politische Fluktuation im System.
 * @param scandalProbability Regelt die Frequenz unvorhergesehener Störfeuer.
 * @param loyaltyAverage Legt das historische Fundament der Parteibindung fest.
 * @param tickRate Steuert den Zeitraffer der Simulation.
 * @param chaosFactor Fügt eine Prise Unvorhersehbarkeit hinzu.
 * @param partyCount Definiert die Fragmentierung des politischen Spektrums.
 * @param budgetEffectiveness Gewichtet die Macht finanzieller Ressourcen im Wahlkampf.
 * @param seed Ermöglicht deterministische Wiederholungen.
 *
 * @author Nico Hoffmann
 * @version 1.2
 */
public record SimulationParameters(
        int populationSize,
        double mediaInfluence,
        double volatilityRate,
        double scandalProbability,
        double loyaltyAverage,
        int tickRate,
        double chaosFactor,
        int partyCount,
        double budgetEffectiveness,
        long seed
) {
    public SimulationParameters {
        if (populationSize < 0) throw new IllegalArgumentException("Population cannot be negative");
        if (tickRate < 1) throw new IllegalArgumentException("Tick rate must be at least 1");
    }

    public SimulationParameters withTickRate(int newTickRate) {
        return new SimulationParameters(
                populationSize, mediaInfluence, volatilityRate, scandalProbability,
                loyaltyAverage, newTickRate, chaosFactor, partyCount, budgetEffectiveness, seed
        );
    }
}
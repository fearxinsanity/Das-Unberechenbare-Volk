package de.schulprojekt.duv.model.calculation;

/**
 * Zentraler Cache für Parteidaten pro Simulationsschritt
 * <p>
 *     Vermeidet redundante Operationen bei der parallelen Wählerverarbeitung.
 *     Garantiert konsistente Entscheidungsgrundlagen für alle Wähler.
 * </p>
 *
 * @param positions Politische Ausrichtung als Referenz für die Distanzberechnung.
 * @param budgetScores Vor genormte Budget-Werte zur direkten Gewichtung
 * @param dailyMomentum Faktor für natürliche Beliebtheitsschwankungen.
 * @param uniformRange Faktor für Unvorhersehbarkeit der Entscheidung.
 * @param globalMediaFactor Aktuelle Wirksamkeit der Medien.
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
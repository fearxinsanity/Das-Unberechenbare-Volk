package de.schulprojekt.duv.model.calculation;

/**
 * Fasst die Bewertung einer Partei durch einen Wähler zusammen.
 * Dient als unveränderliche Entscheidungsgrundlage, um die attraktivste Partei zu ermitteln.
 *
 * @param partyIndex Referenz auf die Partei, um das Ergebnis später der richtigen Partei zuordnen zu können.
 * @param distanceScore Score für die politische Nähe. Ein höherer Wert bedeutet eine größere inhaltliche Übereinstimmung.
 * @param budgetScore Spiegelt den Einfluss der Wahlkampagne wider.
 * @param penalty Summe aller Punktabzüge durch Skandale, die die Attraktivität der Partei direkt mindern.
 * @param finalScore Der finale Vergleichswert inklusive Zufallsrauschen. Bestimmt über Wahlentscheidung.
 *
 * @author Nico Hoffmann
 * @version 1.0
 */
public record PartyEvaluationResult(
        int partyIndex,
        double distanceScore,
        double budgetScore,
        double penalty,
        double finalScore
) {
}

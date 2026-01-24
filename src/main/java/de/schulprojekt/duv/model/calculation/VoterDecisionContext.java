package de.schulprojekt.duv.model.calculation;

import de.schulprojekt.duv.model.voter.VoterType;

/**
 * Kapselt alle relevanten Kontextinformationen,
 * die für die Parteiwahl des Wählers notwendig sind.
 *
 * @param position Die politische Position des Wählers.
 * @param loyalty Der Loyalitätswert des Wählers gegenüber seiner aktuellen Partei.
 * @param mediaInfluence Die Anfälligkeit des Wählers für Medieneinfluss.
 * @param voterType Der wissenschaftliche Wählertyp
 * @param currentPartyIndex Der Index der aktuellen Partei des Wählers
 * @param currentPenalty Der aktuelle Wert, der auf der aktuellen Partei lastet.
 *
 * @author Nico Hoffmann
 * @version 1.0
 */
public record VoterDecisionContext(
        float position,
        float loyalty,
        float mediaInfluence,
        VoterType voterType,
        int currentPartyIndex,
        double currentPenalty
) {
}

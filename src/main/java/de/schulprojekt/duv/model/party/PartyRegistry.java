package de.schulprojekt.duv.model.party;

import de.schulprojekt.duv.model.core.SimulationParameters;
import de.schulprojekt.duv.model.party.Party;
import de.schulprojekt.duv.model.party.PartyTemplate;
import de.schulprojekt.duv.util.CSVLoader;
import de.schulprojekt.duv.util.SimulationConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Verwaltet die Liste aller aktiven Parteien in der Simulation.
 * Kümmert sich um die Initialisierung (Laden aus CSV) und die Erstellung der "Unsicher"-Partei.
 */
public class PartyRegistry {

    private final List<Party> partyList = new ArrayList<>();
    private final CSVLoader csvLoader;
    private final Random random = new Random();

    public PartyRegistry(CSVLoader csvLoader) {
        this.csvLoader = csvLoader;
    }

    /**
     * Initialisiert die Parteien basierend auf den Parametern.
     * Erstellt immer zuerst die "Unsicher"-Partei und füllt dann mit zufälligen Parteien aus der CSV auf.
     */
    public void initializeParties(SimulationParameters params) {
        partyList.clear();
        int partyCount = params.getNumberOfParties();

        // 1. Die "Unsicher"-Partei (Nichtwähler/Unentschlossene)
        // Wir nutzen hier Konstanten aus SimulationConfig, falls vorhanden, oder Fallback-Werte.
        String undecidedName = SimulationConfig.UNDECIDED_NAME; // "Unsicher"
        // Da Party jetzt String colorCode erwartet, wandeln wir die Color aus Config evtl. um oder nutzen String
        String undecidedColor = "#808080"; // Grau für Unsicher

        Party undecided = new Party(
                undecidedName,
                "UNS",           // Kürzel
                undecidedColor,
                50.0,            // Politische Position (Mitte)
                0.0,             // Kein Budget
                0                // Initiale Wähler (wird später berechnet)
        );
        partyList.add(undecided);

        // 2. Zufällige echte Parteien aus CSV laden
        List<PartyTemplate> templates = csvLoader.getRandomPartyTemplates(partyCount);

        for (int i = 0; i < templates.size(); i++) {
            PartyTemplate template = templates.get(i);

            // Zufällige Positionierung im politischen Spektrum
            // Wir verteilen sie gleichmäßig mit etwas Zufall ("Jitter")
            double step = 100.0 / (partyCount + 1);
            double basePos = step * (i + 1);
            double jitter = (random.nextDouble() - 0.5) * 10.0; // +/- 5.0
            double position = Math.max(0, Math.min(100, basePos + jitter));

            // Budget berechnen (Basis + Zufall) * Faktor
            double baseBudget = 300000.0 + random.nextDouble() * 400000.0;
            double budget = baseBudget * params.getCampaignBudgetFactor();

            // Erstelle die echte Party-Instanz aus dem Template
            partyList.add(template.toParty(position, budget));
        }
    }

    /**
     * Aktualisiert die Wählerzahlen aller Parteien.
     * Wird von der Engine aufgerufen, nachdem die Population berechnet wurde.
     */
    public void updateSupporterCounts(int[] counts) {
        // Sicherstellen, dass die Arrays zusammenpassen
        int limit = Math.min(counts.length, partyList.size());

        for (int i = 0; i < limit; i++) {
            partyList.get(i).setCurrentSupporterCount(counts[i]);
        }
    }

    /**
     * Gibt die Liste aller Parteien zurück (inkl. "Unsicher" an Index 0).
     */
    public List<Party> getParties() {
        return partyList;
    }
}
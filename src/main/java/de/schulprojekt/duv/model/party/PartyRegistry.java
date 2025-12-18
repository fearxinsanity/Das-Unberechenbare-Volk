package de.schulprojekt.duv.model.party;

import de.schulprojekt.duv.model.party.Party;
import de.schulprojekt.duv.model.party.PartyTemplate;
import de.schulprojekt.duv.model.core.SimulationParameters;
import de.schulprojekt.duv.util.CSVLoader;
import de.schulprojekt.duv.util.SimulationConfig;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

public class PartyRegistry {

    private final List<Party> partyList = new CopyOnWriteArrayList<>();
    private final CSVLoader csvLoader;
    private final Random random = new Random();

    public PartyRegistry(CSVLoader csvLoader) {
        this.csvLoader = csvLoader;
    }

    public void initializeParties(SimulationParameters params) {
        partyList.clear();
        int partyCount = params.getNumberOfParties();

        // 1. Unsicher-Partei
        Party undecided = new Party(
                SimulationConfig.UNDECIDED_NAME,
                SimulationConfig.UNDECIDED_NAME,
                SimulationConfig.UNDECIDED_COLOR,
                SimulationConfig.UNDECIDED_POSITION,
                0, 0
        );
        partyList.add(undecided);

        // 2. Echte Parteien
        List<PartyTemplate> templates = csvLoader.getRandomPartyTemplates(partyCount);
        for (int i = 0; i < partyCount; i++) {
            PartyTemplate template = templates.get(i);

            // Positionierung (Logik aus deiner Engine übernommen)
            double pos = Math.max(5, Math.min(95, (100.0 / (partyCount + 1)) * (i + 1) + (random.nextDouble() - 0.5) * 10));

            double baseBudget = 300000.0 + random.nextDouble() * 400000.0;
            double budget = baseBudget * params.getCampaignBudgetFactor();

            partyList.add(template.toParty(pos, budget));
        }
    }

    public void updateSupporterCounts(int[] counts) {
        for(int i=0; i<counts.length; i++) {
            if (i < partyList.size()) {
                partyList.get(i).setCurrentSupporterCount(counts[i]);
            }
        }
    }

    public List<Party> getParties() {
        return partyList;
    }
}
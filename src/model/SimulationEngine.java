package model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SimulationEngine {
    private List<Party> parties;
    private List<Voter> voters;

    public SimulationEngine(int numberOfVoters, int numberOfParties) {
        this.parties = new ArrayList<>(numberOfParties);
        this.voters = new ArrayList<>(numberOfVoters);

        for (int i = 0; i < numberOfParties; i++) {
            parties.add(new Party("Party " + (i + 1), "Ideology " + (i + 1), 100000.0, 0));
        }

        Random rand = new Random();
        for (int i = 0; i < numberOfVoters; i++) {
            Party initialPreference = parties.get(rand.nextInt(parties.size()));
            double opinionStrength = rand.nextDouble();
            double susceptibilityToInfluence = rand.nextDouble();
            voters.add(new Voter(initialPreference, opinionStrength, susceptibilityToInfluence));
        }
    }

    public void runSimulation(int numberOfSteps) {
        for (int step = 0; step < numberOfSteps; step++) {
            for (Voter voter : voters) {
                voter.updatePreference(parties);
            }

            for (Party party : parties) {
                party.setSupporter(0);
            }

            for (Voter voter : voters) {
                Party preferredParty = voter.getCurrentPreference();
                preferredParty.setSupporter(preferredParty.getSupporter() + 1);
            }
        }
    }

    public List<Party> getParties() {
        return parties;
    }
}

package de.schulprojekt.duv.model.engine;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;

import de.schulprojekt.duv.model.entities.Voter;
import de.schulprojekt.duv.model.entities.Party;

public class SimulationEngine {

    // --- Verteilungen (Normalverteilung, Gleichverteilung) ---
    private NormalDistribution normalDistribution;
    private UniformRealDistribution uniformRealDistribution;

    private final List<Voter> voterList;
    private final List<Party> partyList;

    private SimulationParameters parameters;
    private Random random = new Random();

    public SimulationEngine(SimulationParameters params) {
        this.parameters = params;
        this.voterList = new ArrayList<>();
        this.partyList = new ArrayList<>();

        // Normalverteilung für Loyalität (Mittelwert aus Parameter, Standardabweichung 15)
        this.normalDistribution = new NormalDistribution(
                params.getInitialLoyaltyMean(),
                15.0
        );

        // Gleichverteilung für Kampagnen-Effektivität
        this.uniformRealDistribution = new UniformRealDistribution(
                0.0,
                params.getUniformRandomRange()
        );
    }

    // --- VERTEILUNGS-GENERATOREN ---

    /**
     * Generiert einen Loyalitätswert mit Normalverteilung.
     * Mittelwert: initialLoyaltyMean, Standardabweichung: 15
     */
    public double generateLoyaltyValue() {
        double value = normalDistribution.sample();
        // Auf Bereich 0-100 begrenzen
        return Math.max(0, Math.min(100, value));
    }

    /**
     * Generiert Kampagnen-Effektivität mit Gleichverteilung.
     * Bereich: 0 bis uniformRandomRange
     */
    public double generateCampaignEffectiveness() {
        return uniformRealDistribution.sample();
    }

    // --- INITIALIZATION / RESET ---
    public void initializeSimulation() {
        int partyCount = parameters.getNumberOfParties();
        if (partyCount == 0) {
            return;
        }

        String[] partyColors = {"007bff", "dc3545", "ffc107", "28a745", "6f42c1", "20c997", "fd7e14", "6c757d"};

        // 1. Parteien erstellen
        for (int i = 0; i < partyCount; i++) {
            String name = "Partei " + (char) ('A' + i);
            double position;

            if (partyCount <= 1) {
                position = 50.0;
            } else {
                position = 100.0 / (partyCount - 1) * i;
            }

            String color = partyColors[i % partyColors.length];
            double budget = 500000.0;

            Party party = new Party(name, color, position, budget, 0);
            this.partyList.add(party);
        }

        // 2. Wähler erstellen mit Normalverteilung für politische Position
        int totalVoters = parameters.getTotalVoterCount();
        NormalDistribution positionDistribution = new NormalDistribution(50.0, 25.0);

        for (int i = 0; i < totalVoters; i++) {
            // Zufällige Partei zuweisen (gleichverteilt)
            Party initialParty = this.partyList.get(random.nextInt(partyCount));

            // Loyalität mit Normalverteilung
            double loyalty = generateLoyaltyValue();

            // Politische Position mit Normalverteilung (Mitte bei 50, Streuung 25)
            double politicalPosition = positionDistribution.sample();
            politicalPosition = Math.max(0, Math.min(100, politicalPosition));

            // Medien-Beeinflussbarkeit gleichverteilt
            double mediaInfluenceability = random.nextDouble();

            Voter voter = new Voter(initialParty, loyalty, politicalPosition, mediaInfluenceability);
            this.voterList.add(voter);

            initialParty.setCurrentSupporterCount(initialParty.getCurrentSupporterCount() + 1);
        }
    }

    public void resetState() {
        this.voterList.clear();
        this.partyList.clear();
        initializeSimulation();
    }

    public void updateParameters(SimulationParameters newParams) {
        this.parameters = newParams;

        this.normalDistribution = new NormalDistribution(
                newParams.getInitialLoyaltyMean(),
                15.0
        );
        this.uniformRealDistribution = new UniformRealDistribution(
                0.0,
                newParams.getUniformRandomRange()
        );
    }

    // --- SIMULATION STEP LOGIC ---

    /**
     * Berechnet die Attraktivität einer Partei für einen Wähler.
     * Basierend auf: politischer Distanz, Kampagnen-Budget und Medien-Beeinflussbarkeit.
     */
    private double getPartyTargetScore(Voter voter, Party party, double campaignEffectiveness) {
        // Je näher die politische Position, desto höher der Score
        double distance = Math.abs(voter.getPoliticalPosition() - party.getPoliticalPosition());
        double proximityScore = 100.0 / (distance + 1.0);

        // Kampagnen-Einfluss (Budget * Effektivität * Beeinflussbarkeit)
        double campaignInfluence = (party.getCampaignBudget() / 100000.0)
                * campaignEffectiveness
                * voter.getMediaInfluenceability()
                * (parameters.getGlobalMediaInfluence() / 100.0);

        // Loyalitäts-Bonus wenn bereits bei dieser Partei
        double loyaltyBonus = 0;
        if (voter.getCurrentParty().equals(party)) {
            loyaltyBonus = voter.getPartyLoyalty() * 0.5;
        }

        return proximityScore + campaignInfluence + loyaltyBonus;
    }

    /**
     * Führt einen Simulationsschritt aus.
     * Keine Skandale - nur normale Wähler-Mobilität.
     */
    public List<VoterTransition> runSimulationStep() {
        List<VoterTransition> transitions = new ArrayList<>();

        // Mobilität bestimmt die Wahrscheinlichkeit, dass ein Wähler wechselt
        double baseMobility = parameters.getBaseMobilityRate() / 100.0;

        for (Voter voter : voterList) {
            // Wechsel-Wahrscheinlichkeit basierend auf Mobilität und inverser Loyalität
            double switchProbability = baseMobility * (1.0 - voter.getPartyLoyalty() / 100.0) * voter.getMediaInfluenceability();

            if (random.nextDouble() < switchProbability) {
                Party currentParty = voter.getCurrentParty();
                Party bestTargetParty = currentParty;
                double highestScore = -1.0;

                // Kampagnen-Effektivität für diesen Tick (Gleichverteilung)
                double campaignEffectiveness = generateCampaignEffectiveness();

                // Beste Partei finden
                for (Party targetParty : partyList) {
                    double score = getPartyTargetScore(voter, targetParty, campaignEffectiveness);
                    if (score > highestScore) {
                        highestScore = score;
                        bestTargetParty = targetParty;
                    }
                }

                // Partei wechseln wenn bessere gefunden
                if (!currentParty.equals(bestTargetParty)) {
                    voter.setCurrentParty(bestTargetParty);
                    currentParty.setCurrentSupporterCount(currentParty.getCurrentSupporterCount() - 1);
                    bestTargetParty.setCurrentSupporterCount(bestTargetParty.getCurrentSupporterCount() + 1);
                    transitions.add(new VoterTransition(currentParty, bestTargetParty));
                }
            }
        }

        return transitions;
    }

    // --- GETTERS ---
    public List<Voter> getVoters() {
        return Collections.unmodifiableList(this.voterList);
    }

    public List<Party> getParties() {
        return Collections.unmodifiableList(this.partyList);
    }

    public SimulationParameters getParameters() {
        return parameters;
    }
}
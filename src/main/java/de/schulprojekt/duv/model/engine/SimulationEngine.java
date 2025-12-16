package de.schulprojekt.duv.model.engine;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;

import de.schulprojekt.duv.model.entities.Party;
import de.schulprojekt.duv.model.entities.PartyTemplate;
import de.schulprojekt.duv.model.entities.Scandal;
import de.schulprojekt.duv.model.entities.Voter;
import de.schulprojekt.duv.util.CSVLoader;

/**
 * Die zentrale Simulations-Engine.
 *
 * Verwendet drei verschiedene Zufallsverteilungen:
 * 1. Normalverteilung - Wähler-Loyalität und politische Positionen
 * 2. Gleichverteilung - Kampagnen-Effektivität
 * 3. Exponentialverteilung - Zeitpunkt von Skandalen (Poisson-Prozess)
 */
public class SimulationEngine {

    // --- Verteilungen ---
    private NormalDistribution normalDistribution;
    private UniformRealDistribution uniformRealDistribution;
    private ExponentialDistribution exponentialDistribution;

    // --- Daten ---
    private final List<Voter> voterList;
    private final List<Party> partyList;
    private Party undecidedParty; // Der graue "Unsicher"-Ball

    private final CSVLoader csvLoader;
    private SimulationParameters parameters;
    private Random random = new Random();

    // --- Skandal-Tracking ---
    private double timeUntilNextScandal;
    private int currentStep = 0;
    private List<ScandalEvent> activeScandals = new ArrayList<>();
    private ScandalEvent lastScandal = null;

    // --- Konstanten ---
    private static final String UNDECIDED_COLOR = "6c757d"; // Grau
    private static final String UNDECIDED_NAME = "Unsicher";
    private static final double SCANDAL_DURATION_STEPS = 50; // Wie lange ein Skandal wirkt

    public SimulationEngine(SimulationParameters params) {
        this.parameters = params;
        this.voterList = new ArrayList<>();
        this.partyList = new ArrayList<>();
        this.csvLoader = new CSVLoader();

        initializeDistributions();
        scheduleNextScandal();
    }

    private void initializeDistributions() {
        // Normalverteilung für Loyalität (Mittelwert aus Parameter, Standardabweichung 15)
        this.normalDistribution = new NormalDistribution(
                parameters.getInitialLoyaltyMean(),
                15.0
        );

        // Gleichverteilung für Kampagnen-Effektivität
        this.uniformRealDistribution = new UniformRealDistribution(
                0.0,
                parameters.getUniformRandomRange()
        );

        // Exponentialverteilung für Skandal-Zeitpunkte
        // Lambda = Skandal-Chance pro Tick (je höher, desto häufiger)
        double scandalLambda = parameters.getScandalChance() / 100.0;
        if (scandalLambda <= 0) scandalLambda = 0.01;
        this.exponentialDistribution = new ExponentialDistribution(1.0 / scandalLambda);
    }

    /**
     * Plant den nächsten Skandal basierend auf Exponentialverteilung.
     */
    private void scheduleNextScandal() {
        // Exponentialverteilung gibt die Wartezeit bis zum nächsten Ereignis
        this.timeUntilNextScandal = exponentialDistribution.sample();
    }

    // --- VERTEILUNGS-GENERATOREN ---

    /**
     * Generiert einen Loyalitätswert mit Normalverteilung.
     */
    public double generateLoyaltyValue() {
        double value = normalDistribution.sample();
        return Math.max(0, Math.min(100, value));
    }

    /**
     * Generiert Kampagnen-Effektivität mit Gleichverteilung.
     */
    public double generateCampaignEffectiveness() {
        return uniformRealDistribution.sample();
    }

    /**
     * Prüft ob ein Skandal eintritt (Exponentialverteilung).
     */
    public boolean shouldScandalOccur() {
        timeUntilNextScandal -= 1.0;
        if (timeUntilNextScandal <= 0) {
            scheduleNextScandal();
            return true;
        }
        return false;
    }

    // --- INITIALIZATION / RESET ---

    public void initializeSimulation() {
        partyList.clear();
        voterList.clear();
        activeScandals.clear();
        lastScandal = null;
        currentStep = 0;

        int partyCount = parameters.getNumberOfParties();
        if (partyCount == 0) return;

        // 1. "Unsicher"-Partei erstellen (immer in der Mitte)
        undecidedParty = new Party(UNDECIDED_NAME, UNDECIDED_COLOR, 50.0, 0, 0);
        partyList.add(undecidedParty);

        // 2. Echte Parteien aus CSV laden
        List<PartyTemplate> templates = csvLoader.getRandomPartyTemplates(partyCount);

        // Zufällige Parteistärken generieren (für ungleiche Anfangsverteilung)
        double[] partyWeights = new double[partyCount];
        double totalWeight = 0;
        for (int i = 0; i < partyCount; i++) {
            // Jede Partei bekommt ein zufälliges Gewicht zwischen 0.3 und 1.0
            partyWeights[i] = 0.3 + random.nextDouble() * 0.7;
            totalWeight += partyWeights[i];
        }
        // Normalisieren
        for (int i = 0; i < partyCount; i++) {
            partyWeights[i] /= totalWeight;
        }

        for (int i = 0; i < partyCount; i++) {
            PartyTemplate template = templates.get(i);

            // Politische Position mit mehr Zufall verteilen
            double basePosition = (100.0 / (partyCount + 1)) * (i + 1);
            // Zufällige Abweichung von ±15
            double positionJitter = (random.nextDouble() - 0.5) * 30.0;
            double position = Math.max(5, Math.min(95, basePosition + positionJitter));

            // Verschiebe weg von 50 (Mitte = Unsicher)
            if (Math.abs(position - 50.0) < 10.0) {
                position += (position < 50.0) ? -15.0 : 15.0;
            }

            // Zufälliges Budget zwischen 300k und 700k
            double budget = 300000.0 + random.nextDouble() * 400000.0;
            Party party = template.toParty(position, budget);
            this.partyList.add(party);
        }

        // 3. Wähler erstellen mit ZUFÄLLIGER Verteilung
        int totalVoters = parameters.getTotalVoterCount();
        NormalDistribution positionDistribution = new NormalDistribution(50.0, 25.0);

        // Anfangs sind ca. 15-25% unsicher (zufällig)
        double undecidedRatio = 0.15 + random.nextDouble() * 0.10;
        int undecidedCount = (int)(totalVoters * undecidedRatio);

        for (int i = 0; i < totalVoters; i++) {
            Party initialParty;

            if (i < undecidedCount) {
                initialParty = undecidedParty;
            } else {
                // Gewichtete zufällige Auswahl basierend auf partyWeights
                double rand = random.nextDouble();
                double cumulative = 0;
                int selectedIndex = 0;
                for (int j = 0; j < partyCount; j++) {
                    cumulative += partyWeights[j];
                    if (rand <= cumulative) {
                        selectedIndex = j;
                        break;
                    }
                }
                initialParty = this.partyList.get(selectedIndex + 1); // +1 weil Index 0 = Unsicher
            }

            // Loyalität mit Normalverteilung (mehr Variation)
            double loyalty = generateLoyaltyValue();

            // Politische Position mit Normalverteilung
            double politicalPosition = positionDistribution.sample();
            politicalPosition = Math.max(0, Math.min(100, politicalPosition));

            // Medienbeeinflussbarkeit mit mehr Variation
            double mediaInfluenceability = Math.pow(random.nextDouble(), 0.7); // Leicht nach oben verschoben

            Voter voter = new Voter(initialParty, loyalty, politicalPosition, mediaInfluenceability);
            this.voterList.add(voter);
            initialParty.setCurrentSupporterCount(initialParty.getCurrentSupporterCount() + 1);
        }

        scheduleNextScandal();
    }

    public void resetState() {
        initializeSimulation();
    }

    public void updateParameters(SimulationParameters newParams) {
        this.parameters = newParams;
        initializeDistributions();
    }

    // --- SIMULATION STEP LOGIC ---

    /**
     * Berechnet die Attraktivität einer Partei für einen Wähler.
     */
    private double getPartyTargetScore(Voter voter, Party party, double campaignEffectiveness) {
        // Unsichere Wähler haben keinen Kampagnen-Einfluss
        if (party.getName().equals(UNDECIDED_NAME)) {
            return 30.0; // Basis-Attraktivität für Unsicherheit
        }

        double distance = Math.abs(voter.getPoliticalPosition() - party.getPoliticalPosition());
        double proximityScore = 100.0 / (distance + 1.0);

        double campaignInfluence = (party.getCampaignBudget() / 100000.0)
                * campaignEffectiveness
                * voter.getMediaInfluenceability()
                * (parameters.getGlobalMediaInfluence() / 100.0);

        // Skandal-Malus
        double scandalPenalty = 0;
        for (ScandalEvent event : activeScandals) {
            if (event.getAffectedParty().equals(party)) {
                int stepsSinceScandal = currentStep - event.getOccurredAtStep();
                double decayFactor = Math.max(0, 1.0 - (stepsSinceScandal / SCANDAL_DURATION_STEPS));
                scandalPenalty += event.getScandal().getStrength() * 50.0 * decayFactor;
            }
        }

        double loyaltyBonus = 0;
        if (voter.getCurrentParty().equals(party)) {
            loyaltyBonus = voter.getPartyLoyalty() * 0.5;
        }

        return Math.max(0, proximityScore + campaignInfluence + loyaltyBonus - scandalPenalty);
    }

    /**
     * Führt einen Simulationsschritt aus.
     */
    public List<VoterTransition> runSimulationStep() {
        currentStep++;
        List<VoterTransition> transitions = new ArrayList<>();

        // Alte Skandale aufräumen
        activeScandals.removeIf(e -> currentStep - e.getOccurredAtStep() > SCANDAL_DURATION_STEPS);

        // Skandal-Check mit Exponentialverteilung
        if (shouldScandalOccur() && partyList.size() > 1) {
            // Wähle zufällige echte Partei (nicht Unsicher)
            List<Party> realParties = partyList.stream()
                    .filter(p -> !p.getName().equals(UNDECIDED_NAME))
                    .toList();

            if (!realParties.isEmpty()) {
                Party targetParty = realParties.get(random.nextInt(realParties.size()));
                Scandal scandal = csvLoader.getRandomScandal();
                ScandalEvent event = new ScandalEvent(scandal, targetParty, currentStep);
                activeScandals.add(event);
                lastScandal = event;
            }
        }

        // WICHTIG: Konstante Basis-Mobilität (kein Abfall über Zeit!)
        double baseMobility = parameters.getBaseMobilityRate() / 100.0;

        for (Voter voter : voterList) {
            Party currentParty = voter.getCurrentParty();

            // Basis-Wechselwahrscheinlichkeit
            double switchProbability = baseMobility * (1.0 - voter.getPartyLoyalty() / 200.0)
                    * voter.getMediaInfluenceability();

            // Unsichere Wähler haben höhere Wechsel-Chance
            if (currentParty.getName().equals(UNDECIDED_NAME)) {
                switchProbability *= 1.5; // 50% höher
            }

            // Skandal erhöht Wechselwahrscheinlichkeit für betroffene Partei
            for (ScandalEvent event : activeScandals) {
                if (event.getAffectedParty().equals(currentParty)) {
                    switchProbability *= (1.0 + event.getScandal().getStrength());
                }
            }

            if (random.nextDouble() < switchProbability) {
                double campaignEffectiveness = generateCampaignEffectiveness();

                Party targetParty;

                // Logik: Meistens erst zu "Unsicher", dann zu neuer Partei
                if (!currentParty.getName().equals(UNDECIDED_NAME)
                        && random.nextDouble() < SimulationParameters.UNDECIDED_TRANSITION_RATE) {
                    // Wechsel zu "Unsicher"
                    targetParty = undecidedParty;
                } else {
                    // Wechsel zu bester Partei
                    targetParty = currentParty;
                    double highestScore = -1.0;

                    for (Party party : partyList) {
                        if (party.equals(currentParty)) continue;

                        double score = getPartyTargetScore(voter, party, campaignEffectiveness);
                        // Etwas Zufall hinzufügen für mehr Dynamik
                        score *= (0.8 + random.nextDouble() * 0.4);

                        if (score > highestScore) {
                            highestScore = score;
                            targetParty = party;
                        }
                    }
                }

                if (!currentParty.equals(targetParty)) {
                    voter.setCurrentParty(targetParty);
                    currentParty.setCurrentSupporterCount(currentParty.getCurrentSupporterCount() - 1);
                    targetParty.setCurrentSupporterCount(targetParty.getCurrentSupporterCount() + 1);
                    transitions.add(new VoterTransition(currentParty, targetParty));
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

    public Party getUndecidedParty() {
        return undecidedParty;
    }

    public SimulationParameters getParameters() {
        return parameters;
    }

    public ScandalEvent getLastScandal() {
        ScandalEvent scandal = lastScandal;
        lastScandal = null; // Nur einmal zurückgeben
        return scandal;
    }

    public List<ScandalEvent> getActiveScandals() {
        return Collections.unmodifiableList(activeScandals);
    }

    public int getCurrentStep() {
        return currentStep;
    }
}
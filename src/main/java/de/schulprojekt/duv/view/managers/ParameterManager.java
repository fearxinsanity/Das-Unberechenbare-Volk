package de.schulprojekt.duv.view.managers;

import de.schulprojekt.duv.model.core.SimulationParameters;
import de.schulprojekt.duv.util.validation.ParameterValidator;
import de.schulprojekt.duv.util.validation.ValidationMessage;
import de.schulprojekt.duv.view.Main;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

import java.util.Locale;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages all parameter input fields, validation, and formatting.
 * Handles text fields, sliders, and parameter synchronization with the simulation engine.
 * Provides input validation, formatting with locale support, and randomization features.
 * Automatically clamps values to allowed ranges on input.
 *
 * @author Nico Hoffmann
 * @version 1.5
 */
public class ParameterManager {

    // ========================================
    // Static Variables
    // ========================================

    private static final Logger LOGGER = Logger.getLogger(ParameterManager.class.getName());
    private static final double DEFAULT_BUDGET = 500000.0;

    // ========================================
    // Instance Variables
    // ========================================

    // Fields are final as they are initialized in the constructor and never reassigned
    private final TextField voterCountField;
    private final TextField partyCountField;
    private final TextField budgetField;
    private final TextField scandalChanceField;
    private final TextField seedField;

    private final Slider mediaInfluenceSlider;
    private final Slider mobilityRateSlider;
    private final Slider loyaltyMeanSlider;
    private final Slider randomRangeSlider;

    private boolean isUpdatingInternal = false;

    // ========================================
    // Constructors
    // ========================================

    /**
     * Constructs a ParameterManager with all required UI components.
     *
     * @param voterCountField the text field for voter population input
     * @param partyCountField the text field for party count input
     * @param budgetField the text field for budget input
     * @param scandalChanceField the text field for scandal probability input
     * @param seedField the text field for simulation seed input
     * @param mediaInfluenceSlider the slider for media influence adjustment
     * @param mobilityRateSlider the slider for voter mobility adjustment
     * @param loyaltyMeanSlider the slider for loyalty average adjustment
     * @param randomRangeSlider the slider for chaos factor adjustment
     */
    public ParameterManager(
            TextField voterCountField,
            TextField partyCountField,
            TextField budgetField,
            TextField scandalChanceField,
            TextField seedField,
            Slider mediaInfluenceSlider,
            Slider mobilityRateSlider,
            Slider loyaltyMeanSlider,
            Slider randomRangeSlider
    ) {
        this.voterCountField = voterCountField;
        this.partyCountField = partyCountField;
        this.budgetField = budgetField;
        this.scandalChanceField = scandalChanceField;
        this.seedField = seedField;
        this.mediaInfluenceSlider = mediaInfluenceSlider;
        this.mobilityRateSlider = mobilityRateSlider;
        this.loyaltyMeanSlider = loyaltyMeanSlider;
        this.randomRangeSlider = randomRangeSlider;
    }

    // ========================================
    // Business Logic Methods
    // ========================================

    /**
     * Initializes all input fields with event handlers and formatters.
     * Sets up input validation, focus listeners, and automatic formatting.
     * Also configures slider bounds from ParameterValidator.
     */
    public void initializeFields() {
        // Berechne maximale Längen basierend auf ParameterValidator + Puffer für Formatierung
        int maxPopLength = String.valueOf(ParameterValidator.getMaxPopulation()).length() + 3; // +3 für Trennzeichen
        int maxPartyLength = String.valueOf(ParameterValidator.getMaxParties()).length();
        // Erhöht für 500 Mio. Limit (ca. 11 Ziffern + Punkte = 15 Zeichen Puffer)
        int maxBudgetLength = 15;
        int maxScandalLength = 5;
        int maxSeedLength = 20;

        setupInteractiveField(voterCountField, maxPopLength);
        setupInteractiveField(partyCountField, maxPartyLength);
        setupInteractiveField(budgetField, maxBudgetLength); // Hier wird das Budget-Feld eingerichtet
        setupInteractiveField(scandalChanceField, maxScandalLength);

        applyInputFilter(seedField, maxSeedLength);
        seedField.setOnAction(e -> applyMasterSeed());
        seedField.focusedProperty().addListener((obs, old, isNowFocused) -> {
            if (!isNowFocused) applyMasterSeed();
        });

        mediaInfluenceSlider.setMin(ParameterValidator.getMinPercentage());
        mediaInfluenceSlider.setMax(ParameterValidator.getMaxPercentage());
        mediaInfluenceSlider.valueProperty().addListener((obs, old, val) -> updateSeedOnly());

        mobilityRateSlider.setMin(ParameterValidator.getMinPercentage());
        mobilityRateSlider.setMax(ParameterValidator.getMaxPercentage());
        mobilityRateSlider.valueProperty().addListener((obs, old, val) -> updateSeedOnly());

        loyaltyMeanSlider.setMin(ParameterValidator.getMinPercentage());
        loyaltyMeanSlider.setMax(ParameterValidator.getMaxPercentage());
        loyaltyMeanSlider.valueProperty().addListener((obs, old, val) -> updateSeedOnly());

        randomRangeSlider.setMin(ParameterValidator.getMinChaos());
        randomRangeSlider.setMax(ParameterValidator.getMaxChaos());
        randomRangeSlider.valueProperty().addListener((obs, old, val) -> updateSeedOnly());
    }

    private void updateSeedOnly() {
        if (isUpdatingInternal) return;
        long newSeed = java.util.Objects.hash(
                voterCountField.getText(),
                partyCountField.getText(),
                mediaInfluenceSlider.getValue(),
                mobilityRateSlider.getValue(),
                loyaltyMeanSlider.getValue(),
                scandalChanceField.getText(),
                randomRangeSlider.getValue()
        );
        isUpdatingInternal = true;
        seedField.setText(String.valueOf(Math.abs(newSeed)));
        isUpdatingInternal = false;
    }

    private void applyMasterSeed() {
        if (isUpdatingInternal) return;
        try {
            isUpdatingInternal = true;
            long seed = parseLongSafe(seedField.getText());
            Random masterRand = new Random(seed);

            int rPop = ParameterValidator.getMinPopulation() +
                    masterRand.nextInt(ParameterValidator.getMaxPopulation() - ParameterValidator.getMinPopulation());
            int rParties = ParameterValidator.getMinParties() +
                    masterRand.nextInt(ParameterValidator.getMaxParties() - ParameterValidator.getMinParties());

            voterCountField.setText(String.format(Main.getLocale(), "%,d", rPop));
            partyCountField.setText(String.valueOf(rParties));
            mediaInfluenceSlider.setValue(masterRand.nextDouble() * 100.0);
            mobilityRateSlider.setValue(masterRand.nextDouble() * 100.0);
            loyaltyMeanSlider.setValue(masterRand.nextDouble() * 100.0);
            scandalChanceField.setText(String.format(Locale.US, "%.1f", masterRand.nextDouble() * 20.0));
            randomRangeSlider.setValue(masterRand.nextDouble() * 10.0);

        } finally {
            isUpdatingInternal = false;
        }
    }

    public void synchronizeWithParameters(SimulationParameters params) {
        isUpdatingInternal = true;
        voterCountField.setText(String.format(Main.getLocale(), "%,d", params.populationSize()));
        partyCountField.setText(String.valueOf(params.partyCount()));
        scandalChanceField.setText(String.format(Locale.US, "%.1f", params.scandalProbability()));
        seedField.setText(String.valueOf(params.seed()));
        mediaInfluenceSlider.setValue(params.mediaInfluence());
        mobilityRateSlider.setValue(params.volatilityRate());
        loyaltyMeanSlider.setValue(params.loyaltyAverage());
        randomRangeSlider.setValue(params.chaosFactor());

        double displayBudget = params.budgetEffectiveness() * DEFAULT_BUDGET;
        budgetField.setText(String.format(Main.getLocale(), "%,.0f", displayBudget));
        isUpdatingInternal = false;
    }

    public SimulationParameters buildParametersFromUI(int currentTickRate) {
        try {
            int popSize = parseIntSafe(voterCountField.getText(), 100000);
            popSize = ParameterValidator.clampInt(popSize, ParameterValidator.getMinPopulation(), ParameterValidator.getMaxPopulation());
            int parties = parseIntSafe(partyCountField.getText(), 5);
            parties = ParameterValidator.clampInt(parties, ParameterValidator.getMinParties(), ParameterValidator.getMaxParties());
            double scandalProb = ParameterValidator.clampDouble(parseDoubleSafe(scandalChanceField.getText(), 5.0), ParameterValidator.getMinScandalProb(), ParameterValidator.getMaxScandalProb());
            double budgetInput = parseBudgetSafe(budgetField.getText());
            double budgetEffectiveness = ParameterValidator.clampDouble(budgetInput / DEFAULT_BUDGET, ParameterValidator.getMinBudgetEffectiveness(), ParameterValidator.getMaxBudgetEffectiveness());
            long seed = parseLongSafe(seedField.getText());

            return new SimulationParameters(popSize, mediaInfluenceSlider.getValue(), mobilityRateSlider.getValue(), scandalProb, loyaltyMeanSlider.getValue(), currentTickRate, randomRangeSlider.getValue(), parties, budgetEffectiveness, seed);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, ValidationMessage.INVALID_PARAMETER_INPUT.toString(), e);
            return null;
        }
    }

    public void applyInitialSettings(long population, long budget) {
        if (voterCountField != null) {
            int safePop = ParameterValidator.clampInt((int) population, ParameterValidator.getMinPopulation(), ParameterValidator.getMaxPopulation());
            voterCountField.setText(String.format(Main.getLocale(), "%,d", safePop));
        }
        if (budgetField != null) {
            budgetField.setText(String.format(Main.getLocale(), "%,d", budget));
        }
        updateSeedOnly();
    }

    public void randomizeParameters() {
        long newSeed = new Random().nextLong(1_000_000_000L);
        isUpdatingInternal = true;
        seedField.setText(String.valueOf(Math.abs(newSeed)));
        isUpdatingInternal = false;
        applyMasterSeed();
    }

    public void adjustIntField(TextField field, int delta, int min, int max) {
        int val = parseIntSafe(field.getText(), min);
        int newVal = ParameterValidator.clampInt(val + delta, min, max);
        field.setText(String.format(Main.getLocale(), "%,d", newVal));
        updateSeedOnly();
    }

    public void adjustDoubleField(TextField field, double delta) {
        double val = parseDoubleSafe(field.getText(), 0.0);
        double newVal = ParameterValidator.clampDouble(val + delta, ParameterValidator.getMinScandalProb(), ParameterValidator.getMaxScandalProb());
        field.setText(String.format(Locale.US, "%.1f", newVal));
        updateSeedOnly();
    }

    private void setupInteractiveField(TextField field, int maxLength) {
        if (field == null) return;
        applyInputFilter(field, maxLength);
        field.setOnAction(e -> { formatAndApply(field); updateSeedOnly(); });
        field.focusedProperty().addListener((obs, bool, isNowFocused) -> {
            if (!isNowFocused) { formatAndApply(field); updateSeedOnly(); }
        });
    }

    private void applyInputFilter(TextField field, int maxLength) {
        if (field == null) return;
        boolean isDecimal = (field == scandalChanceField);
        String regex = isDecimal ? "[0-9.,]*" : "[0-9.]*";

        field.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.length() > maxLength) {
                return null;
            }
            return newText.matches(regex) ? change : null;
        }));
    }

    /**
     * Formats the field's content and ensures it's within valid bounds.
     * Automatically clamps excessive values to the defined maximums.
     *
     * @param field the text field to validate and format
     */
    private void formatAndApply(TextField field) {
        String text = field.getText();
        if (text == null || text.isEmpty()) return;

        try {
            if (field == voterCountField) {
                int val = (int) parseLongSafe(text);
                int clamped = ParameterValidator.clampInt(val, ParameterValidator.getMinPopulation(), ParameterValidator.getMaxPopulation());
                field.setText(String.format(Main.getLocale(), "%,d", clamped));

            } else if (field == partyCountField) {
                int val = (int) parseLongSafe(text);
                int clamped = ParameterValidator.clampInt(val, ParameterValidator.getMinParties(), ParameterValidator.getMaxParties());
                field.setText(String.format(Main.getLocale(), "%,d", clamped));

            } else if (field == budgetField) {
                // HIER IST DIE BUDGET-VALIDIERUNG
                double val = parseBudgetSafe(text);
                double maxBudget = ParameterValidator.getMaxBudgetEffectiveness() * DEFAULT_BUDGET;
                double clamped = Math.max(0, Math.min(maxBudget, val));
                field.setText(String.format(Main.getLocale(), "%,.0f", clamped));

            } else if (field == scandalChanceField) {
                double val = parseDoubleSafe(text, 0.0);
                double clamped = ParameterValidator.clampDouble(val, ParameterValidator.getMinScandalProb(), ParameterValidator.getMaxScandalProb());
                field.setText(String.format(Locale.US, "%.1f", clamped));

            }
        } catch (Exception ignored) {
            // Fehlerhafte Eingaben werden ignoriert
        }
    }

    private long parseLongSafe(String text) {
        String clean = text.replaceAll("[^0-9]", "");
        return clean.isEmpty() ? 0 : Long.parseLong(clean);
    }

    private int parseIntSafe(String text, int defaultValue) {
        try { return Integer.parseInt(text.replaceAll("[^0-9]", "")); } catch (Exception e) { return defaultValue; }
    }

    private double parseDoubleSafe(String text, double defaultValue) {
        try { return Double.parseDouble(text.replace(",", ".")); } catch (Exception e) { return defaultValue; }
    }

    private double parseBudgetSafe(String text) {
        try { return Double.parseDouble(text.replace(".", "").replace(",", ".")); } catch (Exception e) { return DEFAULT_BUDGET; }
    }
}
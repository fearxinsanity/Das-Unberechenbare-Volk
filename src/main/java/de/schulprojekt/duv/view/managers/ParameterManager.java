package de.schulprojekt.duv.view.managers;

import de.schulprojekt.duv.model.core.SimulationParameters;
import de.schulprojekt.duv.util.validation.ParameterValidator;
import de.schulprojekt.duv.util.validation.ValidationMessage;
import de.schulprojekt.duv.view.Main;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages all parameter input fields, validation, and formatting.
 * Handles text fields, sliders, and parameter synchronization with the simulation engine.
 * Provides input validation, formatting with locale support, and randomization features.
 *
 * @author Nico Hoffmann
 * @version 1.0
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

    private TextField voterCountField;
    private TextField partyCountField;
    private TextField budgetField;
    private TextField scandalChanceField;
    private TextField seedField;

    private Slider mediaInfluenceSlider;
    private Slider mobilityRateSlider;
    private Slider loyaltyMeanSlider;
    private Slider randomRangeSlider;

    private boolean isUpdatingInternal = false;

    // ========================================
    // Constructors
    // ========================================

    /**
     * Default constructor for ParameterManager.
     * Creates an empty manager that must be configured with setters.
     */
    @SuppressWarnings("unused")
    public ParameterManager() {
    }

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
        setupInteractiveField(voterCountField);
        setupInteractiveField(partyCountField);
        setupInteractiveField(budgetField);
        setupInteractiveField(scandalChanceField);

        applyInputFilter(seedField);
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

    /**
     * Synchronizes UI fields with given simulation parameters.
     * Updates all text fields and sliders to reflect the current parameter values.
     *
     * @param params the simulation parameters to display
     */
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

    /**
     * Reads current UI values and constructs a SimulationParameters object.
     * Validates and clamps all input values to acceptable ranges from ParameterValidator.
     *
     * @param currentTickRate the current tick rate to preserve
     * @return the constructed SimulationParameters, or null if validation fails
     */
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

    /**
     * Applies initial settings from StartController.
     * Used when transitioning from the start screen to the dashboard.
     *
     * @param population the initial population size
     * @param budget the initial budget value
     */
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

    /**
     * Randomizes all parameter values with safe bounds from ParameterValidator.
     * Generates random values within acceptable ranges for all parameters.
     */
    public void randomizeParameters() {
        long newSeed = new Random().nextLong(1_000_000_000L);
        isUpdatingInternal = true;
        seedField.setText(String.valueOf(Math.abs(newSeed)));
        isUpdatingInternal = false;
        applyMasterSeed();
    }

    /**
     * Adjusts an integer field by a delta value with bounds checking.
     *
     * @param field the text field to adjust
     * @param delta the amount to add or subtract
     * @param min the minimum allowed value
     * @param max the maximum allowed value
     */
    public void adjustIntField(TextField field, int delta, int min, int max) {
        int val = parseIntSafe(field.getText(), min);
        int newVal = ParameterValidator.clampInt(val + delta, min, max);
        field.setText(String.format(Main.getLocale(), "%,d", newVal));
        updateSeedOnly();
    }

    /**
     * Adjusts a double field by a delta value with bounds checking.
     *
     * @param field the text field to adjust
     * @param delta the amount to add or subtract
     */
    public void adjustDoubleField(TextField field, double delta) {
        double val = parseDoubleSafe(field.getText(), 0.0);
        double newVal = ParameterValidator.clampDouble(val + delta, ParameterValidator.getMinScandalProb(), ParameterValidator.getMaxScandalProb());
        field.setText(String.format(Locale.US, "%.1f", newVal));
        updateSeedOnly();
    }

    // ========================================
    // Utility Methods
    // ========================================

    private void setupInteractiveField(TextField field) {
        if (field == null) return;
        applyInputFilter(field);
        field.setOnAction(e -> { formatAndApply(field); updateSeedOnly(); });
        field.focusedProperty().addListener((obs, bool, isNowFocused) -> {
            if (!isNowFocused) { formatAndApply(field); updateSeedOnly(); }
        });
    }

    private void applyInputFilter(TextField field) {
        if (field == null) return;
        boolean isDecimal = (field == scandalChanceField);
        String regex = isDecimal ? "[0-9.,]*" : "[0-9.]*";
        field.setTextFormatter(new TextFormatter<>(change -> change.getControlNewText().matches(regex) ? change : null));
    }

    private void formatAndApply(TextField field) {
        String text = field.getText();
        if (text == null || text.isEmpty()) return;
        try {
            if (field == scandalChanceField) {
                field.setText(String.format(Locale.US, "%.1f", parseDoubleSafe(text, 0.0)));
            } else if (field != seedField) {
                long val = parseLongSafe(text);
                field.setText(NumberFormat.getInstance(Main.getLocale()).format(val));
            }
        } catch (Exception ignored) {}
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
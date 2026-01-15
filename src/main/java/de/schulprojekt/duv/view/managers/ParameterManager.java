package de.schulprojekt.duv.view.managers;

import de.schulprojekt.duv.model.core.SimulationParameters;
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

    private static final double MIN_SCANDAL_PROB = 0.0;
    private static final double MAX_SCANDAL_PROB = 60.0;
    private static final int MIN_POPULATION = 10_000;
    private static final int MAX_POPULATION = 500_000;
    private static final double MAX_BUDGET_FACTOR = 1000.0;
    private static final double DEFAULT_BUDGET = 500000.0;

    // ========================================
    // Instance Variables
    // ========================================

    private TextField voterCountField;
    private TextField partyCountField;
    private TextField budgetField;
    private TextField scandalChanceField;

    private Slider mediaInfluenceSlider;
    private Slider mobilityRateSlider;
    private Slider loyaltyMeanSlider;
    private Slider randomRangeSlider;

    private Runnable onParameterChangeCallback;

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
            Slider mediaInfluenceSlider,
            Slider mobilityRateSlider,
            Slider loyaltyMeanSlider,
            Slider randomRangeSlider
    ) {
        this.voterCountField = voterCountField;
        this.partyCountField = partyCountField;
        this.budgetField = budgetField;
        this.scandalChanceField = scandalChanceField;
        this.mediaInfluenceSlider = mediaInfluenceSlider;
        this.mobilityRateSlider = mobilityRateSlider;
        this.loyaltyMeanSlider = loyaltyMeanSlider;
        this.randomRangeSlider = randomRangeSlider;
    }

    // ========================================
    // Getter Methods
    // ========================================

    /**
     * Gets the voter count text field.
     *
     * @return the voter count input field
     */
    @SuppressWarnings("unused")
    public TextField getVoterCountField() {
        return voterCountField;
    }

    /**
     * Gets the party count text field.
     *
     * @return the party count input field
     */
    @SuppressWarnings("unused")
    public TextField getPartyCountField() {
        return partyCountField;
    }

    /**
     * Gets the budget text field.
     *
     * @return the budget input field
     */
    @SuppressWarnings("unused")
    public TextField getBudgetField() {
        return budgetField;
    }

    /**
     * Gets the scandal chance text field.
     *
     * @return the scandal probability input field
     */
    @SuppressWarnings("unused")
    public TextField getScandalChanceField() {
        return scandalChanceField;
    }

    // ========================================
    // Setter Methods
    // ========================================

    /**
     * Sets the callback to be invoked when parameters change.
     *
     * @param callback the runnable to execute on parameter change
     */
    public void setOnParameterChangeCallback(Runnable callback) {
        this.onParameterChangeCallback = callback;
    }

    // ========================================
    // Business Logic Methods
    // ========================================

    /**
     * Initializes all input fields with event handlers and formatters.
     * Sets up input validation, focus listeners, and automatic formatting.
     */
    public void initializeFields() {
        setupInteractiveField(voterCountField);
        setupInteractiveField(partyCountField);
        setupInteractiveField(budgetField);
        setupInteractiveField(scandalChanceField);
    }

    /**
     * Synchronizes UI fields with given simulation parameters.
     * Updates all text fields and sliders to reflect the current parameter values.
     *
     * @param params the simulation parameters to display
     */
    public void synchronizeWithParameters(SimulationParameters params) {
        voterCountField.setText(String.format(Locale.GERMANY, "%,d", params.populationSize()));
        partyCountField.setText(String.valueOf(params.partyCount()));
        scandalChanceField.setText(String.format(Locale.US, "%.1f", params.scandalProbability()));
        mediaInfluenceSlider.setValue(params.mediaInfluence());
        mobilityRateSlider.setValue(params.volatilityRate());
        loyaltyMeanSlider.setValue(params.loyaltyAverage());
        randomRangeSlider.setValue(params.chaosFactor());

        double displayBudget = params.budgetEffectiveness() * 500000.0;
        budgetField.setText(String.format(Locale.GERMANY, "%,.0f", displayBudget));
    }

    /**
     * Reads current UI values and constructs a SimulationParameters object.
     * Validates and clamps all input values to acceptable ranges.
     *
     * @param currentTickRate the current tick rate to preserve
     * @return the constructed SimulationParameters, or null if validation fails
     */
    public SimulationParameters buildParametersFromUI(int currentTickRate) {
        try {
            int popSize = parseIntSafe(voterCountField.getText(), 100000);
            popSize = Math.clamp(popSize, MIN_POPULATION, MAX_POPULATION);

            int parties = parseIntSafe(partyCountField.getText(), 5);
            parties = Math.clamp(parties, 2, 8);

            double scandalProb = Math.clamp(
                    parseDoubleSafe(scandalChanceField.getText(), 5.0),
                    MIN_SCANDAL_PROB,
                    MAX_SCANDAL_PROB
            );

            double budgetInput = parseBudgetSafe(budgetField.getText());
            double budgetEffectiveness = Math.clamp(budgetInput / 500000.0, 0.1, MAX_BUDGET_FACTOR);

            return new SimulationParameters(
                    popSize,
                    mediaInfluenceSlider.getValue(),
                    mobilityRateSlider.getValue(),
                    scandalProb,
                    loyaltyMeanSlider.getValue(),
                    currentTickRate,
                    randomRangeSlider.getValue(),
                    parties,
                    budgetEffectiveness
            );
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Invalid parameter input, using defaults", e);
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
            long safePop = Math.clamp(population, MIN_POPULATION, MAX_POPULATION);
            voterCountField.setText(String.format(Locale.GERMANY, "%,d", safePop));
        }
        if (budgetField != null) {
            budgetField.setText(String.format(Locale.GERMANY, "%,d", budget));
        }
        triggerParameterChange();
    }

    /**
     * Randomizes all parameter values with safe bounds.
     * Generates random values within acceptable ranges for all parameters.
     *
     * @param animationCallback callback to handle animation completion (optional)
     */
    public void randomizeParameters(Runnable animationCallback) {
        removeInputFilters();

        Random rand = new Random();
        int rPop = 10000 + rand.nextInt(MAX_POPULATION - 10000);
        int rParties = 2 + rand.nextInt(7);
        double rMedia = rand.nextDouble() * 100.0;
        double rVolatility = rand.nextDouble() * 100.0;
        double rLoyalty = rand.nextDouble() * 100.0;
        double rBudget = 50000.0 + rand.nextDouble() * 1950000.0;
        double rScandal = rand.nextDouble() * 15.0;
        double rChaos = 0.1 + rand.nextDouble() * 2.9;

        voterCountField.setText(String.format(Locale.GERMANY, "%,d", rPop));
        partyCountField.setText(String.valueOf(rParties));
        budgetField.setText(String.format(Locale.GERMANY, "%,.0f", rBudget));
        scandalChanceField.setText(String.format(Locale.US, "%.1f", rScandal));

        mediaInfluenceSlider.setValue(rMedia);
        mobilityRateSlider.setValue(rVolatility);
        loyaltyMeanSlider.setValue(rLoyalty);
        randomRangeSlider.setValue(rChaos);

        applyInputFilters();
        if (animationCallback != null) {
            animationCallback.run();
        }
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
        field.setText(String.format(Locale.GERMANY, "%,d", Math.clamp(val + delta, min, max)));
        triggerParameterChange();
    }

    /**
     * Adjusts a double field by a delta value with bounds checking.
     *
     * @param field the text field to adjust
     * @param delta the amount to add or subtract
     */
    public void adjustDoubleField(TextField field, double delta) {
        double val = parseDoubleSafe(field.getText(), 0.0);
        field.setText(String.format(Locale.US, "%.1f",
                Math.clamp(val + delta, MIN_SCANDAL_PROB, MAX_SCANDAL_PROB)));
        triggerParameterChange();
    }

    // ========================================
    // Utility Methods
    // ========================================

    /**
     * Sets up interactive behavior for a text field.
     * Adds input filtering, formatting, and event handlers.
     *
     * @param field the text field to configure
     */
    private void setupInteractiveField(TextField field) {
        if (field == null) return;

        applyInputFilter(field);

        field.setOnAction(_ -> formatAndApply(field));

        field.focusedProperty().addListener((_, _, isNowFocused) -> {
            if (!isNowFocused) {
                formatAndApply(field);
            }
        });

        field.setOnKeyPressed(_ -> field.setStyle(""));
    }

    /**
     * Applies input validation filter to a text field.
     * Restricts input to valid numeric characters based on field type.
     *
     * @param field the text field to filter
     */
    private void applyInputFilter(TextField field) {
        if (field == null) return;
        boolean isDecimal = (field == scandalChanceField);
        String regex = isDecimal ? "[0-9.,]*" : "[0-9.]*";

        field.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            return newText.matches(regex) ? change : null;
        }));
    }

    /**
     * Applies input filters to all text fields.
     */
    private void applyInputFilters() {
        applyInputFilter(voterCountField);
        applyInputFilter(partyCountField);
        applyInputFilter(budgetField);
        applyInputFilter(scandalChanceField);
    }

    /**
     * Temporarily removes input filters from all text fields.
     * Used during randomization to bypass validation.
     */
    private void removeInputFilters() {
        if (voterCountField != null) voterCountField.setTextFormatter(null);
        if (partyCountField != null) partyCountField.setTextFormatter(null);
        if (budgetField != null) budgetField.setTextFormatter(null);
        if (scandalChanceField != null) scandalChanceField.setTextFormatter(null);
    }

    /**
     * Formats and validates a text field's content.
     * Applies locale-specific number formatting and triggers parameter change callback.
     *
     * @param field the text field to format
     */
    private void formatAndApply(TextField field) {
        String text = field.getText();
        if (text == null || text.isEmpty()) return;

        try {
            boolean isDecimal = (field == scandalChanceField);

            if (isDecimal) {
                double val = parseDoubleSafe(text, 0.0);
                field.setText(String.format(Locale.US, "%.1f", val));
            } else {
                long val = parseLongSafe(text);

                if (field == voterCountField) {
                    val = Math.clamp(val, MIN_POPULATION, MAX_POPULATION);
                }

                NumberFormat formatter = NumberFormat.getInstance(Locale.GERMANY);
                field.setText(formatter.format(val));
            }

            triggerParameterChange();

        } catch (Exception e) {
            field.setStyle("-fx-border-color: red;");
        }
    }

    /**
     * Safely parses a long value from text, removing non-numeric characters.
     *
     * @param text the text to parse
     * @return the parsed long value, or 0 if parsing fails
     */
    private long parseLongSafe(String text) {
        String clean = text.replaceAll("[^0-9]", "");
        return clean.isEmpty() ? 0 : Long.parseLong(clean);
    }

    /**
     * Safely parses an integer value from text with a default fallback.
     *
     * @param text the text to parse
     * @param defaultValue the value to return if parsing fails
     * @return the parsed integer value, or defaultValue if parsing fails
     */
    private int parseIntSafe(String text, int defaultValue) {
        try {
            return Integer.parseInt(text.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Safely parses a double value from text with a default fallback.
     *
     * @param text the text to parse
     * @param defaultValue the value to return if parsing fails
     * @return the parsed double value, or defaultValue if parsing fails
     */
    private double parseDoubleSafe(String text, double defaultValue) {
        try {
            return Double.parseDouble(text.replace(",", "."));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Safely parses budget value from German formatted text.
     *
     * @param text the text to parse
     * @return the parsed budget value, or DEFAULT_BUDGET if parsing fails
     */
    private double parseBudgetSafe(String text) {
        try {
            String clean = text.replace(".", "").replace(",", ".");
            return Double.parseDouble(clean);
        } catch (Exception e) {
            return DEFAULT_BUDGET;
        }
    }

    /**
     * Triggers the parameter change callback if set.
     */
    private void triggerParameterChange() {
        if (onParameterChangeCallback != null) {
            onParameterChangeCallback.run();
        }
    }
}

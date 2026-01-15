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
 * Handles text fields, sliders, and parameter synchronization.
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
     * Default constructor.
     */
    public ParameterManager() {
    }

    /**
     * Constructor with all input fields.
     *
     * @param voterCountField the voter population input field
     * @param partyCountField the party count input field
     * @param budgetField the budget input field
     * @param scandalChanceField the scandal probability input field
     * @param mediaInfluenceSlider the media influence slider
     * @param mobilityRateSlider the voter mobility slider
     * @param loyaltyMeanSlider the loyalty average slider
     * @param randomRangeSlider the chaos factor slider
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

    public TextField getVoterCountField() {
        return voterCountField;
    }

    public TextField getPartyCountField() {
        return partyCountField;
    }

    public TextField getBudgetField() {
        return budgetField;
    }

    public TextField getScandalChanceField() {
        return scandalChanceField;
    }

    // ========================================
    // Setter Methods
    // ========================================

    public void setOnParameterChangeCallback(Runnable callback) {
        this.onParameterChangeCallback = callback;
    }

    // ========================================
    // Business Logic Methods
    // ========================================

    /**
     * Initializes all input fields with event handlers and formatters.
     */
    public void initializeFields() {
        setupInteractiveField(voterCountField);
        setupInteractiveField(partyCountField);
        setupInteractiveField(budgetField);
        setupInteractiveField(scandalChanceField);
    }

    /**
     * Synchronizes UI fields with given simulation parameters.
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
     * Reads current UI values and creates a SimulationParameters object.
     *
     * @param currentTickRate the current tick rate to preserve
     * @return the constructed SimulationParameters
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
     * Randomizes all parameter values with animation effect.
     *
     * @param animationCallback callback to handle animation completion
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

        // Set text values (animations handled by caller via VisualFX)
        voterCountField.setText(String.format(Locale.GERMANY, "%,d", rPop));
        partyCountField.setText(String.valueOf(rParties));
        budgetField.setText(String.format(Locale.GERMANY, "%,.0f", rBudget));
        scandalChanceField.setText(String.format(Locale.US, "%.1f", rScandal));

        mediaInfluenceSlider.setValue(rMedia);
        mobilityRateSlider.setValue(rVolatility);
        loyaltyMeanSlider.setValue(rLoyalty);
        randomRangeSlider.setValue(rChaos);

        // Re-apply filters and trigger callback
        applyInputFilters();
        if (animationCallback != null) {
            animationCallback.run();
        }
    }

    /**
     * Adjusts an integer field by a delta value.
     *
     * @param field the text field to adjust
     * @param delta the amount to add/subtract
     * @param min the minimum allowed value
     * @param max the maximum allowed value
     */
    public void adjustIntField(TextField field, int delta, int min, int max) {
        int val = parseIntSafe(field.getText(), min);
        field.setText(String.format(Locale.GERMANY, "%,d", Math.clamp(val + delta, min, max)));
        triggerParameterChange();
    }

    /**
     * Adjusts a double field by a delta value.
     *
     * @param field the text field to adjust
     * @param delta the amount to add/subtract
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

    private void setupInteractiveField(TextField field) {
        if (field == null) return;

        applyInputFilter(field);

        field.setOnAction(e -> {
            formatAndApply(field);
        });

        field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                formatAndApply(field);
            }
        });

        field.setOnKeyPressed(e -> field.setStyle(""));
    }

    private void applyInputFilter(TextField field) {
        if (field == null) return;
        boolean isDecimal = (field == scandalChanceField);
        String regex = isDecimal ? "[0-9.,]*" : "[0-9.]*";

        field.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            return newText.matches(regex) ? change : null;
        }));
    }

    private void applyInputFilters() {
        applyInputFilter(voterCountField);
        applyInputFilter(partyCountField);
        applyInputFilter(budgetField);
        applyInputFilter(scandalChanceField);
    }

    private void removeInputFilters() {
        if (voterCountField != null) voterCountField.setTextFormatter(null);
        if (partyCountField != null) partyCountField.setTextFormatter(null);
        if (budgetField != null) budgetField.setTextFormatter(null);
        if (scandalChanceField != null) scandalChanceField.setTextFormatter(null);
    }

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

    private long parseLongSafe(String text) {
        String clean = text.replaceAll("[^0-9]", "");
        return clean.isEmpty() ? 0 : Long.parseLong(clean);
    }

    private int parseIntSafe(String text, int defaultValue) {
        try {
            return Integer.parseInt(text.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private double parseDoubleSafe(String text, double defaultValue) {
        try {
            return Double.parseDouble(text.replace(",", "."));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private double parseBudgetSafe(String text) {
        try {
            String clean = text.replace(".", "").replace(",", ".");
            return Double.parseDouble(clean);
        } catch (Exception e) {
            return DEFAULT_BUDGET;
        }
    }

    private void triggerParameterChange() {
        if (onParameterChangeCallback != null) {
            onParameterChangeCallback.run();
        }
    }
}

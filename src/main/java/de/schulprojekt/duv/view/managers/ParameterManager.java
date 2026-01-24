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
 * Verwaltet alle Eingabefelder, Validierungen und Formatierungen.
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

    private final TextField voterCountField;
    private final TextField partyCountField;
    private final TextField budgetField;
    private final TextField scandalChanceField;

    private final Slider mediaInfluenceSlider;
    private final Slider mobilityRateSlider;
    private final Slider loyaltyMeanSlider;
    private final Slider randomRangeSlider;

    // ========================================
    // Constructors
    // ========================================

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
    // Business Logic Methods
    // ========================================

    public void initializeFields() {
        int maxPopLength = String.valueOf(ParameterValidator.getMaxPopulation()).length() + 3;
        int maxPartyLength = String.valueOf(ParameterValidator.getMaxParties()).length();
        int maxBudgetLength = 15;
        int maxScandalLength = 5;

        setupInteractiveField(voterCountField, maxPopLength);
        setupInteractiveField(partyCountField, maxPartyLength);
        setupInteractiveField(budgetField, maxBudgetLength);
        setupInteractiveField(scandalChanceField, maxScandalLength);

        mediaInfluenceSlider.setMin(ParameterValidator.getMinPercentage());
        mediaInfluenceSlider.setMax(ParameterValidator.getMaxPercentage());

        mobilityRateSlider.setMin(ParameterValidator.getMinPercentage());
        mobilityRateSlider.setMax(ParameterValidator.getMaxPercentage());

        loyaltyMeanSlider.setMin(ParameterValidator.getMinPercentage());
        loyaltyMeanSlider.setMax(ParameterValidator.getMaxPercentage());

        randomRangeSlider.setMin(ParameterValidator.getMinChaos());
        randomRangeSlider.setMax(ParameterValidator.getMaxChaos());
    }

    public void synchronizeWithParameters(SimulationParameters params) {
        voterCountField.setText(String.format(Main.getLocale(), "%,d", params.populationSize()));
        partyCountField.setText(String.valueOf(params.partyCount()));
        scandalChanceField.setText(String.format(Locale.US, "%.1f", params.scandalProbability()));

        mediaInfluenceSlider.setValue(params.mediaInfluence());
        mobilityRateSlider.setValue(params.volatilityRate());
        loyaltyMeanSlider.setValue(params.loyaltyAverage());
        randomRangeSlider.setValue(params.chaosFactor());

        double displayBudget = params.budgetEffectiveness() * DEFAULT_BUDGET;
        budgetField.setText(String.format(Main.getLocale(), "%,.0f", displayBudget));
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
    }

    public void randomizeParameters() {
        Random rand = new Random();
        int rPop = ParameterValidator.getMinPopulation() +
                rand.nextInt(ParameterValidator.getMaxPopulation() - ParameterValidator.getMinPopulation() + 1);
        int rParties = ParameterValidator.getMinParties() +
                rand.nextInt(ParameterValidator.getMaxParties() - ParameterValidator.getMinParties() + 1);

        voterCountField.setText(String.format(Main.getLocale(), "%,d", rPop));
        partyCountField.setText(String.valueOf(rParties));

        mediaInfluenceSlider.setValue(rand.nextDouble() * 100.0);
        mobilityRateSlider.setValue(rand.nextDouble() * 100.0);
        loyaltyMeanSlider.setValue(rand.nextDouble() * 100.0);

        double rScandal = rand.nextDouble() * (ParameterValidator.getMaxScandalProb() - ParameterValidator.getMinScandalProb());
        scandalChanceField.setText(String.format(Locale.US, "%.1f", rScandal));

        double rChaos = rand.nextDouble() * ParameterValidator.getMaxChaos();
        randomRangeSlider.setValue(rChaos);
    }

    public void adjustIntField(TextField field, int delta, int min, int max) {
        int val = parseIntSafe(field.getText(), min);
        int newVal = ParameterValidator.clampInt(val + delta, min, max);
        field.setText(String.format(Main.getLocale(), "%,d", newVal));
    }

    public void adjustDoubleField(TextField field, double delta) {
        double val = parseDoubleSafe(field.getText(), 0.0);
        double newVal = ParameterValidator.clampDouble(val + delta, ParameterValidator.getMinScandalProb(), ParameterValidator.getMaxScandalProb());
        field.setText(String.format(Locale.US, "%.1f", newVal));
    }

    private void setupInteractiveField(TextField field, int maxLength) {
        if (field == null) return;
        applyInputFilter(field, maxLength);
        field.setOnAction(e -> formatAndApply(field));
        field.focusedProperty().addListener((obs, bool, isNowFocused) -> {
            if (!isNowFocused) formatAndApply(field);
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
                double val = parseBudgetSafe(text);
                double maxBudget = ParameterValidator.getMaxBudgetEffectiveness() * DEFAULT_BUDGET;
                double clamped = Math.max(0, Math.min(maxBudget, val));
                field.setText(String.format(Main.getLocale(), "%,.0f", clamped));

            } else if (field == scandalChanceField) {
                double val = parseDoubleSafe(text, 0.0);
                double clamped = ParameterValidator.clampDouble(val, ParameterValidator.getMinScandalProb(), ParameterValidator.getMaxScandalProb());
                field.setText(String.format(Locale.US, "%.1f", clamped));
            }
        } catch (Exception ignored) { }
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
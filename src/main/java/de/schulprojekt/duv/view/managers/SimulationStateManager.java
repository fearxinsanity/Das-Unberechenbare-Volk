package de.schulprojekt.duv.view.managers;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import de.schulprojekt.duv.view.util.VisualFX;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Manages simulation state, timer, locking mechanisms, and UI status display.
 * Handles the complete simulation lifecycle including running, paused, and reset states.
 * Provides visual feedback through button states, blinking effects, and status labels.
 *
 * @author Nico Hoffmann
 * @version 1.1
 */
public class SimulationStateManager {

    // ========================================
    // Static Variables
    // ========================================

    private static final Logger LOGGER = Logger.getLogger(SimulationStateManager.class.getName());
    private static final int DEFAULT_DURATION_SECONDS = 30;

    // ========================================
    // Instance Variables
    // ========================================

    private Timeline simulationTimer;
    private int configDurationSeconds;
    private int remainingSeconds;
    private int currentTick;

    private Label timeStepLabel;
    private TextField durationField;

    private Button executeToggleButton;
    private Button resetButton;
    private Button intelButton;
    private Button parliamentButton;

    private VBox populationBox;
    private VBox partyBox;
    private VBox budgetBox;
    private VBox seedBox;
    private VBox durationBox;
    private VBox randomBox;

    private Label populationOverlay;
    private Label partyOverlay;
    private Label budgetOverlay;
    private Label seedOverlay;
    private Label durationOverlay;
    private Label randomOverlay;

    private VBox leftSidebar;
    private VBox rightSidebar;

    private String originalIntelText;
    private String originalParliamentText;

    private final Map<Node, FadeTransition> activeBlinks;

    private Runnable onPauseCallback;

    // ========================================
    // Constructors
    // ========================================

    /**
     * Default constructor for SimulationStateManager.
     * Initializes with default duration and empty state.
     */
    public SimulationStateManager() {
        this.configDurationSeconds = DEFAULT_DURATION_SECONDS;
        this.remainingSeconds = DEFAULT_DURATION_SECONDS;
        this.currentTick = 0;
        this.activeBlinks = new HashMap<>();
    }

    // ========================================
    // Getter Methods
    // ========================================

    /**
     * Gets the current simulation tick count.
     *
     * @return the current tick number
     */
    public int getCurrentTick() {
        return currentTick;
    }

    /**
     * Gets the remaining time in seconds.
     *
     * @return the remaining seconds in the simulation
     */
    public int getRemainingSeconds() {
        return remainingSeconds;
    }

    /**
     * Gets the configured duration in seconds.
     *
     * @return the total configured duration
     */
    @SuppressWarnings("unused")
    public int getConfigDurationSeconds() {
        return configDurationSeconds;
    }

    /**
     * Checks if the simulation timer is currently running.
     *
     * @return true if the timer is running, false otherwise
     */
    @SuppressWarnings("unused")
    public boolean isTimerRunning() {
        return simulationTimer != null && simulationTimer.getStatus() == Animation.Status.RUNNING;
    }

    // ========================================
    // Setter Methods
    // ========================================

    /**
     * Sets the current simulation tick.
     *
     * @param tick the tick count to set
     */
    public void setCurrentTick(int tick) {
        this.currentTick = tick;
    }

    /**
     * Sets the status display label.
     *
     * @param label the label to display simulation status
     */
    public void setTimeStepLabel(Label label) {
        this.timeStepLabel = label;
    }

    /**
     * Sets the duration input field.
     *
     * @param field the text field for duration display
     */
    public void setDurationField(TextField field) {
        this.durationField = field;
    }

    /**
     * Sets all control buttons for state management.
     *
     * @param executeToggle the start/pause simulation button
     * @param reset the reset simulation button
     * @param intel the intelligence report button
     * @param parliament the parliament view button
     */
    public void setButtons(Button executeToggle, Button reset, Button intel, Button parliament) {
        this.executeToggleButton = executeToggle;
        this.resetButton = reset;
        this.intelButton = intel;
        this.parliamentButton = parliament;

        if (intel != null) {
            this.originalIntelText = intel.getText();
        }
        if (parliament != null) {
            this.originalParliamentText = parliament.getText();
        }
    }

    /**
     * Sets the parameter boxes and their lock overlays.
     *
     * @param popBox the population parameter box
     * @param partyBox the party count parameter box
     * @param budgetBox the budget parameter box
     * @param seedBox the seed parameter box
     * @param durationBox the duration parameter box
     * @param randomBox the random button box
     * @param popOverlay the population lock overlay
     * @param partyOverlay the party lock overlay
     * @param budgetOverlay the budget lock overlay
     * @param seedOverlay the seed lock overlay
     * @param durationOverlay the duration lock overlay
     * @param randomOverlay the random lock overlay
     */
    public void setLockingContainers(
            VBox popBox, VBox partyBox, VBox budgetBox, VBox seedBox, VBox durationBox, VBox randomBox,
            Label popOverlay, Label partyOverlay, Label budgetOverlay, Label seedOverlay, Label durationOverlay, Label randomOverlay
    ) {
        this.populationBox = popBox;
        this.partyBox = partyBox;
        this.budgetBox = budgetBox;
        this.seedBox = seedBox;
        this.durationBox = durationBox;
        this.randomBox = randomBox;
        this.populationOverlay = popOverlay;
        this.partyOverlay = partyOverlay;
        this.budgetOverlay = budgetOverlay;
        this.seedOverlay = seedOverlay;
        this.durationOverlay = durationOverlay;
        this.randomOverlay = randomOverlay;
    }

    /**
     * Sets the sidebar containers for visual effects.
     *
     * @param left the left sidebar container
     * @param right the right sidebar container
     */
    public void setSidebars(VBox left, VBox right) {
        this.leftSidebar = left;
        this.rightSidebar = right;
    }

    /**
     * Sets the callback to invoke when simulation is paused.
     *
     * @param callback the runnable to execute on pause
     */
    public void setOnPauseCallback(Runnable callback) {
        this.onPauseCallback = callback;
    }

    // ========================================
    // Business Logic Methods
    // ========================================

    /**
     * Initializes and configures the simulation timer.
     * Creates a timeline that decrements remaining time every second.
     * Triggers completion callback and visual effects when time reaches zero.
     */
    public void setupTimer() {
        simulationTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            remainingSeconds--;

            if (remainingSeconds <= 0) {
                remainingSeconds = 0;
                simulationTimer.stop();

                if (onPauseCallback != null) {
                    onPauseCallback.run();
                }

                if (executeToggleButton != null) {
                    executeToggleButton.setDisable(true);
                }

                if (resetButton != null) {
                    resetButton.setDisable(false);
                }

                updateStatusDisplay(false);
                lockResultButtons(false);
                VisualFX.triggerSidebarGlitch(leftSidebar, rightSidebar);
                VisualFX.startPulse(intelButton, Color.LIME);
                VisualFX.startPulse(parliamentButton, Color.LIME);
                LOGGER.info("Simulation finished. Access granted.");
            } else {
                updateStatusDisplay(true);
            }
        }));
        simulationTimer.setCycleCount(Timeline.INDEFINITE);
        updateDurationDisplay();
    }

    /**
     * Starts the simulation timer and locks parameter inputs.
     * Updates button states and UI feedback to reflect running state.
     */
    public void startTimer() {
        if (simulationTimer != null && remainingSeconds > 0) {
            if (simulationTimer.getStatus() != Animation.Status.RUNNING) {
                simulationTimer.play();
            }
            setSimulationLocked(true);
            lockResultButtons(true);
            updateButtonStates(true);
            updateStatusDisplay(true);
        }
    }

    /**
     * Pauses the simulation timer.
     * Updates button states and status display to reflect paused state.
     */
    public void pauseTimer() {
        if (simulationTimer != null) {
            simulationTimer.pause();
        }
        updateButtonStates(false);
        updateStatusDisplay(false);
    }

    /**
     * Resets the simulation to initial state.
     * Stops timer, resets tick count, unlocks inputs, and updates UI.
     */
    public void resetSimulation() {
        if (simulationTimer != null) {
            simulationTimer.stop();
        }
        this.currentTick = 0;
        this.remainingSeconds = configDurationSeconds;
        updateDurationDisplay();
        setSimulationLocked(false);
        lockResultButtons(true);

        if (executeToggleButton != null) {
            executeToggleButton.setDisable(false);
        }

        updateButtonStates(false);

        if (resetButton != null) {
            resetButton.setDisable(true);
        }

        updateStatusDisplay(false);
    }

    /**
     * Increments the configured simulation duration by 30 seconds.
     * Maximum duration is capped at 300 seconds (5 minutes).
     */
    public void incrementDuration() {
        if (configDurationSeconds < 300) {
            configDurationSeconds += 30;
            remainingSeconds = configDurationSeconds;
            updateDurationDisplay();
        }
    }

    /**
     * Decrements the configured simulation duration by 30 seconds.
     * Minimum duration is capped at 30 seconds.
     */
    public void decrementDuration() {
        if (configDurationSeconds > 30) {
            configDurationSeconds -= 30;
            remainingSeconds = configDurationSeconds;
            updateDurationDisplay();
        }
    }

    /**
     * Updates the status display label with current simulation state.
     * Shows running/paused status, tick count, and remaining time.
     *
     * @param isRunning whether the simulation is currently running
     */
    public void updateStatusDisplay(boolean isRunning) {
        if (timeStepLabel == null) return;

        String statusText = isRunning ? "RUNNING" : "PAUSED";
        String color = isRunning ? "#55ff55" : "#ff5555";

        int m = remainingSeconds / 60;
        int s = remainingSeconds % 60;
        String timeText = String.format("%02d:%02d", m, s);

        timeStepLabel.setText(String.format(
                "STATUS: %s | TICK: %d | T-MINUS: %s",
                statusText, currentTick, timeText
        ));
        timeStepLabel.setStyle(String.format(
                "-fx-text-fill: %s; -fx-font-family: 'Consolas'; -fx-font-weight: bold;",
                color
        ));
    }

    /**
     * Locks or unlocks the result buttons (Intelligence and Parliament views).
     * Locked buttons show "[ LOCKED ]" text and are disabled.
     *
     * @param locked true to lock buttons, false to unlock
     */
    public void lockResultButtons(boolean locked) {
        setButtonLockState(intelButton, locked, originalIntelText);
        setButtonLockState(parliamentButton, locked, originalParliamentText);

        if (locked) {
            if (intelButton != null) VisualFX.stopPulse(intelButton);
            if (parliamentButton != null) VisualFX.stopPulse(parliamentButton);
        }
    }

    /**
     * Stops the simulation timer completely.
     * Used during application shutdown.
     */
    public void stopTimer() {
        if (simulationTimer != null) {
            simulationTimer.stop();
        }
    }

    // ========================================
    // Utility Methods
    // ========================================

    /**
     * Updates the duration display field with formatted time.
     */
    private void updateDurationDisplay() {
        if (durationField == null) return;
        int m = configDurationSeconds / 60;
        int s = configDurationSeconds % 60;
        durationField.setText(String.format("%02d:%02d", m, s));
    }

    /**
     * Updates control button states based on simulation state.
     *
     * @param isRunning true if simulation is running
     */
    private void updateButtonStates(boolean isRunning) {
        if (executeToggleButton != null) {
            executeToggleButton.setText(isRunning ? "⏸ FREEZE" : "▶ EXECUTE");
        }
        if (resetButton != null) {
            resetButton.setDisable(isRunning);
        }
    }

    /**
     * Locks or unlocks parameter input boxes.
     *
     * @param locked true to lock inputs
     */
    private void setSimulationLocked(boolean locked) {
        toggleBoxLockState(populationBox, populationOverlay, locked);
        toggleBoxLockState(partyBox, partyOverlay, locked);
        toggleBoxLockState(budgetBox, budgetOverlay, locked);
        toggleBoxLockState(seedBox, seedOverlay, locked);
        toggleBoxLockState(durationBox, durationOverlay, locked);
        toggleBoxLockState(randomBox, randomOverlay, locked);
    }

    /**
     * Toggles the lock state of a parameter box with visual feedback.
     *
     * @param box the container box to lock/unlock
     * @param overlay the lock overlay label
     * @param locked true to lock, false to unlock
     */
    private void toggleBoxLockState(VBox box, Label overlay, boolean locked) {
        if (box == null) return;
        box.setDisable(locked);
        if (overlay != null) overlay.setVisible(locked);

        Parent container = box.getParent();
        if (container != null) {
            if (locked) {
                if (!container.getStyleClass().contains("locked-zone")) {
                    container.getStyleClass().add("locked-zone");
                }
                setBlinking(container, true);
            } else {
                container.getStyleClass().remove("locked-zone");
                setBlinking(container, false);
            }
        }
    }

    /**
     * Starts or stops a blinking animation on a node.
     *
     * @param node the node to animate
     * @param blinking true to start blinking, false to stop
     */
    private void setBlinking(Node node, boolean blinking) {
        if (blinking) {
            if (!activeBlinks.containsKey(node)) {
                FadeTransition fade = new FadeTransition(Duration.seconds(0.8), node);
                fade.setFromValue(1.0);
                fade.setToValue(0.5);
                fade.setCycleCount(Animation.INDEFINITE);
                fade.setAutoReverse(true);
                fade.play();
                activeBlinks.put(node, fade);
            }
        } else {
            if (activeBlinks.containsKey(node)) {
                FadeTransition fade = activeBlinks.get(node);
                fade.stop();
                node.setOpacity(1.0);
                activeBlinks.remove(node);
            }
        }
    }

    /**
     * Sets the visual lock state of a button.
     *
     * @param btn the button to modify
     * @param locked true to show locked state
     * @param originalText the original button text to restore when unlocked
     */
    private void setButtonLockState(Button btn, boolean locked, String originalText) {
        if (btn == null) return;

        btn.setDisable(locked);
        if (locked) {
            if (!btn.getStyleClass().contains("locked-button")) {
                btn.getStyleClass().add("locked-button");
            }
            btn.setText("[ LOCKED ]");
        } else {
            btn.getStyleClass().remove("locked-button");
            if (originalText != null) {
                btn.setText(originalText);
            }
        }
    }
}
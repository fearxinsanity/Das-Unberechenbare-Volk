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
 * Handles simulation lifecycle (running, paused, reset) and UI feedback.
 *
 * @author Nico Hoffmann
 * @version 1.0
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

    private Button startButton;
    private Button pauseButton;
    private Button resetButton;
    private Button intelButton;
    private Button parliamentButton;

    private VBox populationBox;
    private VBox partyBox;
    private VBox budgetBox;
    private VBox durationBox;

    private Label populationOverlay;
    private Label partyOverlay;
    private Label budgetOverlay;
    private Label durationOverlay;

    private VBox leftSidebar;
    private VBox rightSidebar;

    private String originalIntelText;
    private String originalParliamentText;

    private final Map<Node, FadeTransition> activeBlinks;

    private Runnable onTimerCompleteCallback;
    private Runnable onPauseCallback;

    // ========================================
    // Constructors
    // ========================================

    /**
     * Default constructor.
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

    public int getCurrentTick() {
        return currentTick;
    }

    public int getRemainingSeconds() {
        return remainingSeconds;
    }

    public int getConfigDurationSeconds() {
        return configDurationSeconds;
    }

    public boolean isTimerRunning() {
        return simulationTimer != null && simulationTimer.getStatus() == Animation.Status.RUNNING;
    }

    // ========================================
    // Setter Methods
    // ========================================

    public void setCurrentTick(int tick) {
        this.currentTick = tick;
    }

    public void setTimeStepLabel(Label label) {
        this.timeStepLabel = label;
    }

    public void setDurationField(TextField field) {
        this.durationField = field;
    }

    public void setButtons(Button start, Button pause, Button reset, Button intel, Button parliament) {
        this.startButton = start;
        this.pauseButton = pause;
        this.resetButton = reset;
        this.intelButton = intel;
        this.parliamentButton = parliament;

        // Capture original text
        if (intel != null) {
            this.originalIntelText = intel.getText();
        }
        if (parliament != null) {
            this.originalParliamentText = parliament.getText();
        }
    }

    public void setLockingContainers(
            VBox popBox, VBox partyBox, VBox budgetBox, VBox durationBox,
            Label popOverlay, Label partyOverlay, Label budgetOverlay, Label durationOverlay
    ) {
        this.populationBox = popBox;
        this.partyBox = partyBox;
        this.budgetBox = budgetBox;
        this.durationBox = durationBox;
        this.populationOverlay = popOverlay;
        this.partyOverlay = partyOverlay;
        this.budgetOverlay = budgetOverlay;
        this.durationOverlay = durationOverlay;
    }

    public void setSidebars(VBox left, VBox right) {
        this.leftSidebar = left;
        this.rightSidebar = right;
    }

    public void setOnTimerCompleteCallback(Runnable callback) {
        this.onTimerCompleteCallback = callback;
    }

    public void setOnPauseCallback(Runnable callback) {
        this.onPauseCallback = callback;
    }

    // ========================================
    // Business Logic Methods
    // ========================================

    /**
     * Initializes and starts the simulation timer.
     */
    public void setupTimer() {
        simulationTimer = new Timeline(new KeyFrame(Duration.seconds(1), ignored -> {
            remainingSeconds--;
            updateStatusDisplay(true);

            if (remainingSeconds <= 0) {
                if (onPauseCallback != null) {
                    onPauseCallback.run();
                }
                lockResultButtons(false);
                VisualFX.triggerSidebarGlitch(leftSidebar, rightSidebar);
                VisualFX.startPulse(intelButton, Color.LIME);
                VisualFX.startPulse(parliamentButton, Color.LIME);
                LOGGER.info("Simulation finished. Access granted.");
            }
        }));
        simulationTimer.setCycleCount(Timeline.INDEFINITE);
        updateDurationDisplay();
    }

    /**
     * Starts the simulation timer.
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
     */
    public void pauseTimer() {
        if (simulationTimer != null) {
            simulationTimer.pause();
        }
        updateButtonStates(false);
        updateStatusDisplay(false);
    }

    /**
     * Resets the simulation state and timer.
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
        updateButtonStates(false);
        updateStatusDisplay(false);
    }

    /**
     * Increments the configured duration by 30 seconds.
     */
    public void incrementDuration() {
        if (configDurationSeconds < 300) {
            configDurationSeconds += 30;
            remainingSeconds = configDurationSeconds;
            updateDurationDisplay();
        }
    }

    /**
     * Decrements the configured duration by 30 seconds.
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
     * Locks or unlocks the result buttons (Intel, Parliament).
     *
     * @param locked true to lock, false to unlock
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
     */
    public void stopTimer() {
        if (simulationTimer != null) {
            simulationTimer.stop();
        }
    }

    // ========================================
    // Utility Methods
    // ========================================

    private void updateDurationDisplay() {
        if (durationField == null) return;
        int m = configDurationSeconds / 60;
        int s = configDurationSeconds % 60;
        durationField.setText(String.format("%02d:%02d", m, s));
    }

    private void updateButtonStates(boolean isRunning) {
        if (startButton != null) startButton.setDisable(isRunning);
        if (pauseButton != null) pauseButton.setDisable(!isRunning);
        if (resetButton != null) resetButton.setDisable(isRunning);
    }

    private void setSimulationLocked(boolean locked) {
        toggleBoxLockState(populationBox, populationOverlay, locked);
        toggleBoxLockState(partyBox, partyOverlay, locked);
        toggleBoxLockState(budgetBox, budgetOverlay, locked);
        toggleBoxLockState(durationBox, durationOverlay, locked);
    }

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

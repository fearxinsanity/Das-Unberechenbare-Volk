package de.schulprojekt.duv.view.managers;

import de.schulprojekt.duv.view.Main;
import de.schulprojekt.duv.view.util.VisualFX;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * Verwaltet den Simulationsstatus, den Timer, Sperrmechanismen und die UI-Statusanzeige.
 *
 * @author Nico Hoffmann
 * @version 1.0
 */
public class SimulationStateManager {

    // ========================================
    // Static Constants
    // ========================================

    private static final Logger LOGGER = Logger.getLogger(SimulationStateManager.class.getName());

    private static final int DEFAULT_DURATION_SECONDS = 30;
    private static final int MIN_DURATION_SECONDS = 30;
    private static final int MAX_DURATION_SECONDS = 300; // Maximal 5 Minuten

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
    private VBox durationBox;
    private VBox randomBox;

    private Label populationOverlay;
    private Label partyOverlay;
    private Label budgetOverlay;
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
     * Standardkonstruktor für den SimulationStateManager.
     * Initialisiert mit der Standarddauer und einem leeren Zustand.
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

    @SuppressWarnings("unused")
    public int getConfigDurationSeconds() {
        return configDurationSeconds;
    }

    @SuppressWarnings("unused")
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
        if (this.durationField != null) {
            setupDurationInputValidation();
            updateDurationDisplay();
        }
    }

    /**
     * Setzt alle Steuerungsschaltflächen für die Zustandsverwaltung.
     *
     * @param executeToggle die Schaltfläche zum Starten/Pausieren der Simulation
     * @param reset die Schaltfläche zum Zurücksetzen der Simulation
     * @param intel die Schaltfläche für den Geheimdienstbericht
     * @param parliament die Schaltfläche für die Parlamentsansicht
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
     * Setzt die Parameter-Boxen und deren Sperr-Overlays.
     *
     * @param popBox die Box für den Bevölkerungsparameter
     * @param partyBox die Box für die Parteienanzahl
     * @param budgetBox die Box für das Budget
     * @param durationBox die Box für die Simulationsdauer
     * @param randomBox die Box für die Zufallsschaltfläche
     * @param popOverlay das Sperr-Overlay für die Bevölkerung
     * @param partyOverlay das Sperr-Overlay für die Parteien
     * @param budgetOverlay das Sperr-Overlay für das Budget
     * @param durationOverlay das Sperr-Overlay für die Dauer
     * @param randomOverlay das Sperr-Overlay für Zufallswerte
     */
    public void setLockingContainers(
            VBox popBox, VBox partyBox, VBox budgetBox, VBox durationBox, VBox randomBox,
            Label popOverlay, Label partyOverlay, Label budgetOverlay, Label durationOverlay, Label randomOverlay
    ) {
        this.populationBox = popBox;
        this.partyBox = partyBox;
        this.budgetBox = budgetBox;
        this.durationBox = durationBox;
        this.randomBox = randomBox;
        this.populationOverlay = popOverlay;
        this.partyOverlay = partyOverlay;
        this.budgetOverlay = budgetOverlay;
        this.durationOverlay = durationOverlay;
        this.randomOverlay = randomOverlay;
    }

    /**
     * Setzt die Sidebar-Container für visuelle Effekte.
     *
     * @param left der linke Sidebar-Container
     * @param right der rechte Sidebar-Container
     */
    public void setSidebars(VBox left, VBox right) {
        this.leftSidebar = left;
        this.rightSidebar = right;
    }

    /**
     * Setzt den Callback, der aufgerufen wird, wenn die Simulation pausiert wird.
     *
     * @param callback das Runnable, das bei Pause ausgeführt werden soll
     */
    public void setOnPauseCallback(Runnable callback) {
        this.onPauseCallback = callback;
    }

    // ========================================
    // Business Logic Methods
    // ========================================

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
            updateDurationDisplay();
        }));
        simulationTimer.setCycleCount(Timeline.INDEFINITE);
        updateDurationDisplay();
    }

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

    public void pauseTimer() {
        if (simulationTimer != null) {
            simulationTimer.pause();
        }
        updateButtonStates(false);
        updateStatusDisplay(false);
    }

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

    public void incrementDuration() {
        if (configDurationSeconds < MAX_DURATION_SECONDS) {
            configDurationSeconds += 30;
            if (configDurationSeconds > MAX_DURATION_SECONDS) configDurationSeconds = MAX_DURATION_SECONDS;
            remainingSeconds = configDurationSeconds;
            updateDurationDisplay();
        }
    }

    public void decrementDuration() {
        if (configDurationSeconds > MIN_DURATION_SECONDS) {
            configDurationSeconds -= 30;
            if (configDurationSeconds < MIN_DURATION_SECONDS) configDurationSeconds = MIN_DURATION_SECONDS;
            remainingSeconds = configDurationSeconds;
            updateDurationDisplay();
        }
    }

    /**
     * Aktualisiert das Label der Statusanzeige mit dem aktuellen Simulationszustand.
     * Zeigt den Status (laufend/pausiert), die Tick-Anzahl und die verbleibende Zeit an.
     *
     * @param isRunning ob die Simulation aktuell läuft
     */
    public void updateStatusDisplay(boolean isRunning) {
        if (timeStepLabel == null) return;
        ResourceBundle bundle = ResourceBundle.getBundle("de.schulprojekt.duv.messages", Main.getLocale());

        String statusKey = isRunning ? "state.running" : "state.paused";
        String color = isRunning ? "#55ff55" : "#ff5555";

        int m = remainingSeconds / 60;
        int s = remainingSeconds % 60;
        String timeText = String.format("%02d:%02d", m, s);

        timeStepLabel.setText(String.format(bundle.getString("state.status"),
                bundle.getString(statusKey), currentTick, timeText
        ));
        timeStepLabel.setStyle(String.format(
                "-fx-text-fill: %s; -fx-font-family: 'Consolas'; -fx-font-weight: bold;",
                color
        ));
    }

    /**
     * Sperrt oder entsperrt die Ergebnisschaltflächen (Geheimdienst- und Parlamentsansichten).
     * Gesperrte Schaltflächen zeigen einen speziellen Text an und sind deaktiviert.
     *
     * @param locked true, um Schaltflächen zu sperren, false zum Entsperren
     */
    public void lockResultButtons(boolean locked) {
        setButtonLockState(intelButton, locked, originalIntelText);
        setButtonLockState(parliamentButton, locked, originalParliamentText);

        if (locked) {
            if (intelButton != null) VisualFX.stopPulse(intelButton);
            if (parliamentButton != null) VisualFX.stopPulse(parliamentButton);
        }
    }

    public void stopTimer() {
        if (simulationTimer != null) {
            simulationTimer.stop();
        }
    }

    // ========================================
    // Helper Methods
    // ========================================

    /**
     * Aktualisiert das Feld zur Anzeige der Dauer mit der formatierten Zeit.
     * Wenn die Simulation läuft, wird die verbleibende Zeit angezeigt.
     * Wenn sie gestoppt ist, wird die konfigurierte Dauer angezeigt.
     */
    private void updateDurationDisplay() {
        if (durationField == null) return;

        int secondsToShow = (simulationTimer != null && simulationTimer.getStatus() == Animation.Status.RUNNING)
                ? remainingSeconds
                : configDurationSeconds;

        int m = secondsToShow / 60;
        int s = secondsToShow % 60;
        durationField.setText(String.format("%02d:%02d", m, s));
    }

    /**
     * Richtet die Eingabevalidierung für das Dauer-Feld ein.
     * Erlaubt nur Zahlen und Doppelpunkte.
     * Validiert und begrenzt den Wert bei Eingabe oder Fokusverlust.
     */
    private void setupDurationInputValidation() {
        durationField.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.length() > 5) {
                return null;
            }
            return newText.matches("[0-9:]*") ? change : null;
        }));

        durationField.setOnAction(e -> validateAndApplyDurationInput());

        durationField.focusedProperty().addListener((obs, oldVal, isFocused) -> {
            if (!isFocused) {
                validateAndApplyDurationInput();
            }
        });
    }

    private void validateAndApplyDurationInput() {
        String text = durationField.getText();
        if (text == null || text.isEmpty()) {
            updateDurationDisplay();
            return;
        }

        int seconds = parseDurationString(text);
        seconds = Math.max(MIN_DURATION_SECONDS, Math.min(MAX_DURATION_SECONDS, seconds));

        this.configDurationSeconds = seconds;
        this.remainingSeconds = seconds;
        updateDurationDisplay();
    }

    private int parseDurationString(String text) {
        try {
            if (text.contains(":")) {
                String[] parts = text.split(":");
                if (parts.length == 2) {
                    int m = Integer.parseInt(parts[0]);
                    int s = Integer.parseInt(parts[1]);
                    return m * 60 + s;
                }
            } else {
                return Integer.parseInt(text);
            }
        } catch (NumberFormatException ignored) { }
        return configDurationSeconds;
    }

    /**
     * Aktualisiert die Zustände der Steuerungsschaltflächen basierend auf dem Simulationsstatus.
     *
     * @param isRunning true, wenn die Simulation läuft
     */
    private void updateButtonStates(boolean isRunning) {
        ResourceBundle bundle = ResourceBundle.getBundle("de.schulprojekt.duv.messages", Main.getLocale());
        if (executeToggleButton != null) {
            executeToggleButton.setText(isRunning ? bundle.getString("state.freeze") : bundle.getString("state.execute"));
        }
        if (resetButton != null) {
            resetButton.setDisable(isRunning);
        }
    }

    /**
     * Sperrt oder entsperrt die Parameter-Eingabeboxen.
     *
     * @param locked true, um Eingaben zu sperren
     */
    private void setSimulationLocked(boolean locked) {
        toggleBoxLockState(populationBox, populationOverlay, locked);
        toggleBoxLockState(partyBox, partyOverlay, locked);
        toggleBoxLockState(budgetBox, budgetOverlay, locked);
        toggleBoxLockState(durationBox, durationOverlay, locked);
        toggleBoxLockState(randomBox, randomOverlay, locked);
    }

    /**
     * Schaltet den Sperrzustand einer Parameter-Box mit visuellem Feedback um.
     *
     * @param box die Container-Box zum Sperren/Entsperren
     * @param overlay das Label für das Sperr-Overlay
     * @param locked true zum Sperren, false zum Entsperren
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
     * Startet oder stoppt eine Blink-Animation auf einem Knoten.
     *
     * @param node der zu animierende Knoten
     * @param blinking true zum Starten des Blinkens, false zum Stoppen
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
     * Setzt den visuellen Sperrzustand einer Schaltfläche.
     *
     * @param btn die zu ändernde Schaltfläche
     * @param locked true, um den gesperrten Zustand anzuzeigen
     * @param originalText der ursprüngliche Schaltflächentext
     */
    private void setButtonLockState(Button btn, boolean locked, String originalText) {
        if (btn == null) return;
        ResourceBundle bundle = ResourceBundle.getBundle("de.schulprojekt.duv.messages", Main.getLocale());

        btn.setDisable(locked);
        if (locked) {
            if (!btn.getStyleClass().contains("locked-button")) {
                btn.getStyleClass().add("locked-button");
            }
            btn.setText(bundle.getString("state.locked"));
        } else {
            btn.getStyleClass().remove("locked-button");
            if (originalText != null) {
                btn.setText(originalText);
            }
        }
    }
}
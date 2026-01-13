package de.schulprojekt.duv.view.util;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputControl;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import java.util.Locale;
import java.util.Random;

/**
 * Utility class for global visual effects and responsive UI scaling.
 * Cannot be instantiated.
 */
public final class VisualFX {

    // --- Constants: Glitch Effect ---
    private static final String CSS_GLITCH_CLASS = "glitch-active";
    private static final int GLITCH_DURATION_MS = 50;
    private static final int GLITCH_OFFSET_X = 5;
    private static final int GLITCH_CYCLES = 6;

    // --- Constants: Responsive Scaling ---
    private static final double FONT_BASE_SIZE = 12.0;
    private static final double FONT_REF_WIDTH = 1280.0;
    private static final double FONT_MIN_SIZE = 11.0;
    private static final double FONT_MAX_SIZE = 18.0;

    // --- Constructor ---

    private VisualFX() {
        // Prevent instantiation
    }

    // --- Public API ---

    /**
     * Triggers a short "glitch" shaking effect on two sidebar elements.
     * Typically used when a scandal occurs.
     *
     * @param leftNode  The left UI element to shake.
     * @param rightNode The right UI element to shake.
     */
    public static void triggerSidebarGlitch(Node leftNode, Node rightNode) {
        if (leftNode == null || rightNode == null) return;

        // Apply CSS class for visual distortion (if defined in CSS)
        leftNode.getStyleClass().add(CSS_GLITCH_CLASS);
        rightNode.getStyleClass().add(CSS_GLITCH_CLASS);

        // Shake Left
        TranslateTransition ttLeft = createShakeTransition(leftNode, GLITCH_OFFSET_X);

        // Shake Right (Counter-movement)
        TranslateTransition ttRight = createShakeTransition(rightNode, -GLITCH_OFFSET_X);

        // Play in parallel
        ParallelTransition pt = new ParallelTransition(ttLeft, ttRight);

        pt.setOnFinished(ignored -> {
            leftNode.getStyleClass().remove(CSS_GLITCH_CLASS);
            rightNode.getStyleClass().remove(CSS_GLITCH_CLASS);
        });
        pt.play();
    }

    /**
     * Dynamically adjusts the root font size based on window width
     * to simulate responsive scaling across the application.
     *
     * @param scene       The scene to adjust.
     * @param windowWidth The current width of the window.
     */
    public static void adjustResponsiveScale(Scene scene, double windowWidth) {
        if (scene == null || scene.getRoot() == null) return;

        // Calculate scaling factor based on reference width
        double scaleFactor = windowWidth / FONT_REF_WIDTH;

        // Calculate raw size
        double rawSize = FONT_BASE_SIZE * Math.sqrt(scaleFactor);

        // Clamp value to defined limits (Java 21+)
        double newSize = Math.clamp(rawSize, FONT_MIN_SIZE, FONT_MAX_SIZE);

        // Apply font size to root node
        scene.getRoot().setStyle(
                "-fx-font-size: " + String.format(Locale.US, "%.1f", newSize) + "px;"
        );
    }

    // --- 1. Typewriter Effect ---

    /**
     * Erzeugt einen Schreibmaschinen-Effekt für ein Label.
     * @param label Das Label, in dem der Text erscheinen soll.
     * @param content Der vollständige Text.
     * @param delayMillis Verzögerung zwischen den Buchstaben.
     */
    public static void playTypewriterAnimation(Label label, String content, int delayMillis) {
        if (label == null || content == null) return;

        final StringBuilder currentText = new StringBuilder();
        Timeline timeline = new Timeline();

        label.setText(""); // Reset content

        for (int i = 0; i < content.length(); i++) {
            final int index = i;
            KeyFrame frame = new KeyFrame(
                    Duration.millis(i * delayMillis),
                    event -> {
                        currentText.append(content.charAt(index));
                        // Cursor nur anzeigen, wenn nicht fertig
                        String cursor = (index < content.length() - 1) ? "█" : "";
                        label.setText(currentText.toString() + cursor);
                    }
            );
            timeline.getKeyFrames().add(frame);
        }

        // Safety: Ensure full text is set at the end
        timeline.setOnFinished(e -> label.setText(content));
        timeline.play();
    }

    // --- 2. Alarm Pulse Effect ---

    /**
     * Lässt ein Node (Button, Label, Pane) rot pulsieren, um Alarmbereitschaft zu signalisieren.
     * @param node Das UI-Element, das pulsieren soll.
     */
    public static void startAlarmPulse(Node node) {
        if (node == null) return;

        FadeTransition fadeTransition = new FadeTransition(Duration.seconds(0.8), node);
        fadeTransition.setFromValue(1.0);
        fadeTransition.setToValue(0.4);
        fadeTransition.setCycleCount(Animation.INDEFINITE);
        fadeTransition.setAutoReverse(true);

        DropShadow alarmGlow = new DropShadow();
        alarmGlow.setColor(Color.RED);
        alarmGlow.setRadius(20);
        alarmGlow.setSpread(0.5);
        node.setEffect(alarmGlow);

        fadeTransition.play();
    }

    public static void stopAlarmPulse(Node node) {
        if (node == null) return;
        node.setEffect(null);
        node.setOpacity(1.0);
    }

    // --- 3. Decryption Effect ---

    /**
     * Simuliert einen Entschlüsselungseffekt für Labels oder TextFields.
     * Gibt die Timeline zurück, damit man auf das Ende warten kann.
     *
     * @param control Das UI Element (Label oder TextField).
     * @param finalValue Der Zielwert als String.
     * @return Die Animations-Timeline.
     */
    public static Timeline animateDecryption(Control control, String finalValue) {
        if (control == null) return null;

        Timeline timeline = new Timeline();
        Random random = new Random();
        int iterations = 15;
        int delay = 40;

        for (int i = 0; i < iterations; i++) {
            timeline.getKeyFrames().add(new KeyFrame(
                    Duration.millis(i * delay),
                    e -> {
                        // Generate random string of same length
                        String fake = generateRandomString(finalValue.length(), random);
                        setTextOnControl(control, fake);
                    }
            ));
        }

        // Set final value
        timeline.getKeyFrames().add(new KeyFrame(
                Duration.millis(iterations * delay),
                e -> setTextOnControl(control, finalValue)
        ));

        timeline.play();
        return timeline;
    }

    // --- Private Helpers ---

    private static TranslateTransition createShakeTransition(Node node, double byX) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(GLITCH_DURATION_MS), node);
        tt.setByX(byX);
        tt.setCycleCount(GLITCH_CYCLES);
        tt.setAutoReverse(true);
        return tt;
    }

    private static String generateRandomString(int length, Random random) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            if (random.nextBoolean()) {
                sb.append(random.nextInt(10));
            } else {
                sb.append((char) ('A' + random.nextInt(26)));
            }
        }
        return sb.toString();
    }

    private static void setTextOnControl(Control control, String text) {
        if (control instanceof Label l) {
            l.setText(text);
        } else if (control instanceof TextInputControl t) {
            t.setText(text);
        }
    }
}
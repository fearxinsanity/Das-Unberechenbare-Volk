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

    // --- Key for storing animation in Node properties ---
    private static final String KEY_PULSE_ANIMATION = "duv.visualfx.pulse";

    private VisualFX() {
        // Prevent instantiation
    }

    // --- Public API ---

    public static void triggerSidebarGlitch(Node leftNode, Node rightNode) {
        if (leftNode == null || rightNode == null) return;

        leftNode.getStyleClass().add(CSS_GLITCH_CLASS);
        rightNode.getStyleClass().add(CSS_GLITCH_CLASS);

        TranslateTransition ttLeft = createShakeTransition(leftNode, GLITCH_OFFSET_X);
        TranslateTransition ttRight = createShakeTransition(rightNode, -GLITCH_OFFSET_X);

        ParallelTransition pt = new ParallelTransition(ttLeft, ttRight);

        pt.setOnFinished(ignored -> {
            leftNode.getStyleClass().remove(CSS_GLITCH_CLASS);
            rightNode.getStyleClass().remove(CSS_GLITCH_CLASS);
        });
        pt.play();
    }

    public static void adjustResponsiveScale(Scene scene, double windowWidth) {
        if (scene == null || scene.getRoot() == null) return;

        double scaleFactor = windowWidth / FONT_REF_WIDTH;
        double rawSize = FONT_BASE_SIZE * Math.sqrt(scaleFactor);
        double newSize = Math.clamp(rawSize, FONT_MIN_SIZE, FONT_MAX_SIZE);

        scene.getRoot().setStyle(
                "-fx-font-size: " + String.format(Locale.US, "%.1f", newSize) + "px;"
        );
    }

    // --- 1. Typewriter Effect ---

    public static void playTypewriterAnimation(Label label, String content, int delayMillis) {
        if (label == null || content == null) return;

        final StringBuilder currentText = new StringBuilder();
        Timeline timeline = new Timeline();

        label.setText("");

        for (int i = 0; i < content.length(); i++) {
            final int index = i;
            KeyFrame frame = new KeyFrame(
                    Duration.millis(i * delayMillis),
                    event -> {
                        currentText.append(content.charAt(index));
                        String cursor = (index < content.length() - 1) ? "█" : "";
                        label.setText(currentText.toString() + cursor);
                    }
            );
            timeline.getKeyFrames().add(frame);
        }

        timeline.setOnFinished(e -> label.setText(content));
        timeline.play();
    }

    // --- 2. Pulse Effect (Improved) ---

    /**
     * Startet ein Pulsieren in der angegebenen Farbe.
     * Speichert die Animation im Node, damit sie sauber gestoppt werden kann.
     */
    public static void startPulse(Node node, Color color) {
        if (node == null) return;

        // Vorherige Animation stoppen, falls vorhanden
        stopPulse(node);

        FadeTransition fadeTransition = new FadeTransition(Duration.seconds(0.8), node);
        fadeTransition.setFromValue(1.0);
        fadeTransition.setToValue(0.4);
        fadeTransition.setCycleCount(Animation.INDEFINITE);
        fadeTransition.setAutoReverse(true);

        DropShadow glow = new DropShadow();
        glow.setColor(color);
        glow.setRadius(20);
        glow.setSpread(0.5);
        node.setEffect(glow);

        fadeTransition.play();

        // Animation im Node speichern
        node.getProperties().put(KEY_PULSE_ANIMATION, fadeTransition);
    }

    /**
     * Stoppt das Pulsieren und setzt den Node zurück.
     */
    public static void stopPulse(Node node) {
        if (node == null) return;

        // Animation abrufen und stoppen
        if (node.getProperties().containsKey(KEY_PULSE_ANIMATION)) {
            Object anim = node.getProperties().get(KEY_PULSE_ANIMATION);
            if (anim instanceof Animation a) {
                a.stop();
            }
            node.getProperties().remove(KEY_PULSE_ANIMATION);
        }

        // Visuelles Reset
        node.setEffect(null);
        node.setOpacity(1.0);
    }

    // --- 3. Decryption Effect ---

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
                        String fake = generateRandomString(finalValue.length(), random);
                        setTextOnControl(control, fake);
                    }
            ));
        }

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
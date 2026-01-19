package de.schulprojekt.duv.view.util;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import java.util.Locale;

/**
 * Utility class for global visual effects and responsive UI scaling.
 * @author Nico Hoffmann
 * @version 1.0
 */
public final class VisualFX {

    // ========================================
    // Static Variables
    // ========================================

    private static final String CSS_GLITCH_CLASS = "glitch-active";
    private static final int GLITCH_DURATION_MS = 50;
    private static final int GLITCH_OFFSET_X = 5;
    private static final int GLITCH_CYCLES = 6;

    private static final double FONT_BASE_SIZE = 12.0;
    private static final double FONT_REF_WIDTH = 1280.0;
    private static final double FONT_MIN_SIZE = 11.0;
    private static final double FONT_MAX_SIZE = 18.0;

    private static final String KEY_PULSE_ANIMATION = "duv.visualfx.pulse";

    // ========================================
    // Constructors
    // ========================================

    private VisualFX() {
        // Prevent instantiation
    }

    // ========================================
    // Business Logic Methods
    // ========================================

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

    public static void playTypewriterAnimation(Label label, String content, int delayMillis) {
        if (label == null || content == null) return;

        final StringBuilder currentText = new StringBuilder();
        Timeline timeline = new Timeline();

        label.setText("");

        for (int i = 0; i < content.length(); i++) {
            final int index = i;
            KeyFrame frame = new KeyFrame(
                    Duration.millis(i * delayMillis),
                    e -> {
                        currentText.append(content.charAt(index));
                        String cursor = (index < content.length() - 1) ? "█" : "";
                        label.setText(currentText + cursor);
                    }
            );
            timeline.getKeyFrames().add(frame);
        }

        timeline.setOnFinished(e -> label.setText(content));
        timeline.play();
    }

    /**
     * Starts a pulsing effect in the specified color.
     * @param node the target node
     * @param color the pulse color
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
     * Stops the pulsing effect and resets the node.
     * @param node the target node
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

    // ========================================
    // Utility Methods
    // ========================================

    private static TranslateTransition createShakeTransition(Node node, double byX) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(GLITCH_DURATION_MS), node);
        tt.setByX(byX);
        tt.setCycleCount(GLITCH_CYCLES);
        tt.setAutoReverse(true);
        return tt;
    }

}
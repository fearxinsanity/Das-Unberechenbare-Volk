package de.schulprojekt.duv.view.util;

import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.util.Duration;
import java.util.Locale;

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

    // --- Private Helpers ---

    private static TranslateTransition createShakeTransition(Node node, double byX) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(GLITCH_DURATION_MS), node);
        tt.setByX(byX);
        tt.setCycleCount(GLITCH_CYCLES);
        tt.setAutoReverse(true);
        return tt;
    }
}
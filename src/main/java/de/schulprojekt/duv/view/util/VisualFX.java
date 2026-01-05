package de.schulprojekt.duv.view.util;

import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.util.Duration;
import java.util.Locale;

public class VisualFX {

    /**
     * Führt einen kurzen "Glitch"-Effekt auf zwei seitlichen Elementen aus.
     * Wird typischerweise bei Skandalen ausgelöst.
     */
    public static void triggerSidebarGlitch(Node leftNode, Node rightNode) {
        if (leftNode == null || rightNode == null) return;

        // CSS-Klasse für visuelle Verzerrung (falls definiert)
        leftNode.getStyleClass().add("glitch-active");
        rightNode.getStyleClass().add("glitch-active");

        // Wackeln links
        TranslateTransition ttLeft = new TranslateTransition(Duration.millis(50), leftNode);
        ttLeft.setByX(5);
        ttLeft.setCycleCount(6);
        ttLeft.setAutoReverse(true);

        // Wackeln rechts (gegenläufig)
        TranslateTransition ttRight = new TranslateTransition(Duration.millis(50), rightNode);
        ttRight.setByX(-5);
        ttRight.setCycleCount(6);
        ttRight.setAutoReverse(true);

        // Parallel abspielen
        ParallelTransition pt = new ParallelTransition(ttLeft, ttRight);
        pt.setOnFinished(e -> {
            leftNode.getStyleClass().remove("glitch-active");
            rightNode.getStyleClass().remove("glitch-active");
        });
        pt.play();
    }

    /**
     * Passt die Schriftgröße der Root-Szene dynamisch an die Fensterbreite an,
     * um Responsive-Verhalten zu simulieren (Scaling).
     */
    public static void adjustResponsiveScale(Scene scene, double windowWidth) {
        if (scene == null) return;

        double baseSize = 12.0;
        double referenceWidth = 1280.0;

        // Berechnung des Skalierungsfaktors
        double scaleFactor = windowWidth / referenceWidth;
        // Begrenzung (Clamping) zwischen 11px und 18px
        double newSize = Math.max(11.0, Math.min(18.0, baseSize * Math.sqrt(scaleFactor)));

        // Setzen der Schriftgröße auf dem Root-Knoten
        if (scene.getRoot() != null) {
            scene.getRoot().setStyle("-fx-font-size: " + String.format(Locale.US, "%.1f", newSize) + "px;");
        }
    }
}
package de.schulprojekt.duv.view.managers;

import de.schulprojekt.duv.view.util.VisualFX;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

/**
 * Verwaltet das UI-Layout, die responsive Skalierung und visuelle Effekte.
 *
 * @author Nico Hoffmann
 * @version 1.0
 */
public class UIControlManager {

    // ========================================
    // Static Variables
    // ========================================

    private static final double MIN_SIDEBAR_WIDTH = 250.0;
    private static final double MAX_SIDEBAR_WIDTH = 450.0;
    private static final double SIDEBAR_WIDTH_RATIO = 0.22;

    // ========================================
    // Instance Variables
    // ========================================

    private Pane animationPane;
    private VBox leftSidebar;
    private VBox rightSidebar;

    private boolean isInitialized;

    // ========================================
    // Constructors
    // ========================================

    /**
     * Standardkonstruktor für den UIControlManager.
     * Erstellt einen nicht initialisierten Manager, der über Setter konfiguriert werden muss.
     */
    @SuppressWarnings("unused")
    public UIControlManager() {
        this.isInitialized = false;
    }

    /**
     * Erstellt einen UIControlManager mit allen erforderlichen UI-Komponenten.
     *
     * @param animationPane das Haupt-Pane für Animationen/Visualisierungen
     * @param leftSidebar der Container für die linke Parameter-Sidebar
     * @param rightSidebar der Container für die rechte Steuerungs-Sidebar
     */
    public UIControlManager(Pane animationPane, VBox leftSidebar, VBox rightSidebar) {
        this.animationPane = animationPane;
        this.leftSidebar = leftSidebar;
        this.rightSidebar = rightSidebar;
        this.isInitialized = false;
    }

    // ========================================
    // Getter Methods
    // ========================================

    @SuppressWarnings("unused")
    public boolean isInitialized() {
        return isInitialized;
    }

    // ========================================
    // Setter Methods
    // ========================================

    @SuppressWarnings("unused")
    public void setAnimationPane(Pane pane) {
        this.animationPane = pane;
    }

    @SuppressWarnings("unused")
    public void setSidebars(VBox left, VBox right) {
        this.leftSidebar = left;
        this.rightSidebar = right;
    }

    // ========================================
    // Business Logic Methods
    // ========================================

    public void setupResponsiveLayout() {
        if (animationPane == null) return;

        Platform.runLater(() -> {
            Scene scene = animationPane.getScene();
            if (scene == null) return;

            // Responsive Skalierung basierend auf der Fensterbreite
            scene.widthProperty().addListener((obs, nbr, newValue) ->
                    VisualFX.adjustResponsiveScale(scene, newValue.doubleValue())
            );
            VisualFX.adjustResponsiveScale(scene, scene.getWidth());

            // Dynamische Berechnung der Sidebar-Breite
            // Formel: Breite = clamp(scene.width * 0.22, 250, 450)
            if (leftSidebar != null) {
                leftSidebar.prefWidthProperty().bind(
                        Bindings.max(
                                MIN_SIDEBAR_WIDTH,
                                Bindings.min(
                                        MAX_SIDEBAR_WIDTH,
                                        scene.widthProperty().multiply(SIDEBAR_WIDTH_RATIO)
                                )
                        )
                );
            }

            if (rightSidebar != null) {
                rightSidebar.prefWidthProperty().bind(
                        Bindings.max(
                                MIN_SIDEBAR_WIDTH,
                                Bindings.min(
                                        MAX_SIDEBAR_WIDTH,
                                        scene.widthProperty().multiply(SIDEBAR_WIDTH_RATIO)
                                )
                        )
                );
            }

            isInitialized = true;
        });
    }

    public void triggerSidebarGlitch() {
        if (leftSidebar != null && rightSidebar != null) {
            VisualFX.triggerSidebarGlitch(leftSidebar, rightSidebar);
        }
    }

    // ========================================
    // Utility Methods
    // ========================================

    @SuppressWarnings("unused")
    public boolean validateComponents() {
        return animationPane != null && leftSidebar != null && rightSidebar != null;
    }
}
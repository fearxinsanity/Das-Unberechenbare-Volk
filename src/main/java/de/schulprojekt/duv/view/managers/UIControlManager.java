package de.schulprojekt.duv.view.managers;

import de.schulprojekt.duv.view.util.VisualFX;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

/**
 * Manages UI layout, responsive scaling, and visual effects.
 * Handles responsive design and UI component positioning.
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
     * Default constructor.
     */
    public UIControlManager() {
        this.isInitialized = false;
    }

    /**
     * Constructor with UI components.
     *
     * @param animationPane the main animation pane
     * @param leftSidebar the left sidebar container
     * @param rightSidebar the right sidebar container
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

    public boolean isInitialized() {
        return isInitialized;
    }

    // ========================================
    // Setter Methods
    // ========================================

    public void setAnimationPane(Pane pane) {
        this.animationPane = pane;
    }

    public void setSidebars(VBox left, VBox right) {
        this.leftSidebar = left;
        this.rightSidebar = right;
    }

    // ========================================
    // Business Logic Methods
    // ========================================

    /**
     * Sets up responsive layout bindings and scaling.
     */
    public void setupResponsiveLayout() {
        if (animationPane == null) return;

        Platform.runLater(() -> {
            Scene scene = animationPane.getScene();
            if (scene == null) return;

            // Responsive scaling
            scene.widthProperty().addListener((ignored, ignored2, newVal) ->
                    VisualFX.adjustResponsiveScale(scene, newVal.doubleValue())
            );
            VisualFX.adjustResponsiveScale(scene, scene.getWidth());

            // Sidebar width bindings
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

    /**
     * Triggers a visual glitch effect on sidebars.
     */
    public void triggerSidebarGlitch() {
        if (leftSidebar != null && rightSidebar != null) {
            VisualFX.triggerSidebarGlitch(leftSidebar, rightSidebar);
        }
    }

    // ========================================
    // Utility Methods
    // ========================================

    /**
     * Validates that all required UI components are set.
     *
     * @return true if all components are present
     */
    public boolean validateComponents() {
        return animationPane != null && leftSidebar != null && rightSidebar != null;
    }
}

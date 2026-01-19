package de.schulprojekt.duv.view.managers;

import de.schulprojekt.duv.view.util.VisualFX;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

/**
 * Manages UI layout, responsive scaling, and visual effects.
 * Handles responsive design with dynamic sidebar width calculation
 * and provides visual effect triggers for enhanced user feedback.
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
     * Default constructor for UIControlManager.
     * Creates an uninitialized manager that must be configured with setters.
     */
    @SuppressWarnings("unused")
    public UIControlManager() {
        this.isInitialized = false;
    }

    /**
     * Constructs a UIControlManager with all required UI components.
     *
     * @param animationPane the main animation/visualization pane
     * @param leftSidebar the left parameter sidebar container
     * @param rightSidebar the right control sidebar container
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

    /**
     * Checks if the responsive layout has been initialized.
     *
     * @return true if layout is initialized, false otherwise
     */
    @SuppressWarnings("unused")
    public boolean isInitialized() {
        return isInitialized;
    }

    // ========================================
    // Setter Methods
    // ========================================

    /**
     * Sets the animation pane component.
     *
     * @param pane the animation pane to manage
     */
    @SuppressWarnings("unused")
    public void setAnimationPane(Pane pane) {
        this.animationPane = pane;
    }

    /**
     * Sets the sidebar components.
     *
     * @param left the left sidebar container
     * @param right the right sidebar container
     */
    @SuppressWarnings("unused")
    public void setSidebars(VBox left, VBox right) {
        this.leftSidebar = left;
        this.rightSidebar = right;
    }

    // ========================================
    // Business Logic Methods
    // ========================================

    /**
     * Sets up responsive layout bindings and dynamic scaling.
     * Binds sidebar widths to scene width with min/max constraints.
     * Applies responsive scaling based on window size changes.
     * Must be called after scene is attached to stage.
     */
    public void setupResponsiveLayout() {
        if (animationPane == null) return;

        Platform.runLater(() -> {
            Scene scene = animationPane.getScene();
            if (scene == null) return;

            // Responsive scaling based on window width
            scene.widthProperty().addListener((obs, nbr, newValue) ->
                    VisualFX.adjustResponsiveScale(scene, newValue.doubleValue())
            );
            VisualFX.adjustResponsiveScale(scene, scene.getWidth());

            // Dynamic sidebar width calculation
            // Formula: width = clamp(scene.width * 0.22, 250, 450)
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
     * Triggers a visual glitch effect on both sidebars.
     * Used to provide dramatic feedback for significant events (e.g., scandal, timer completion).
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
     * Validates that all required UI components are properly set.
     *
     * @return true if all components are present and valid, false otherwise
     */
    @SuppressWarnings("unused")
    public boolean validateComponents() {
        return animationPane != null && leftSidebar != null && rightSidebar != null;
    }
}

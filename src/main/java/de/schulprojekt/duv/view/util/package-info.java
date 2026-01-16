/**
 * View-specific utility classes for UI effects and animations.
 *
 * <p>Contains helper classes for JavaFX-specific operations that are
 * reused across multiple view components. These utilities are tightly
 * coupled to the JavaFX framework and should not be used in the Model
 * or Controller layers.
 *
 * <h2>Key Components:</h2>
 * <ul>
 *   <li>{@link de.schulprojekt.duv.view.util.VisualFX} - Animation utilities for glitch effects,
 *       pulse animations, and visual feedback
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * Button myButton = new Button("Click me");
 * VisualFX.applyPulseAnimation(myButton);
 * }</pre>
 *
 * <h2>Design Note:</h2>
 * <p>This package is part of the View layer and contains JavaFX
 * dependencies. It should never be imported by Model classes.
 *
 * @author Nico Hoffmann
 * @version 1.0
 * @see javafx.animation
 * @see javafx.scene.effect
 */
package de.schulprojekt.duv.view.util;
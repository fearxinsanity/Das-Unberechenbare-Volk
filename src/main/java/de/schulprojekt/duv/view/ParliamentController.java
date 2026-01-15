package de.schulprojekt.duv.view;

import de.schulprojekt.duv.model.party.Party;
import de.schulprojekt.duv.view.components.ParliamentRenderer;
import de.schulprojekt.duv.view.components.TooltipManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.layout.Pane;

import java.util.List;

/**
 * Controller for the Parliament View (Semicircle Seating).
 * * @author Nico Hoffmann
 * @version 1.1
 */
public class ParliamentController {

    // ========================================
    // Instance Variables
    // ========================================

    @FXML private Pane canvasContainer;

    private ParliamentRenderer renderer;
    private TooltipManager tooltipManager;
    private Parent previousView;

    // ========================================
    // Business Logic Methods
    // ========================================

    /**
     * Initializes the view with party data and a reference to the previous view.
     * * @param parties list of parties to render
     * @param previousView the dashboard root for navigation back
     */
    public void initData(List<Party> parties, Parent previousView) {
        this.previousView = previousView;
        this.renderer = new ParliamentRenderer(canvasContainer);
        this.tooltipManager = new TooltipManager(canvasContainer);

        this.renderer.renderDistribution(parties);
        setupInteractions();
    }

    // ========================================
    // Utility Methods
    // ========================================

    private void setupInteractions() {
        canvasContainer.setOnMouseMoved(event -> {
            if (renderer != null) {
                Party hovered = renderer.getPartyAt(event.getX(), event.getY());
                renderer.setHoveredParty(hovered);
            }
        });

        canvasContainer.setOnMouseClicked(event -> {
            if (renderer == null || tooltipManager == null) return;

            Party clickedParty = renderer.getPartyAt(event.getX(), event.getY());
            if (clickedParty != null) {
                renderer.setSelectedParty(clickedParty);
                double[] center = renderer.getPartyCenterCoordinates(clickedParty);
                tooltipManager.showStaticTooltip(clickedParty, center[0], center[1]);
            } else {
                renderer.setSelectedParty(null);
                tooltipManager.hideTooltip();
            }
        });
    }

    @FXML
    public void handleBack(ActionEvent ignored) {
        if (renderer != null) {
            renderer.stop();
        }

        if (previousView != null && canvasContainer.getScene() != null) {
            canvasContainer.getScene().setRoot(previousView);
        }
    }
}
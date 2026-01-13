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
 * Handles user interaction (Hover, Click) and navigation back to Dashboard.
 */
public class ParliamentController {

    // --- FXML Components ---
    @FXML private Pane canvasContainer;

    // --- Logic & Renderers ---
    private ParliamentRenderer renderer;
    private TooltipManager tooltipManager;
    private Parent previousView;

    // --- Initialization ---

    public void initData(List<Party> parties, Parent previousView) {
        this.previousView = previousView;

        // Initialize Components
        this.renderer = new ParliamentRenderer(canvasContainer);
        this.tooltipManager = new TooltipManager(canvasContainer);

        // Initial Render
        this.renderer.renderDistribution(parties);

        // Setup Event Handlers
        setupInteractions();
    }

    // --- Interaction Logic ---

    private void setupInteractions() {
        // 1. Hover Effect (Glow)
        canvasContainer.setOnMouseMoved(event -> {
            if (renderer != null) {
                Party hovered = renderer.getPartyAt(event.getX(), event.getY());
                renderer.setHoveredParty(hovered);
            }
        });

        // 2. Click Handling (Tooltip & Selection)
        canvasContainer.setOnMouseClicked(event -> {
            if (renderer == null || tooltipManager == null) return;

            Party clickedParty = renderer.getPartyAt(event.getX(), event.getY());

            if (clickedParty != null) {
                // Select and Show Tooltip
                renderer.setSelectedParty(clickedParty);

                double[] center = renderer.getPartyCenterCoordinates(clickedParty);
                tooltipManager.showStaticTooltip(clickedParty, center[0], center[1]);
            } else {
                // Deselect and Hide
                renderer.setSelectedParty(null);
                tooltipManager.hideTooltip();
            }
        });
    }

    // --- Navigation ---

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
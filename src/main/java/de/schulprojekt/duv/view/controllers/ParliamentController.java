package de.schulprojekt.duv.view.controllers;

import de.schulprojekt.duv.model.party.Party;
import de.schulprojekt.duv.view.Main;
import de.schulprojekt.duv.view.components.ParliamentRenderer;
import de.schulprojekt.duv.view.components.TooltipManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;

import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller für die Parlamentsansicht.
 * @author Nico Hoffmann
 * @version 1.1
 */
public class ParliamentController {

    // ========================================
    // Instanzvariablen
    // ========================================

    @FXML private Pane canvasContainer;
    @FXML private Label totalSeatsLabel;

    private ParliamentRenderer renderer;
    private TooltipManager tooltipManager;
    private Parent previousView;

    // ========================================
    // Business-Logik-Methoden
    // ========================================

    /**
     * Initialisiert die Ansicht mit Parteidaten und einer Referenz auf die vorherige Ansicht.
     * @param parties Liste der zu rendernden Parteien
     * @param previousView die Dashboard-Wurzel für die Rücknavigation
     */
    public void initData(List<Party> parties, Parent previousView) {
        this.previousView = previousView;
        this.renderer = new ParliamentRenderer(canvasContainer);
        this.tooltipManager = new TooltipManager(canvasContainer);

        this.renderer.renderDistribution(parties);

        if (totalSeatsLabel != null) {
            ResourceBundle bundle = ResourceBundle.getBundle("de.schulprojekt.duv.messages", Main.getLocale());
            totalSeatsLabel.setText(renderer.getTotalSeats() + bundle.getString("parl.seats_suffix"));
        }

        setupInteractions();
    }

    // ========================================
    // Hilfsmethoden
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
                int seatCount = renderer.getSeatCountForParty(clickedParty);
                tooltipManager.showStaticTooltip(clickedParty, seatCount, center[0], center[1]);
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
package de.schulprojekt.duv.view.components;

import de.schulprojekt.duv.model.party.Party;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

import java.util.List;

public class TooltipManager {
    private final Node interactionLayer; // Normalerweise das Canvas oder die Pane darüber
    private final CanvasRenderer renderer;
    private final Tooltip tooltip;

    private List<Party> currentParties;

    public TooltipManager(Node interactionLayer, CanvasRenderer renderer) {
        this.interactionLayer = interactionLayer;
        this.renderer = renderer;

        this.tooltip = new Tooltip();
        this.tooltip.setShowDelay(Duration.ZERO);
        this.tooltip.setHideDelay(Duration.ZERO);

        setupListeners();
    }

    private void setupListeners() {
        interactionLayer.setOnMouseMoved(this::handleMouseMove);
        interactionLayer.setOnMouseExited(e -> tooltip.hide());
    }

    public void updateData(List<Party> parties) {
        this.currentParties = parties;
    }

    private void handleMouseMove(MouseEvent event) {
        if (currentParties == null) return;

        boolean found = false;
        double mouseX = event.getX();
        double mouseY = event.getY();

        // Prüfen, ob Maus über einer Partei ist
        for (Party p : currentParties) {
            CanvasRenderer.Point pos = renderer.getPartyPosition(p.getName());
            if (pos == null) continue;

            double dist = new Point2D(mouseX, mouseY).distance(pos.x, pos.y);

            // Annahme: Radius ~40px als Hitbox
            if (dist < 40) {
                showTooltip(p, event);
                found = true;
                break;
            }
        }

        if (!found) {
            tooltip.hide();
        }
    }

    private void showTooltip(Party p, MouseEvent event) {
        // WICHTIG: Achte auf das "%s" am Ende des Strings, nicht "%.2f"
        String text = String.format("%s\nWähler: %d\nAusrichtung: %s",
                p.getName(),
                p.getCurrentSupporterCount(),
                p.getPoliticalOrientationName());

        tooltip.setText(text);

        if (interactionLayer.getScene() != null && interactionLayer.getScene().getWindow() != null) {
            tooltip.show(interactionLayer.getScene().getWindow(), event.getScreenX() + 10, event.getScreenY() + 10);
        }
    }
}
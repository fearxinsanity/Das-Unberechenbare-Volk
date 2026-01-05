package de.schulprojekt.duv.view;

import de.schulprojekt.duv.model.party.Party;
import de.schulprojekt.duv.view.components.ParliamentRenderer;
import de.schulprojekt.duv.view.components.TooltipManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.layout.Pane;
import java.util.List;

public class ParliamentController {

    @FXML private Pane canvasContainer;

    private ParliamentRenderer renderer;
    private TooltipManager tooltipManager;
    private Parent previousView;

    public void initData(List<Party> parties, Parent previousView) {
        this.previousView = previousView;

        this.renderer = new ParliamentRenderer(canvasContainer);
        this.tooltipManager = new TooltipManager(canvasContainer);

        this.renderer.renderDistribution(parties);

        // Hover: Leuchten
        canvasContainer.setOnMouseMoved(event -> {
            Party hovered = renderer.getPartyAt(event.getX(), event.getY());
            renderer.setHoveredParty(hovered);
        });

        // Klick: Tooltip & Selektion
        canvasContainer.setOnMouseClicked(event -> {
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
    public void handleBack(ActionEvent event) {
        if (renderer != null) renderer.stop();
        if (previousView != null) canvasContainer.getScene().setRoot(previousView);
    }
}
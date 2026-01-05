package de.schulprojekt.duv.view;

import de.schulprojekt.duv.model.party.Party;
import de.schulprojekt.duv.view.components.ParliamentRenderer;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.layout.Pane;
import java.util.List;

public class ParliamentController {

    @FXML private Pane canvasContainer;

    private ParliamentRenderer renderer;
    private Parent previousView;

    /**
     * Initialisiert die Daten und startet den Renderer.
     * @param parties Die Liste der aktuellen Parteien
     * @param previousView Die View (Root-Node), zu der wir zurückkehren wollen (Dashboard)
     */
    public void initData(List<Party> parties, Parent previousView) {
        this.previousView = previousView;

        // Renderer initialisieren (zeichnet auf canvasContainer)
        this.renderer = new ParliamentRenderer(canvasContainer);

        // Zeichnung starten
        this.renderer.renderDistribution(parties);
    }

    @FXML
    public void handleBack(ActionEvent event) {
        // Animation stoppen, um Ressourcen zu sparen
        if (renderer != null) {
            renderer.stop();
        }

        // Zurück zum Dashboard navigieren
        if (previousView != null) {
            canvasContainer.getScene().setRoot(previousView);
        }
    }
}